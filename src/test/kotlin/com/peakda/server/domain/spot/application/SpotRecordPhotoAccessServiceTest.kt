package com.peakda.server.domain.spot.application

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.common.storage.StorageException
import com.peakda.server.domain.spot.entity.SpotRecordPhotoUpload
import com.peakda.server.domain.spot.exception.SpotRecordForbiddenException
import com.peakda.server.domain.spot.repository.SpotRecordPhotoRepository
import com.peakda.server.domain.spot.repository.SpotRecordPhotoUploadRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class SpotRecordPhotoAccessServiceTest {
    private val uploads = mock(SpotRecordPhotoUploadRepository::class.java)
    private val photos = mock(SpotRecordPhotoRepository::class.java)
    private val storage = mock(ObjectStorage::class.java)
    private val service = SpotRecordPhotoAccessService(uploads, photos, storage)
    private val prefix = "spot-records/1/2026/10/1d09271c-0933-4dbd-a832-35d8990c8d64"
    private val key = "$prefix/main.jpg"

    @Test
    fun `등록된 신규 업로드는 스토리지 재조회 없이 같은 소유자가 첨부한다`() {
        val upload = upload()
        `when`(uploads.findByKeyForUpdate(key)).thenReturn(upload)

        assertThat(service.lockForChange(1L, emptySet(), setOf(key))).containsEntry(key, upload)
        verifyNoInteractions(storage)
    }

    @Test
    fun `등록된 타인 key와 삭제된 key는 신규 첨부하지 못한다`() {
        for (upload in listOf(upload(owner = 2L), upload().apply { available = false })) {
            `when`(uploads.findByKeyForUpdate(key)).thenReturn(upload)
            assertThatThrownBy { service.lockForChange(1L, emptySet(), setOf(key)) }
                .isInstanceOf(SpotRecordForbiddenException::class.java)
        }
        verifyNoInteractions(storage)
    }

    @Test
    fun `정상 legacy는 main과 thumbnail 실재를 확인한 후 두 variant로 등록한다`() {
        val upload = upload(variants = "thumbnail,main")
        `when`(uploads.findByKeyForUpdate(key)).thenReturn(null, upload)
        `when`(storage.exists(key)).thenReturn(true)
        `when`(storage.exists("$prefix/thumbnail.jpg")).thenReturn(true)
        `when`(storage.exists("$prefix/medium.jpg")).thenReturn(false)

        assertThat(service.lockForChange(1L, emptySet(), setOf(key))).containsEntry(key, upload)

        verify(uploads).registerLegacyIfAbsent(key, 1L, "thumbnail,main")
    }

    @Test
    fun `세 variant가 실재하는 기존 업로드는 현재 variant를 유지한다`() {
        `when`(uploads.findByKeyForUpdate(key)).thenReturn(null, upload())
        listOf("main", "thumbnail", "medium").forEach {
            `when`(storage.exists("$prefix/$it.jpg")).thenReturn(true)
        }

        service.lockForChange(1L, emptySet(), setOf(key))

        verify(uploads).registerLegacyIfAbsent(key, 1L, SpotRecordPhotoPolicy.CURRENT_VARIANT_NAMES)
    }

    @Test
    fun `medium 일시 조회 장애를 두 variant legacy로 등록하지 않는다`() {
        `when`(uploads.findByKeyForUpdate(key)).thenReturn(null, upload(variants = "thumbnail,main"))
        `when`(storage.exists(key)).thenReturn(true)
        `when`(storage.exists("$prefix/thumbnail.jpg")).thenReturn(true)
        `when`(storage.exists("$prefix/medium.jpg")).thenThrow(StorageException(ErrorCode.STORAGE_DOWNLOAD_FAILED))

        assertThatThrownBy { service.lockForChange(1L, emptySet(), setOf(key)) }
            .isInstanceOf(StorageException::class.java)
        verify(uploads, never()).registerLegacyIfAbsent(anyString(), anyLong(), anyString())
    }

    @Test
    fun `medium 장애 후 재시도는 세 variant로 등록해 영구 축소하지 않는다`() {
        `when`(uploads.findByKeyForUpdate(key)).thenReturn(null, null, upload())
        `when`(storage.exists(key)).thenReturn(true)
        `when`(storage.exists("$prefix/thumbnail.jpg")).thenReturn(true)
        `when`(storage.exists("$prefix/medium.jpg"))
            .thenThrow(StorageException(ErrorCode.STORAGE_DOWNLOAD_FAILED)).thenReturn(true)

        assertThatThrownBy { service.lockForChange(1L, emptySet(), setOf(key)) }
            .isInstanceOf(StorageException::class.java)
        verify(uploads, never()).registerLegacyIfAbsent(anyString(), anyLong(), anyString())

        assertThat(service.lockForChange(1L, emptySet(), setOf(key)).getValue(key).variantNames)
            .isEqualTo(SpotRecordPhotoPolicy.CURRENT_VARIANT_NAMES)
        verify(uploads).registerLegacyIfAbsent(key, 1L, SpotRecordPhotoPolicy.CURRENT_VARIANT_NAMES)
        verify(storage, never()).download(anyString())
    }

    @Test
    fun `필수 main 또는 thumbnail이 없으면 등록하지 않는다`() {
        `when`(storage.exists(key)).thenReturn(false)
        assertThatThrownBy { service.lockForChange(1L, emptySet(), setOf(key)) }
            .isInstanceOf(SpotRecordForbiddenException::class.java)

        `when`(storage.exists(key)).thenReturn(true)
        `when`(storage.exists("$prefix/thumbnail.jpg")).thenReturn(false)
        assertThatThrownBy { service.lockForChange(1L, emptySet(), setOf(key)) }
            .isInstanceOf(SpotRecordForbiddenException::class.java)
        verify(uploads, never()).registerLegacyIfAbsent(anyString(), anyLong(), anyString())
    }

    @Test
    fun `prefix만 맞거나 소유자가 다르거나 main key가 아닌 입력은 실재 조회 전에 거부한다`() {
        val invalid = listOf(
            key.replace("/1/", "/2/"), key.replace("/10/", "/13/"),
            "$prefix/medium.jpg", "$prefix/../main.jpg", "spot-records/1/fake/main.jpg",
            "$key?x=1", "https://cdn.example/$key",
        )
        invalid.forEach {
            assertThatThrownBy { service.lockForChange(1L, emptySet(), setOf(it)) }
                .isInstanceOf(SpotRecordForbiddenException::class.java)
        }
        verifyNoInteractions(storage)
    }

    @Test
    fun `필수 객체 조회 장애는 등록 없이 기존 스토리지 오류로 전달한다`() {
        `when`(storage.exists(key)).thenThrow(StorageException(ErrorCode.STORAGE_DOWNLOAD_FAILED))

        assertThatThrownBy { service.lockForChange(1L, emptySet(), setOf(key)) }
            .isInstanceOf(StorageException::class.java)
        verify(uploads, never()).registerLegacyIfAbsent(anyString(), anyLong(), anyString())
    }

    @Test
    fun `미등록 기존 연결은 재정렬하되 해제 시 파일을 보존한다`() {
        val unknownKey = "historical-unverifiable-key"
        assertThat(service.lockForChange(1L, setOf(unknownKey), setOf(unknownKey))).isEmpty()

        service.deleteUnreferenced(1L, setOf(unknownKey), emptyMap())

        verifyNoInteractions(storage)
    }

    @Test
    fun `다른 기록에 참조가 있으면 파일과 사용 가능 상태를 유지한다`() {
        val upload = upload()
        `when`(photos.existsByObjectKey(key)).thenReturn(true)

        service.deleteUnreferenced(1L, setOf(key), mapOf(key to upload))

        assertThat(upload.available).isTrue()
        verifyNoInteractions(storage)
    }

    @Test
    fun `마지막 본인 참조가 해제되면 삭제 전 재첨부를 차단하고 보유 variant를 삭제한다`() {
        val upload = upload(variants = "thumbnail,main")
        doAnswer {
            assertThat(upload.available).isFalse()
            null
        }.`when`(storage).delete(anyString())

        service.deleteUnreferenced(1L, setOf(key), mapOf(key to upload))

        verify(storage).delete("$prefix/thumbnail.jpg")
        verify(storage).delete(key)
        verifyNoMoreInteractions(storage)
    }

    @Test
    fun `여러 key는 항상 같은 순서로 잠근다`() {
        service.lockForChange(1L, setOf("z", "a"), emptySet())
        val order = inOrder(uploads)
        order.verify(uploads).findByKeyForUpdate("a")
        order.verify(uploads).findByKeyForUpdate("z")
    }

    private fun upload(owner: Long = 1L, variants: String = SpotRecordPhotoPolicy.CURRENT_VARIANT_NAMES) =
        SpotRecordPhotoUpload(key, owner, variants, "SERVER_UPLOAD")
}
