package com.peakda.server.domain.spot.application

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.image.ImageResizer
import com.peakda.server.common.image.ResizedImage
import com.peakda.server.common.storage.ObjectKeyUrlResolver
import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.common.storage.StorageException
import com.peakda.server.domain.spot.entity.SpotRecordPhotoUpload
import com.peakda.server.domain.spot.repository.SpotRecordPhotoUploadRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.springframework.mock.web.MockMultipartFile

class SpotRecordPhotoUploaderTest {
    private val storage = mock(ObjectStorage::class.java)
    private val resizer = mock(ImageResizer::class.java)
    private val urls = mock(ObjectKeyUrlResolver::class.java)
    private val uploads = mock(SpotRecordPhotoUploadRepository::class.java)
    private val uploader = SpotRecordPhotoUploader(storage, resizer, urls, uploads)
    private val bytes = byteArrayOf(1, 2)
    private val file = MockMultipartFile("images", "photo.jpg", "image/jpeg", bytes)

    @Test
    fun `업로드 완료 후 인증 사용자와 실제 variant를 등록하고 기존 key 응답을 반환한다`() {
        `when`(resizer.resize(bytes, SpotRecordPhotoPolicy.VARIANTS)).thenReturn(
            SpotRecordPhotoPolicy.VARIANTS.map { ResizedImage(it, bytes) },
        )
        `when`(urls.resolveKey(anyString())).thenReturn("https://cdn.example/photo")

        val response = uploader.upload(7L, listOf(file)).single()

        val captured = ArgumentCaptor.forClass(SpotRecordPhotoUpload::class.java)
        verify(uploads).save(captured.capture())
        assertThat(captured.value.ownerId).isEqualTo(7L)
        assertThat(captured.value.objectKey).isEqualTo(response.objectKey)
        assertThat(SpotRecordPhotoPolicy.isServerMainKey(7L, response.objectKey)).isTrue()
        assertThat(captured.value.variantNames).isEqualTo("thumbnail,medium,main")
        assertThat(captured.value.evidence).isEqualTo("SERVER_UPLOAD")
        assertThat(captured.value.available).isTrue()
        assertThat(response.previewUrl).isEqualTo("https://cdn.example/photo")
        val order = inOrder(storage, uploads)
        SpotRecordPhotoPolicy.VARIANTS.forEach {
            order.verify(storage).upload(SpotRecordPhotoPolicy.variantKeyOf(response.objectKey, it), bytes, it.format.mimeType)
        }
        order.verify(uploads).save(captured.value)
    }

    @Test
    fun `일부 variant 업로드가 실패하면 사용 가능한 업로드로 등록하지 않는다`() {
        `when`(resizer.resize(bytes, SpotRecordPhotoPolicy.VARIANTS)).thenReturn(
            SpotRecordPhotoPolicy.VARIANTS.map { ResizedImage(it, bytes) },
        )
        `when`(storage.upload(anyString(), any(ByteArray::class.java) ?: byteArrayOf(), anyString()))
            .thenThrow(StorageException(ErrorCode.STORAGE_UPLOAD_FAILED))

        assertThatThrownBy { uploader.upload(7L, listOf(file)) }.isInstanceOf(StorageException::class.java)
        verifyNoInteractions(uploads)
    }
}
