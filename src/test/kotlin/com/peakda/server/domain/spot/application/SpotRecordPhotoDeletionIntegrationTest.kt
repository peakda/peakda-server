package com.peakda.server.domain.spot.application

import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.common.storage.StorageException
import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.test.IntegrationTestSupport
import com.peakda.server.domain.spot.entity.*
import com.peakda.server.domain.spot.repository.*
import com.peakda.server.domain.spot.exception.SpotRecordForbiddenException
import com.peakda.server.domain.user.application.UserWithdrawService
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.*
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.anyString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.*
import java.util.UUID
import java.util.concurrent.*

class SpotRecordPhotoDeletionIntegrationTest : IntegrationTestSupport() {
    @Autowired lateinit var jobs: SpotRecordPhotoDeletionRepository
    @Autowired lateinit var uploads: SpotRecordPhotoUploadRepository
    @Autowired lateinit var photos: SpotRecordPhotoRepository
    @Autowired lateinit var records: SpotRecordRepository
    @Autowired lateinit var recordService: SpotRecordService
    @Autowired lateinit var access: SpotRecordPhotoAccessService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var withdraw: UserWithdrawService
    @Autowired lateinit var manager: PlatformTransactionManager
    @Autowired lateinit var transactions: TransactionTemplate
    @MockitoBean lateinit var storage: ObjectStorage
    private val now = Instant.parse("2026-11-08T00:00:00Z")
    private var time = now
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = time
    }
    private lateinit var worker: SpotRecordPhotoDeletionService
    private lateinit var meters: SimpleMeterRegistry

    @BeforeEach fun prepare() {
        clean()
        time = now
        meters = SimpleMeterRegistry()
        worker = newWorker()
        reset(storage)
    }
    @AfterEach fun finish() { clean(); meters.close() }
    private fun clean() {
        jobs.deleteAllInBatch()
        photos.deleteAllInBatch()
        records.deleteAllInBatch()
        uploads.deleteAllInBatch()
    }
    private fun newWorker(max: Int = 3) = SpotRecordPhotoDeletionService(
        jobs, uploads, photos, storage,
        SpotRecordPhotoDeletionProperties(maxAttempts = max, initialBackoff = Duration.ofSeconds(10), maxBackoff = Duration.ofSeconds(15), lease = Duration.ofSeconds(20)),
        meters, manager, clock,
    )
    private fun fixture(owner: Long = 7L): Pair<String, Long> {
        val key = "spot-records/$owner/2026/11/${UUID.randomUUID()}/main.jpg"
        uploads.saveAndFlush(SpotRecordPhotoUpload(key, owner, "thumbnail,medium,main", "SERVER_UPLOAD"))
        val id = records.saveAndFlush(SpotRecord(99L, owner, status = SpotRecordStatus.PUBLISHED)).id!!
        photos.saveAndFlush(SpotRecordPhoto(id, key, 1))
        return key to id
    }
    private fun pending(): SpotRecordPhotoDeletion {
        // 참조 해제의 wall clock 시각을 테스트 clock과 일치시킨다.
        return jobs.findAll().single().also { it.nextAttemptAt = time; jobs.saveAndFlush(it) }
    }
    private fun row(id: Long) = jobs.findById(id).orElseThrow()

    @Test fun `rollback은 예약과 registry 비활성화를 함께 되돌리고 미커밋 작업은 보이지 않는다`() {
        val (key, id) = fixture()
        transactions.executeWithoutResult { tx ->
            recordService.delete(id, 7L)
            assertThat(jobs.count()).isEqualTo(1)
            assertThat(worker.claimNext()).isNull()
            tx.setRollbackOnly()
        }
        assertThat(jobs.count()).isZero()
        assertThat(uploads.findById(key).orElseThrow().available).isTrue()
        assertThat(photos.findBySpotRecordIdOrderBySortOrderAsc(id)).hasSize(1)
        verifyNoInteractions(storage)
    }

    @Test fun `커밋은 PENDING만 남기고 새 worker가 세 variant를 멱등 완료한다`() {
        val (_, id) = fixture()
        recordService.delete(id, 7L)
        val job = pending()
        assertThat(job.status).isEqualTo(PhotoDeletionStatus.PENDING)
        verifyNoInteractions(storage)
        worker = newWorker()
        assertThat(worker.drain()).isEqualTo(1)
        assertThat(row(job.id!!).status).isEqualTo(PhotoDeletionStatus.COMPLETED)
        assertThat(row(job.id!!).completedVariants.split(',')).containsExactly("thumbnail", "medium", "main")
        assertThat(worker.drain()).isZero()
        verify(storage, times(3)).delete(anyString())
    }

    @Test fun `variant 중간 실패는 진행을 보존하고 backoff 후 남은 variant만 재시도한다`() {
        val (key, id) = fixture()
        val medium = "${key.substringBeforeLast('/')}/medium.jpg"
        doThrow(StorageException(ErrorCode.STORAGE_DELETE_FAILED)).doNothing().`when`(storage).delete(medium)
        recordService.delete(id, 7L)
        val job = pending()
        worker.drain()
        assertThat(row(job.id!!).status).isEqualTo(PhotoDeletionStatus.RETRYABLE)
        assertThat(row(job.id!!).completedVariants).isEqualTo("thumbnail")
        assertThat(row(job.id!!).attempts).isEqualTo(1)
        assertThat(row(job.id!!).lastError).isEqualTo("StorageException")
        assertThat(row(job.id!!).nextAttemptAt).isEqualTo(now.plusSeconds(10))
        assertThat(worker.drain()).isZero()
        time = now.plusSeconds(10)
        newWorker().drain()
        assertThat(row(job.id!!).status).isEqualTo(PhotoDeletionStatus.COMPLETED)
        verify(storage, times(1)).delete("${key.substringBeforeLast('/')}/thumbnail.jpg")
        verify(storage, times(2)).delete(medium)
        verify(storage).delete(key)
    }

    @Test fun `한도와 capped backoff 및 내부 재처리는 누적 실패 증거를 보존한다`() {
        val (_, id) = fixture()
        doThrow(StorageException(ErrorCode.STORAGE_DELETE_FAILED)).`when`(storage).delete(anyString())
        recordService.delete(id, 7L)
        val job = pending()
        worker.drain()
        time = now.plusSeconds(10)
        worker.drain()
        assertThat(row(job.id!!).nextAttemptAt).isEqualTo(now.plusSeconds(25))
        time = now.plusSeconds(25)
        worker.drain()
        val failed = row(job.id!!)
        assertThat(failed.status).isEqualTo(PhotoDeletionStatus.FAILED)
        assertThat(failed.attempts).isEqualTo(3)
        assertThat(failed.totalAttempts).isEqualTo(3)
        assertThat(failed.leaseUntil).isNull()
        assertThat(worker.drain()).isZero()
        assertThat(meters.counter("spot.photo.deletion.failed").count()).isEqualTo(1.0)
        assertThat(worker.retryFailed(job.id!!)).isTrue()
        assertThat(row(job.id!!).lastError).isEqualTo("StorageException")
        reset(storage)
        worker.drain()
        assertThat(row(job.id!!).status).isEqualTo(PhotoDeletionStatus.COMPLETED)
        assertThat(row(job.id!!).totalAttempts).isEqualTo(4)
        assertThat(row(job.id!!).reprocessCount).isEqualTo(1)
    }

    @Test fun `영구 실패는 즉시 FAILED로 남고 무한 재시도하지 않는다`() {
        val (_, id) = fixture()
        doThrow(SecurityException("denied")).`when`(storage).delete(anyString())
        recordService.delete(id, 7L)
        val job = pending()
        worker.drain()
        assertThat(row(job.id!!).status).isEqualTo(PhotoDeletionStatus.FAILED)
        assertThat(row(job.id!!).attempts).isEqualTo(1)
        assertThat(row(job.id!!).lastError).isEqualTo("SecurityException")
        time = now.plusSeconds(10000)
        assertThat(worker.drain()).isZero()
    }

    @Test fun `프로세스 중단 lease를 재획득하면 stale token 완료는 거부한다`() {
        val (_, id) = fixture()
        recordService.delete(id, 7L)
        val job = pending()
        val old = worker.claimNext()!!
        assertThat(row(job.id!!).status).isEqualTo(PhotoDeletionStatus.PROCESSING)
        assertThat(worker.claimNext()).isNull()
        time = now.plusSeconds(20)
        val fresh = newWorker().claimNext()!!
        assertThat(fresh.token).isNotEqualTo(old.token)
        assertThat(worker.process(old)).isFalse()
        verifyNoInteractions(storage)
        assertThat(worker.process(fresh)).isTrue()
        assertThat(row(job.id!!).attempts).isEqualTo(2)
        assertThat(row(job.id!!).status).isEqualTo(PhotoDeletionStatus.COMPLETED)
    }

    @Test fun `lease 중단도 최대 횟수 뒤 FAILED가 되어 영구 PROCESSING을 남기지 않는다`() {
        val (_, id) = fixture()
        recordService.delete(id, 7L)
        val job = pending()
        val claims = (0..2).map { index ->
            time = now.plusSeconds(index * 20L)
            worker.claimNext()!!
        }
        time = now.plusSeconds(60)
        assertThat(worker.claimNext()).isNull()
        assertThat(row(job.id!!).status).isEqualTo(PhotoDeletionStatus.FAILED)
        assertThat(row(job.id!!).lastError).isEqualTo("ATTEMPTS_EXHAUSTED_AFTER_LEASE")
        claims.forEach { assertThat(worker.process(it)).isFalse() }
        verifyNoInteractions(storage)
    }

    @Test fun `삭제 성공 직후 DB 진행 손실을 재시작하면 이미 없는 객체도 성공한다`() {
        val (key, id) = fixture()
        val missing = mutableSetOf<String>()
        doAnswer { missing.add(it.getArgument(0)); null }.`when`(storage).delete(anyString())
        recordService.delete(id, 7L)
        val job = pending()
        worker.claimNext() // storage 성공 뒤 DB rollback 또는 프로세스 중단을 재현한다.
        storage.delete("${key.substringBeforeLast('/')}/thumbnail.jpg")
        time = now.plusSeconds(20)
        newWorker().drain()
        assertThat(missing).hasSize(3)
        assertThat(row(job.id!!).status).isEqualTo(PhotoDeletionStatus.COMPLETED)
        verify(storage, times(2)).delete("${key.substringBeforeLast('/')}/thumbnail.jpg")
    }

    @Test fun `현재 참조가 발견된 예약은 파일 보존 종료 후 마지막 참조에서 다시 예약된다`() {
        val (key, id) = fixture()
        // 과거/내부 재처리 작업의 ref 재확인 경로. 실제 참조는 살아 있다.
        val job = jobs.saveAndFlush(SpotRecordPhotoDeletion(key, 7L, "SERVER_UPLOAD", "thumbnail,medium,main", now))
        worker.drain()
        assertThat(row(job.id!!).outcome).isEqualTo("REFERENCED")
        assertThat(uploads.findById(key).orElseThrow().available).isTrue()
        verifyNoInteractions(storage)
        recordService.delete(id, 7L)
        assertThat(jobs.count()).isEqualTo(2)
        jobs.findAll().filter { it.status == PhotoDeletionStatus.PENDING }.forEach { it.nextAttemptAt = now; jobs.saveAndFlush(it) }
        worker.drain()
        verify(storage, times(3)).delete(anyString())
    }

    @Test fun `소유 증거가 다른 작업과 증명불가 legacy는 삭제하지 않는다`() {
        val (key, _) = fixture()
        val job = jobs.saveAndFlush(SpotRecordPhotoDeletion(key, 8L, "SERVER_UPLOAD", "thumbnail,main", now))
        worker.drain()
        assertThat(row(job.id!!).status).isEqualTo(PhotoDeletionStatus.FAILED)
        assertThat(row(job.id!!).lastError).isEqualTo("OWNERSHIP_OR_VARIANTS_UNVERIFIABLE")
        verifyNoInteractions(storage)
    }

    @Test fun `삭제 시작 커밋 후 스토리지 호출 중에도 첨부가 재생성하지 못한다`() {
        val (key, id) = fixture()
        recordService.delete(id, 7L)
        pending()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        doAnswer { entered.countDown(); check(release.await(10, TimeUnit.SECONDS)); null }.`when`(storage).delete(anyString())
        try {
            val deletion = pool.submit { worker.drain() }
            check(entered.await(10, TimeUnit.SECONDS))
            val attach = pool.submit {
                assertThatThrownBy { transactions.executeWithoutResult { access.lockForChange(7L, emptySet(), setOf(key)) } }
                    .isInstanceOf(SpotRecordForbiddenException::class.java)
            }
            attach.get(5, TimeUnit.SECONDS)
            release.countDown()
            deletion.get(10, TimeUnit.SECONDS)
            attach.get(10, TimeUnit.SECONDS)
        } finally { release.countDown(); pool.shutdownNow() }
        assertThat(jobs.findAll().single().status).isEqualTo(PhotoDeletionStatus.COMPLETED)
    }

    @Test fun `스토리지 호출이 멈춰도 lease 재획득과 새 worker 완료를 막지 않는다`() {
        val (key, id) = fixture()
        recordService.delete(id, 7L)
        val job = pending()
        val old = worker.claimNext()!!
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val calls = java.util.concurrent.atomic.AtomicInteger()
        doAnswer {
            if (calls.incrementAndGet() == 1) { entered.countDown(); check(release.await(10, TimeUnit.SECONDS)) }
            null
        }.`when`(storage).delete(anyString())
        val pool = Executors.newSingleThreadExecutor()
        try {
            val oldProcess = pool.submit<Boolean> { worker.process(old) }
            check(entered.await(5, TimeUnit.SECONDS))
            assertThat(row(job.id!!).deletionStarted).isTrue()
            assertThat(uploads.findById(key).orElseThrow().available).isFalse()
            time = now.plusSeconds(20)
            val fresh = newWorker().claimNext()!!
            assertThat(worker.process(fresh)).isTrue()
            assertThat(row(job.id!!).status).isEqualTo(PhotoDeletionStatus.COMPLETED)
            release.countDown()
            assertThat(oldProcess.get(5, TimeUnit.SECONDS)).isFalse()
            verify(storage, times(2)).delete("${key.substringBeforeLast('/')}/thumbnail.jpg")
        } finally { release.countDown(); pool.shutdownNow() }
    }

    @Test fun `삭제 시작 이후 예기치 않은 참조는 등록부를 다시 열지 않고 FAILED로 관측한다`() {
        val (key, id) = fixture()
        recordService.delete(id, 7L)
        val job = pending()
        // 외부 호출 결과가 유실됐을 수 있는 durable intent를 재시작한다.
        jobs.saveAndFlush(job.apply { deletionStarted = true })
        val other = records.saveAndFlush(SpotRecord(99L, 7L, status = SpotRecordStatus.PUBLISHED)).id!!
        photos.saveAndFlush(SpotRecordPhoto(other, key, 1))
        worker.drain()
        assertThat(row(job.id!!).status).isEqualTo(PhotoDeletionStatus.FAILED)
        assertThat(row(job.id!!).lastError).isEqualTo("REFERENCE_AFTER_DELETE_STARTED")
        assertThat(uploads.findById(key).orElseThrow().available).isFalse()
        verifyNoInteractions(storage)
    }

    @Test fun `동시 claim은 같은 작업을 두 번 획득하지 않는다`() {
        val (_, id) = fixture()
        recordService.delete(id, 7L)
        pending()
        val pool = Executors.newFixedThreadPool(2)
        try {
            val futures = (1..2).map { pool.submit<SpotRecordPhotoDeletionClaim?> { newWorker().claimNext() } }
            assertThat(futures.mapNotNull { it.get(10, TimeUnit.SECONDS) }).hasSize(1)
        } finally { pool.shutdownNow() }
    }

    @Test fun `동시 마지막 참조 해제는 하나의 durable 예약만 커밋한다`() {
        val (key, first) = fixture()
        val second = records.saveAndFlush(SpotRecord(99L, 7L, status = SpotRecordStatus.PUBLISHED)).id!!
        photos.saveAndFlush(SpotRecordPhoto(second, key, 1))
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val futures = listOf(first, second).map { id -> pool.submit { start.await(); recordService.delete(id, 7L) } }
            start.countDown()
            futures.forEach { it.get(10, TimeUnit.SECONDS) }
        } finally { pool.shutdownNow() }
        assertThat(jobs.count()).isEqualTo(1)
        assertThat(jobs.findAll().single().status).isEqualTo(PhotoDeletionStatus.PENDING)
        verifyNoInteractions(storage)
    }

    @Test fun `storage 호출 중 lease가 만료되면 완료 진행을 거부하고 재획득으로 복구한다`() {
        val (key, id) = fixture()
        recordService.delete(id, 7L)
        val job = pending()
        val old = worker.claimNext()!!
        var first = true
        doAnswer {
            if (first) { first = false; time = now.plusSeconds(21) }
            null
        }.`when`(storage).delete(anyString())
        assertThat(worker.process(old)).isFalse()
        assertThat(row(job.id!!).completedVariants).isEmpty()
        assertThat(row(job.id!!).status).isEqualTo(PhotoDeletionStatus.PROCESSING)
        val fresh = newWorker().claimNext()!!
        assertThat(worker.process(old)).isFalse()
        assertThat(worker.process(fresh)).isTrue()
        assertThat(row(job.id!!).status).isEqualTo(PhotoDeletionStatus.COMPLETED)
        verify(storage, times(2)).delete("${key.substringBeforeLast('/')}/thumbnail.jpg")
    }

    @Test fun `탈퇴의 기존 위임이 커밋한 작업과 증거는 owner row 물리 삭제 후에도 처리된다`() {
        val user = users.saveAndFlush(User(OAuth2LoginType.KAKAO, UUID.randomUUID().toString(), "탈퇴검증"))
        val userId = user.id!!
        try {
            val (key, _) = fixture(userId)
            withdraw.withdraw(userId, MockHttpServletResponse())
            val job = pending()
            verifyNoInteractions(storage)
            users.deleteById(userId)
            users.flush()
            assertThat(users.existsById(userId)).isFalse()
            assertThat(row(job.id!!).ownerId).isEqualTo(userId)
            assertThat(row(job.id!!).evidence).isEqualTo("SERVER_UPLOAD")
            worker.drain()
            assertThat(row(job.id!!).status).isEqualTo(PhotoDeletionStatus.COMPLETED)
            verify(storage).delete(key)
        } finally { users.deleteById(userId) }
    }
}
