package com.peakda.server.domain.spot.application

import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.common.storage.ObjectKeyUrlResolver
import com.peakda.server.common.storage.StorageProperties
import com.peakda.server.common.storage.StorageException
import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.test.IntegrationTestSupport
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordPhoto
import com.peakda.server.domain.spot.entity.SpotRecordPhotoUpload
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.repository.SpotRecordPhotoRepository
import com.peakda.server.domain.spot.repository.SpotRecordPhotoUploadRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import com.peakda.server.domain.spot.exception.SpotRecordForbiddenException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.support.TransactionTemplate
import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class SpotRecordPhotoAccessIntegrationTest : IntegrationTestSupport() {
    @Autowired lateinit var access: SpotRecordPhotoAccessService
    @Autowired lateinit var photos: SpotRecordPhotoRepository
    @Autowired lateinit var uploads: SpotRecordPhotoUploadRepository
    @Autowired lateinit var records: SpotRecordRepository
    @Autowired lateinit var recordService: SpotRecordService
    @Autowired lateinit var transactions: TransactionTemplate
    @Autowired lateinit var entityManagerFactory: EntityManagerFactory

    @MockitoBean lateinit var storage: ObjectStorage

    @BeforeEach
    fun clean() {
        photos.deleteAllInBatch()
        uploads.deleteAllInBatch()
        records.deleteAllInBatch()
        clearInvocations(storage)
    }

    @AfterEach
    fun cleanup() {
        photos.deleteAllInBatch()
        uploads.deleteAllInBatch()
        records.deleteAllInBatch()
    }

    @Test
    fun `같은 소유자의 여러 기록이 공유하는 사진은 마지막 참조에서만 삭제한다`() {
        val key = key(7L)
        uploads.saveAndFlush(upload(key, 7L))
        val first = saveRecord(7L)
        val second = saveRecord(7L)
        photos.saveAllAndFlush(listOf(SpotRecordPhoto(first, key, 1), SpotRecordPhoto(second, key, 1)))

        recordService.delete(first, 7L)
        verify(storage, never()).delete(org.mockito.ArgumentMatchers.anyString())
        assertThat(uploads.findById(key).orElseThrow().available).isTrue()

        recordService.delete(second, 7L)
        verify(storage).delete("${key.substringBeforeLast('/')}/thumbnail.jpg")
        verify(storage).delete("${key.substringBeforeLast('/')}/medium.jpg")
        verify(storage).delete(key)
        assertThat(uploads.findById(key).orElseThrow().available).isFalse()
    }

    @Test
    fun `오염된 타인 연결은 기록 삭제에서 소유자의 객체를 삭제하지 않는다`() {
        val key = key(7L)
        uploads.saveAndFlush(upload(key, 7L))
        val recordId = saveRecord(8L)
        photos.saveAndFlush(SpotRecordPhoto(recordId, key, 1))

        recordService.delete(recordId, 8L)

        verify(storage, never()).delete(org.mockito.ArgumentMatchers.anyString())
        assertThat(uploads.findById(key).orElseThrow().available).isTrue()
    }

    @Test
    fun `삭제가 key 잠금을 선점하면 경쟁 첨부는 retired key를 되살리지 못한다`() {
        val key = key(7L)
        uploads.saveAndFlush(upload(key, 7L))
        val recordId = saveRecord(7L)
        photos.saveAndFlush(SpotRecordPhoto(recordId, key, 1))
        val deleteLocked = CountDownLatch(1)
        val attachStarted = CountDownLatch(1)
        val allowDelete = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val deleteFuture = executor.submit {
                transactions.executeWithoutResult {
                    val locked = access.lockForChange(7L, setOf(key), emptySet())
                    deleteLocked.countDown()
                    check(allowDelete.await(5, TimeUnit.SECONDS))
                    photos.deleteBySpotRecordId(recordId)
                    access.deleteUnreferenced(7L, setOf(key), locked)
                }
            }
            check(deleteLocked.await(5, TimeUnit.SECONDS))
            val attachFuture = executor.submit {
                attachStarted.countDown()
                assertThatThrownBy {
                    transactions.executeWithoutResult {
                        access.lockForChange(7L, emptySet(), setOf(key))
                    }
                }.isInstanceOf(SpotRecordForbiddenException::class.java)
            }
            check(attachStarted.await(5, TimeUnit.SECONDS))
            assertThatThrownBy { attachFuture.get(200, TimeUnit.MILLISECONDS) }
                .isInstanceOf(TimeoutException::class.java)
            allowDelete.countDown()
            deleteFuture.get(10, TimeUnit.SECONDS)
            attachFuture.get(10, TimeUnit.SECONDS)
        } finally {
            allowDelete.countDown()
            executor.shutdownNow()
        }
        verify(storage).delete(key)
        assertThat(uploads.findById(key).orElseThrow().available).isFalse()
    }

    @Test
    fun `첨부가 key 잠금을 먼저 잡고 커밋하면 뒤따른 기록 삭제가 새 참조를 보존한다`() {
        val key = key(7L)
        uploads.saveAndFlush(upload(key, 7L))
        val first = saveRecord(7L)
        val second = saveRecord(7L)
        photos.saveAndFlush(SpotRecordPhoto(first, key, 1))
        val attachmentLocked = CountDownLatch(1)
        val deleteStarted = CountDownLatch(1)
        val releaseAttachment = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val attachmentFuture = executor.submit {
                transactions.executeWithoutResult {
                    access.lockForChange(7L, emptySet(), setOf(key))
                    photos.saveAndFlush(SpotRecordPhoto(second, key, 1))
                    attachmentLocked.countDown()
                    check(releaseAttachment.await(5, TimeUnit.SECONDS))
                }
            }
            check(attachmentLocked.await(5, TimeUnit.SECONDS))
            val deleteFuture = executor.submit {
                deleteStarted.countDown()
                recordService.delete(first, 7L)
            }
            check(deleteStarted.await(5, TimeUnit.SECONDS))
            assertThatThrownBy { deleteFuture.get(200, TimeUnit.MILLISECONDS) }
                .isInstanceOf(TimeoutException::class.java)
            releaseAttachment.countDown()
            attachmentFuture.get(10, TimeUnit.SECONDS)
            deleteFuture.get(10, TimeUnit.SECONDS)
        } finally {
            releaseAttachment.countDown()
            executor.shutdownNow()
        }
        assertThat(photos.findBySpotRecordIdOrderBySortOrderAsc(second)).hasSize(1)
        assertThat(uploads.findById(key).orElseThrow().available).isTrue()
        verify(storage, never()).delete(org.mockito.ArgumentMatchers.anyString())
    }

    @Test
    fun `탈퇴는 모든 본인 참조를 한 번 정리하고 타인 객체를 보존한다`() {
        val shared = key(7L)
        val foreign = key(8L)
        uploads.saveAllAndFlush(listOf(upload(shared, 7L), upload(foreign, 8L)))
        val ownedRecord = saveRecord(7L)
        val sharedRecord = saveRecord(7L)
        val contaminatedRecord = saveRecord(7L)
        photos.saveAllAndFlush(
            listOf(
                SpotRecordPhoto(ownedRecord, shared, 1),
                SpotRecordPhoto(sharedRecord, shared, 1),
                SpotRecordPhoto(contaminatedRecord, foreign, 1),
            ),
        )

        recordService.deleteAllByUser(7L)

        assertThat(records.findByUserId(7L)).isEmpty()
        assertThat(photos.findBySpotRecordIdIn(listOf(ownedRecord, sharedRecord, contaminatedRecord))).isEmpty()
        assertThat(uploads.findById(shared).orElseThrow().available).isFalse()
        assertThat(uploads.findById(foreign).orElseThrow().available).isTrue()
        verify(storage).delete(shared)
        verify(storage, never()).delete(foreign)
    }

    @Test
    fun `legacy 등록을 실제 저장하고 없는 medium 응답은 기존 원본 URL로 폴백한다`() {
        val key = key(7L)
        val prefix = key.substringBeforeLast('/')
        `when`(storage.exists(key)).thenReturn(true)
        `when`(storage.exists("$prefix/thumbnail.jpg")).thenReturn(true)
        `when`(storage.exists("$prefix/medium.jpg")).thenReturn(false)
        val recordId = saveRecord(7L)

        transactions.executeWithoutResult {
            val registered = access.lockForChange(7L, emptySet(), setOf(key)).getValue(key)
            photos.saveAndFlush(SpotRecordPhoto(recordId, key, 1, registered.variantNames))
        }

        val registered = uploads.findById(key).orElseThrow()
        assertThat(registered.ownerId).isEqualTo(7L)
        assertThat(registered.evidence).isEqualTo("LEGACY_SERVER_KEY_AND_STORAGE")
        assertThat(registered.variantNames).isEqualTo("thumbnail,main")
        val resolver = SpotRecordPhotoUrlResolver(
            ObjectKeyUrlResolver(storage, StorageProperties(bucket = "test", publicBaseUrl = "https://cdn.example")),
        )
        val urls = resolver.variantUrls(key, photos.findBySpotRecordIdOrderBySortOrderAsc(recordId).single().variantNames)
        assertThat(urls).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "thumbnail" to "https://cdn.example/$prefix/thumbnail.jpg",
                "medium" to "https://cdn.example/$key",
                "main" to "https://cdn.example/$key",
            ),
        )
    }

    @Test
    fun `medium 장애 트랜잭션은 소유 등록을 남기지 않고 재시도에서 세 variant를 보존한다`() {
        val key = key(7L)
        val prefix = key.substringBeforeLast('/')
        `when`(storage.exists(key)).thenReturn(true)
        `when`(storage.exists("$prefix/thumbnail.jpg")).thenReturn(true)
        `when`(storage.exists("$prefix/medium.jpg"))
            .thenThrow(StorageException(ErrorCode.STORAGE_DOWNLOAD_FAILED)).thenReturn(true)

        assertThatThrownBy {
            transactions.executeWithoutResult { access.lockForChange(7L, emptySet(), setOf(key)) }
        }.isInstanceOf(StorageException::class.java)
        assertThat(uploads.existsById(key)).isFalse()

        transactions.executeWithoutResult { access.lockForChange(7L, emptySet(), setOf(key)) }

        assertThat(uploads.findById(key).orElseThrow().variantNames).isEqualTo("thumbnail,medium,main")
    }

    @Test
    fun `여러 사진 참조 해제는 flush 후 한 배치 조회로 남은 foreign 참조도 보존한다`() {
        val keys = (1..12).map { index ->
            "spot-records/7/2026/11/123e4567-e89b-42d3-a456-${index.toString().padStart(12, '0')}/main.jpg"
        }
        uploads.saveAllAndFlush(keys.map { upload(it, 7L) })
        val recordIds = (1..3).map { saveRecord(7L) }
        photos.saveAllAndFlush(keys.mapIndexed { index, key -> SpotRecordPhoto(recordIds[index / 4], key, index % 4 + 1) })
        val foreignRecord = saveRecord(8L)
        photos.saveAndFlush(SpotRecordPhoto(foreignRecord, keys.first(), 1))
        val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        val wasEnabled = statistics.isStatisticsEnabled
        statistics.isStatisticsEnabled = true
        try {
            transactions.executeWithoutResult {
                statistics.clear()
                val locked = access.lockForChange(7L, keys.toSet(), emptySet())
                println("upload lock fixture: keys=12, query executions=${statistics.queryExecutionCount}")
                assertThat(statistics.queryExecutionCount).isEqualTo(1L)
                recordIds.forEach { photos.deleteBySpotRecordId(it) }
                statistics.clear()

                access.deleteUnreferenced(7L, keys.toSet(), locked)

                val queries = statistics.queryExecutionCount
                println("reference lookup fixture: keys=12, query executions=$queries")
                assertThat(queries).isEqualTo(1L)
                assertThat(locked.getValue(keys.first()).available).isTrue()
                assertThat(keys.drop(1).map { locked.getValue(it).available }).containsOnly(false)
            }
        } finally {
            statistics.clear()
            statistics.isStatisticsEnabled = wasEnabled
        }
        verify(storage, never()).delete(keys.first())
        keys.drop(1).forEach { verify(storage).delete(it) }
    }

    private fun saveRecord(userId: Long): Long = requireNotNull(
        records.saveAndFlush(SpotRecord(spotId = 99L, userId = userId, status = SpotRecordStatus.PUBLISHED)).id,
    )

    private fun upload(key: String, ownerId: Long) = SpotRecordPhotoUpload(
        objectKey = key,
        ownerId = ownerId,
        variantNames = SpotRecordPhotoPolicy.CURRENT_VARIANT_NAMES,
        evidence = "SERVER_UPLOAD",
    )

    private fun key(ownerId: Long): String =
        "spot-records/$ownerId/2026/11/123e4567-e89b-42d3-a456-426614174000/main.jpg"
}
