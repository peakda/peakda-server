package com.peakda.server.domain.spot.application

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.common.storage.StorageException
import com.peakda.server.domain.spot.entity.SpotRecordPhotoUpload
import com.peakda.server.domain.spot.exception.SpotRecordForbiddenException
import com.peakda.server.domain.spot.repository.SpotRecordPhotoRepository
import com.peakda.server.domain.spot.repository.SpotRecordPhotoUploadRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class SpotRecordPhotoAccessServiceTest {
    private val uploads = mock(SpotRecordPhotoUploadRepository::class.java)
    private val photos = mock(SpotRecordPhotoRepository::class.java)
    private val storage = mock(ObjectStorage::class.java)
    private val records = mock(SpotRecordRepository::class.java)
    private val service = SpotRecordPhotoAccessService(uploads, photos, storage, records)
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
        `when`(photos.findReferencedObjectKeys(listOf(key))).thenReturn(listOf(key))

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
        verify(uploads).findByKeysForUpdate(listOf("a", "z"))
        verifyNoMoreInteractions(uploads)
    }

    @Test
    fun `신규 등록이 섞이면 정렬된 순차 잠금으로 낮은 key 등록을 뒤늦게 시도하지 않는다`() {
        val existing = "z"
        `when`(uploads.findByKeyForUpdate(key)).thenReturn(upload())
        service.lockForChange(1L, setOf(existing), setOf(key, existing))
        val order = inOrder(uploads)
        order.verify(uploads).findByKeyForUpdate(key)
        order.verify(uploads).findByKeyForUpdate(existing)
        verifyNoMoreInteractions(uploads)
    }

    @Test
    fun `대량 기존 연결 잠금은 정렬된 배치로 모든 key를 처리한다`() {
        val keys = (0..1_000).map { "key-${it.toString().padStart(4, '0')}" }
        `when`(uploads.findByKeysForUpdate(keys.take(1_000))).thenReturn(
            keys.take(1_000).map { SpotRecordPhotoUpload(it, 1L, "thumbnail,main", "SERVER_UPLOAD") },
        )
        `when`(uploads.findByKeysForUpdate(keys.drop(1_000))).thenReturn(
            keys.drop(1_000).map { SpotRecordPhotoUpload(it, 1L, "thumbnail,main", "SERVER_UPLOAD") },
        )

        assertThat(service.lockForChange(1L, keys.reversed().toSet(), emptySet()).keys).containsExactlyElementsOf(keys)
        val order = inOrder(uploads)
        order.verify(uploads).findByKeysForUpdate(keys.take(1_000))
        order.verify(uploads).findByKeysForUpdate(keys.drop(1_000))
        verifyNoMoreInteractions(uploads)
    }

    @Test
    fun `backfill은 기존 참조 기록을 ID 순서로 먼저 잠그고 객체를 잠근다`() {
        `when`(photos.findRecordIdsByObjectKey(key)).thenReturn(listOf(3L, 1L))
        `when`(uploads.findByKeyForUpdate(key)).thenReturn(upload())

        assertThat(service.lockForBackfill(key).ownerId).isEqualTo(1L)

        val order = inOrder(photos, records, uploads)
        order.verify(photos).findRecordIdsByObjectKey(key)
        order.verify(records).findByIdsForUpdate(listOf(1L, 3L))
        order.verify(uploads).findByKeyForUpdate(key)
    }

    @Test
    fun `삭제 후보 참조는 flush 뒤 한 번에 조회하고 타인 미등록 retired key는 후보에서 뺀다`() {
        val referenced = upload()
        val unreferenced = SpotRecordPhotoUpload("z/main.jpg", 1L, "thumbnail,main", "SERVER_UPLOAD")
        val foreign = SpotRecordPhotoUpload("foreign/main.jpg", 2L, "thumbnail,main", "SERVER_UPLOAD")
        val retired = SpotRecordPhotoUpload("retired/main.jpg", 1L, "thumbnail,main", "SERVER_UPLOAD", available = false)
        val keys = setOf("unknown", foreign.objectKey, retired.objectKey, unreferenced.objectKey, key)
        val candidates = listOf(key, unreferenced.objectKey)
        `when`(photos.findReferencedObjectKeys(candidates)).thenReturn(listOf(key))

        service.deleteUnreferenced(1L, keys, listOf(referenced, unreferenced, foreign, retired).associateBy { it.objectKey })

        val order = inOrder(photos, storage)
        order.verify(photos).flush()
        order.verify(photos).findReferencedObjectKeys(candidates)
        order.verify(storage).delete("z/thumbnail.jpg")
        order.verify(storage).delete(unreferenced.objectKey)
        verifyNoMoreInteractions(photos, storage)
        assertThat(referenced.available).isTrue()
        assertThat(unreferenced.available).isFalse()
        assertThat(foreign.available).isTrue()
    }

    @Test
    fun `삭제 후보가 없으면 빈 IN 조회를 실행하지 않는다`() {
        service.deleteUnreferenced(1L, setOf("unknown"), emptyMap())

        verify(photos).flush()
        verifyNoMoreInteractions(photos)
        verifyNoInteractions(storage)
    }

    @Test
    fun `내부 조회 단위를 넘는 key도 잘리지 않고 각 배치의 참조를 보존한다`() {
        val locked = (0..1_000).associate { index ->
            val key = "key-${index.toString().padStart(4, '0')}/main.jpg"
            key to SpotRecordPhotoUpload(key, 1L, "thumbnail,main", "SERVER_UPLOAD")
        }
        val firstBatch = locked.keys.take(1_000)
        val lastBatch = locked.keys.drop(1_000)
        `when`(photos.findReferencedObjectKeys(firstBatch)).thenReturn(listOf(firstBatch.first()))
        `when`(photos.findReferencedObjectKeys(lastBatch)).thenReturn(lastBatch)

        service.deleteUnreferenced(1L, locked.keys, locked)

        verify(photos).flush()
        verify(photos).findReferencedObjectKeys(firstBatch)
        verify(photos).findReferencedObjectKeys(lastBatch)
        verifyNoMoreInteractions(photos)
        assertThat(locked.getValue(firstBatch.first()).available).isTrue()
        assertThat(locked.getValue(lastBatch.single()).available).isTrue()
        assertThat(firstBatch.drop(1).map { locked.getValue(it).available }).containsOnly(false)
        verify(storage, never()).delete(lastBatch.single())
    }

    private fun upload(owner: Long = 1L, variants: String = SpotRecordPhotoPolicy.CURRENT_VARIANT_NAMES) =
        SpotRecordPhotoUpload(key, owner, variants, "SERVER_UPLOAD")
}
