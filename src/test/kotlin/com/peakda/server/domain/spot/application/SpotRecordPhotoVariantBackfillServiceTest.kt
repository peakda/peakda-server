package com.peakda.server.domain.spot.application

import com.peakda.server.common.image.ImageResizer
import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.domain.spot.entity.SpotRecordPhoto
import com.peakda.server.domain.spot.repository.SpotRecordPhotoRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import com.peakda.server.domain.spot.entity.SpotRecordPhotoUpload
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.SimpleTransactionStatus
import org.mockito.ArgumentMatchers.any
import org.springframework.transaction.TransactionDefinition
import org.junit.jupiter.api.BeforeEach
import org.springframework.data.domain.PageRequest
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class SpotRecordPhotoVariantBackfillServiceTest {

    private val spotRecordPhotoRepository = mock(SpotRecordPhotoRepository::class.java)
    private val objectStorage = FakeObjectStorage()
    private val photoAccess = mock(SpotRecordPhotoAccessService::class.java)
    private val transactionManager = mock(PlatformTransactionManager::class.java)
    private val service = SpotRecordPhotoVariantBackfillService(
        spotRecordPhotoRepository,
        objectStorage,
        ImageResizer(),
        photoAccess,
        transactionManager,
    )

    @BeforeEach
    fun prepareTransactions() {
        `when`(transactionManager.getTransaction(any(TransactionDefinition::class.java))).thenReturn(SimpleTransactionStatus())
    }

    @Test
    fun `원본에서 빠진 variant 를 만들어 올리고 보유 목록을 기록한다`() {
        val photo = photo("$PREFIX/main.jpg")
        objectStorage.put("$PREFIX/main.jpg", jpegBytes())
        stubTargets(listOf(photo), remaining = 0L)
        stubActive(photo.objectKey)

        val result = service.backfill(50)

        assertThat(result.scanned).isEqualTo(1)
        assertThat(result.updated).isEqualTo(1)
        assertThat(result.failed).isZero()
        assertThat(result.remaining).isZero()
        verify(photoAccess).updateBackfilledVariants(org.mockito.ArgumentMatchers.any(SpotRecordPhotoUpload::class.java)
            ?: SpotRecordPhotoUpload(photo.objectKey, 42L, "thumbnail,main", "SERVER_UPLOAD"))
        assertThat(photo.variantNames).isNull()
        assertThat(objectStorage.uploadedKeys)
            .containsExactlyInAnyOrder("$PREFIX/thumbnail.jpg", "$PREFIX/medium.jpg")
    }

    @Test
    fun `한 장이 실패해도 나머지를 계속 처리한다`() {
        val broken = photo("$BROKEN_PREFIX/main.jpg")
        val healthy = photo("$PREFIX/main.jpg")
        objectStorage.put("$PREFIX/main.jpg", jpegBytes())
        stubTargets(listOf(broken, healthy), remaining = 1L)
        stubActive(broken.objectKey)
        stubActive(healthy.objectKey)

        val result = service.backfill(50)

        assertThat(result.scanned).isEqualTo(2)
        assertThat(result.updated).isEqualTo(1)
        assertThat(result.failed).isEqualTo(1)
        assertThat(result.remaining).isEqualTo(1L)
        assertThat(broken.variantNames).isNull()
        assertThat(healthy.variantNames).isNull()
    }

    @Test
    fun `배치 크기는 허용 범위로 좁혀진다`() {
        stubTargets(emptyList(), remaining = 0L, pageable = PageRequest.of(0, 200))

        val result = service.backfill(1_000)

        assertThat(result.scanned).isZero()
    }

    @Test
    fun `삭제되어 참조가 사라진 조회 스냅샷으로 사진을 재생성하지 않는다`() {
        val stale = photo("$PREFIX/main.jpg")
        objectStorage.put(stale.objectKey, jpegBytes())
        stubTargets(listOf(stale), remaining = 0L)

        val result = service.backfill(50)

        assertThat(result.updated).isZero()
        assertThat(objectStorage.uploadedKeys).isEmpty()
    }

    private fun stubTargets(
        photos: List<SpotRecordPhoto>,
        remaining: Long,
        pageable: PageRequest = PageRequest.of(0, 50),
    ) {
        `when`(spotRecordPhotoRepository.findByVariantNamesIsNullOrderByIdAsc(pageable)).thenReturn(photos)
        `when`(spotRecordPhotoRepository.countByVariantNamesIsNull()).thenReturn(remaining)
    }

    private fun photo(objectKey: String) = SpotRecordPhoto(spotRecordId = 1L, objectKey = objectKey, sortOrder = 1)

    private fun stubActive(key: String) {
        `when`(photoAccess.lockForBackfill(key)).thenReturn(SpotRecordPhotoUpload(key, 42L, "thumbnail,main", "SERVER_UPLOAD"))
        `when`(spotRecordPhotoRepository.findReferencedObjectKeys(listOf(key))).thenReturn(listOf(key))
    }

    private fun jpegBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        ImageIO.write(BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB), "jpg", output)
        return output.toByteArray()
    }

    private class FakeObjectStorage : ObjectStorage {
        private val sources = mutableMapOf<String, ByteArray>()
        val uploadedKeys = mutableListOf<String>()

        fun put(key: String, bytes: ByteArray) {
            sources[key] = bytes
        }

        override fun upload(key: String, bytes: ByteArray, contentType: String): String {
            uploadedKeys += key
            sources[key] = bytes
            return key
        }

        override fun copy(sourceKey: String, destinationKey: String): String = destinationKey

        override fun download(key: String): ByteArray =
            sources[key] ?: throw IllegalStateException("없는 key: $key")

        override fun delete(key: String) {
            sources -= key
        }

        override fun presignedGetUrl(key: String): String = "https://s3/$key"
    }

    companion object {
        private const val PREFIX = "spot-records/42/2026/09/9b1deb4d"
        private const val BROKEN_PREFIX = "spot-records/42/2026/09/broken"
    }
}
