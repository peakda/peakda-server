package com.peakda.server.domain.spot.application

import com.peakda.server.common.image.ImageResizer
import com.peakda.server.common.image.ResizedImage
import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.common.test.IntegrationTestSupport
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordPhoto
import com.peakda.server.domain.spot.entity.SpotRecordPhotoUpload
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.repository.SpotRecordPhotoRepository
import com.peakda.server.domain.spot.repository.SpotRecordPhotoUploadRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class SpotRecordPhotoBackfillIntegrationTest : IntegrationTestSupport() {
    @Autowired lateinit var backfill: SpotRecordPhotoVariantBackfillService
    @Autowired lateinit var deletionService: SpotRecordPhotoDeletionService
    @Autowired lateinit var deletions: com.peakda.server.domain.spot.repository.SpotRecordPhotoDeletionRepository
    @Autowired lateinit var photos: SpotRecordPhotoRepository
    @Autowired lateinit var uploads: SpotRecordPhotoUploadRepository
    @Autowired lateinit var records: SpotRecordRepository
    @Autowired lateinit var recordService: SpotRecordService
    @Autowired lateinit var transactions: TransactionTemplate

    @MockitoBean lateinit var storage: ObjectStorage
    @MockitoBean lateinit var imageResizer: ImageResizer
    @MockitoBean lateinit var assembler: SpotRecordResponseAssembler

    @BeforeEach
    fun clean() {
        deletions.deleteAllInBatch()
        photos.deleteAllInBatch()
        uploads.deleteAllInBatch()
        records.deleteAllInBatch()
        clearInvocations(storage, imageResizer)
    }

    @AfterEach
    fun cleanup() {
        deletions.deleteAllInBatch()
        photos.deleteAllInBatch()
        uploads.deleteAllInBatch()
        records.deleteAllInBatch()
    }

    @Test
    fun `retired 등록이 남은 stale 사진을 백필하지 않는다`() {
        val key = key(7L, "retired")
        val recordId = saveRecord(7L)
        photos.saveAndFlush(SpotRecordPhoto(recordId, key, 1))
        uploads.saveAndFlush(upload(key, 7L, available = false))

        val result = backfill.backfill(50)

        assertThat(result.updated).isZero()
        assertThat(photos.findBySpotRecordIdOrderBySortOrderAsc(recordId).single().variantNames).isNull()
        verify(storage, never()).download(anyString())
        verify(storage, never()).upload(anyString(), any(ByteArray::class.java) ?: byteArrayOf(), anyString())
    }

    @Test
    fun `활성 사진을 백필하면 공유된 모든 association과 등록 variant를 함께 갱신한다`() {
        val key = key(7L, "shared")
        val first = saveRecord(7L)
        val second = saveRecord(7L)
        photos.saveAllAndFlush(listOf(SpotRecordPhoto(first, key, 1), SpotRecordPhoto(second, key, 1)))
        uploads.saveAndFlush(upload(key, 7L))
        stubBackfillStorage()

        val result = backfill.backfill(50)

        assertThat(result.updated).isEqualTo(2)
        assertThat(photos.findBySpotRecordIdIn(listOf(first, second)).map { it.variantNames })
            .containsOnly(SpotRecordPhotoPolicy.CURRENT_VARIANT_NAMES)
        assertThat(uploads.findById(key).orElseThrow().variantNames)
            .isEqualTo(SpotRecordPhotoPolicy.CURRENT_VARIANT_NAMES)
        verify(storage).upload("${key.substringBeforeLast('/')}/thumbnail.jpg", bytes, "image/jpeg")
        verify(storage).upload("${key.substringBeforeLast('/')}/medium.jpg", bytes, "image/jpeg")
    }

    @Test
    fun `백필 저장 실패는 legacy metadata를 남기고 다음 실행에서 재시도할 수 있다`() {
        val key = key(7L, "retry")
        val recordId = saveRecord(7L)
        photos.saveAndFlush(SpotRecordPhoto(recordId, key, 1))
        uploads.saveAndFlush(upload(key, 7L))
        stubBackfillStorage()
        val mediumKey = "${key.substringBeforeLast('/')}/medium.jpg"
        doThrow(IllegalStateException("temporary storage failure")).`when`(storage)
            .upload(org.mockito.ArgumentMatchers.eq(mediumKey) ?: mediumKey,
                any(ByteArray::class.java) ?: byteArrayOf(), anyString())

        val failed = backfill.backfill(50)

        assertThat(failed.failed).isEqualTo(1)
        assertThat(photos.findBySpotRecordIdOrderBySortOrderAsc(recordId).single().variantNames).isNull()
        assertThat(uploads.findById(key).orElseThrow().variantNames).isEqualTo("thumbnail,main")
        doAnswer { it.arguments[0] as String }.`when`(storage)
            .upload(anyString(), any(ByteArray::class.java) ?: byteArrayOf(), anyString())
        assertThat(backfill.backfill(50).updated).isEqualTo(1)
        assertThat(uploads.findById(key).orElseThrow().variantNames).isEqualTo(SpotRecordPhotoPolicy.CURRENT_VARIANT_NAMES)
    }

    @Test
    fun `삭제된 association이 있는 shared key는 현재 참조가 없을 때만 백필 후 삭제한다`() {
        val key = key(7L, "detached")
        val recordId = saveRecord(7L)
        photos.saveAndFlush(SpotRecordPhoto(recordId, key, 1))
        uploads.saveAndFlush(upload(key, 7L))
        stubBackfillStorage()

        transactions.executeWithoutResult {
            photos.deleteBySpotRecordId(recordId)
            photos.flush()
        }
        val result = backfill.backfill(50)

        assertThat(result.updated).isZero()
        verify(storage, never()).download(key)
        assertThat(uploads.findById(key).orElseThrow().available).isTrue()
    }

    @Test
    fun `삭제가 등록부 잠금을 먼저 잡으면 backfill은 커밋 후 참조가 없어 재생성하지 않는다`() {
        val key = key(7L, "delete-first")
        val recordId = saveRecord(7L)
        photos.saveAndFlush(SpotRecordPhoto(recordId, key, 1))
        uploads.saveAndFlush(upload(key, 7L))
        val deleteEntered = CountDownLatch(1)
        val releaseDelete = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val deleteFuture = executor.submit {
                transactions.executeWithoutResult {
                    recordService.delete(recordId, 7L)
                    deleteEntered.countDown()
                    check(releaseDelete.await(5, TimeUnit.SECONDS))
                }
            }
            check(deleteEntered.await(5, TimeUnit.SECONDS))
            val backfillFuture = executor.submit<SpotRecordPhotoVariantBackfillResult> { backfill.backfill(50) }
            assertThatThrownBy { backfillFuture.get(200, TimeUnit.MILLISECONDS) }
                .isInstanceOf(TimeoutException::class.java)
            releaseDelete.countDown()
            deleteFuture.get(10, TimeUnit.SECONDS)
            val result = backfillFuture.get(10, TimeUnit.SECONDS)
            assertThat(result.scanned).isEqualTo(1)
            assertThat(result.updated).isZero()
            verify(storage, never()).download(key)
            verify(storage, never()).upload(anyString(), any(ByteArray::class.java) ?: byteArrayOf(), anyString())
            assertThat(photos.findBySpotRecordIdOrderBySortOrderAsc(recordId)).isEmpty()
        } finally {
            releaseDelete.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `backfill이 등록부 잠금을 먼저 잡으면 삭제가 기다린 뒤 갱신된 variant 세트를 삭제한다`() {
        val key = key(7L, "backfill-first")
        val recordId = saveRecord(7L)
        photos.saveAndFlush(SpotRecordPhoto(recordId, key, 1))
        uploads.saveAndFlush(upload(key, 7L))
        stubBackfillStorage()
        val uploadEntered = CountDownLatch(1)
        val releaseUpload = CountDownLatch(1)
        doAnswer { invocation ->
            uploadEntered.countDown()
            check(releaseUpload.await(5, TimeUnit.SECONDS))
            invocation.arguments[0] as String
        }.`when`(storage).upload(anyString(), any(ByteArray::class.java) ?: byteArrayOf(), anyString())
        val executor = Executors.newFixedThreadPool(2)
        try {
            val backfillFuture = executor.submit<SpotRecordPhotoVariantBackfillResult> { backfill.backfill(50) }
            check(uploadEntered.await(5, TimeUnit.SECONDS))
            val deleteFuture = executor.submit { recordService.delete(recordId, 7L) }
            assertThatThrownBy { deleteFuture.get(200, TimeUnit.MILLISECONDS) }
                .isInstanceOf(TimeoutException::class.java)
            releaseUpload.countDown()
            assertThat(backfillFuture.get(10, TimeUnit.SECONDS).updated).isEqualTo(1)
            deleteFuture.get(10, TimeUnit.SECONDS)
            assertThat(photos.findBySpotRecordIdOrderBySortOrderAsc(recordId)).isEmpty()
            assertThat(uploads.findById(key).orElseThrow().available).isFalse()
            deletionService.drain()
            verify(storage).delete("${key.substringBeforeLast('/')}/medium.jpg")
        } finally {
            releaseUpload.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `최초 legacy 등록 backfill과 재정렬도 같은 기록 잠금으로 최신 variant를 보존한다`() {
        val key = key(7L, "legacy-reorder")
        val recordId = saveRecord(7L)
        photos.saveAndFlush(SpotRecordPhoto(recordId, key, 1))
        `when`(storage.exists(key)).thenReturn(true)
        `when`(storage.exists("${key.substringBeforeLast('/')}/thumbnail.jpg")).thenReturn(true)
        stubBackfillStorage()
        val uploading = CountDownLatch(1)
        val reorderStarted = CountDownLatch(1)
        val releaseUpload = CountDownLatch(1)
        doAnswer { invocation ->
            uploading.countDown()
            check(releaseUpload.await(5, TimeUnit.SECONDS))
            invocation.arguments[0] as String
        }.`when`(storage).upload(anyString(), any(ByteArray::class.java) ?: byteArrayOf(), anyString())
        val executor = Executors.newFixedThreadPool(2)
        try {
            val backfillFuture = executor.submit<SpotRecordPhotoVariantBackfillResult> { backfill.backfill(50) }
            check(uploading.await(5, TimeUnit.SECONDS))
            val reorderFuture = executor.submit {
                reorderStarted.countDown()
                recordService.update(UpdateSpotRecordCommand(recordId, 7L, null, null, null, null, null, listOf(key)))
            }
            check(reorderStarted.await(5, TimeUnit.SECONDS))
            assertThatThrownBy { reorderFuture.get(200, TimeUnit.MILLISECONDS) }.isInstanceOf(TimeoutException::class.java)
            releaseUpload.countDown()
            assertThat(backfillFuture.get(10, TimeUnit.SECONDS).updated).isEqualTo(1)
            reorderFuture.get(10, TimeUnit.SECONDS)
            assertThat(photos.findBySpotRecordIdOrderBySortOrderAsc(recordId).single().variantNames)
                .isEqualTo(SpotRecordPhotoPolicy.CURRENT_VARIANT_NAMES)
            assertThat(uploads.findById(key).orElseThrow().variantNames).isEqualTo(SpotRecordPhotoPolicy.CURRENT_VARIANT_NAMES)
        } finally {
            releaseUpload.countDown()
            executor.shutdownNow()
        }
    }

    private fun stubBackfillStorage() {
        `when`(storage.download(anyString())).thenReturn(bytes)
        `when`(imageResizer.resize(bytes, SpotRecordPhotoPolicy.VARIANTS.filterNot { it.name == SpotRecordPhotoPolicy.MAIN_VARIANT }))
            .thenReturn(
                SpotRecordPhotoPolicy.VARIANTS
                    .filterNot { it.name == SpotRecordPhotoPolicy.MAIN_VARIANT }
                    .map { ResizedImage(it, bytes) },
            )
    }

    private fun saveRecord(userId: Long): Long = requireNotNull(
        records.saveAndFlush(SpotRecord(spotId = 901L, userId = userId, status = SpotRecordStatus.PUBLISHED)).id,
    )

    private fun upload(key: String, ownerId: Long, available: Boolean = true) = SpotRecordPhotoUpload(
        objectKey = key,
        ownerId = ownerId,
        variantNames = "thumbnail,main",
        evidence = "SERVER_UPLOAD",
        available = available,
    )

    private fun key(ownerId: Long, suffix: String) =
        "spot-records/$ownerId/2026/11/123e4567-e89b-42d3-a456-" +
            suffix.hashCode().toUInt().toString().padStart(12, '0').takeLast(12) + "/main.jpg"

    companion object {
        private val bytes = byteArrayOf(1, 2, 3)
    }
}
