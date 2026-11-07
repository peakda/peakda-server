package com.peakda.server.domain.spot.application

import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordPhoto
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.exception.SpotRecordForbiddenException
import com.peakda.server.domain.spot.repository.PlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordPhotoRepository
import com.peakda.server.domain.spot.repository.SpotRecordPlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.verify
import com.peakda.server.domain.spot.entity.SpotRecordPhotoUpload
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.domain.spot.repository.SpotRecordPhotoUploadRepository

class SpotRecordPhotoAuthorizationRegressionTest {
    private val deletions = mock(com.peakda.server.domain.spot.repository.SpotRecordPhotoDeletionRepository::class.java)
    private val records = mock(SpotRecordRepository::class.java)
    private val photos = mock(SpotRecordPhotoRepository::class.java)
    private val storage = mock(ObjectStorage::class.java)
    private val uploads = mock(SpotRecordPhotoUploadRepository::class.java)
    private val spots = mock(SpotService::class.java)
    private val service = SpotRecordService(
        records,
        photos,
        mock(SpotRecordPlantRepository::class.java),
        mock(PlantRepository::class.java),
        spots,
        SpotRecordPhotoAccessService(uploads, photos, storage, records, deletions),
        mock(SpotRecordResponseAssembler::class.java),
    )
    private val record = SpotRecord(spotId = 10L, userId = 1L, status = SpotRecordStatus.DRAFT).apply {
        ReflectionTestUtils.setField(this, "id", 100L)
    }
    private val foreignKey = "spot-records/2/2026/10/1d09271c-0933-4dbd-a832-35d8990c8d64/main.jpg"

    @BeforeEach
    fun setUp() {
        `when`(records.findByIdForUpdate(100L)).thenReturn(record)
    }

    @Test
    fun `기록 생성과 임시저장 덮어쓰기도 타인의 사진 key를 신규 연결하지 못한다`() {
        val input = SpotResolveInput(10L, SpotType.LOCAL, null, "남산", null, 37.55, 126.99, null, 1L)
        val spot = Spot(type = SpotType.LOCAL, name = "남산", latitude = 37.55, longitude = 126.99).apply {
            ReflectionTestUtils.setField(this, "id", 10L)
        }
        `when`(spots.findOrCreate(input)).thenReturn(spot)
        `when`(records.findByUserIdAndStatusForUpdate(1L, SpotRecordStatus.DRAFT)).thenReturn(record)

        assertThatThrownBy {
            service.create(
                CreateSpotRecordCommand(
                    1L, input, null, null, null, null, emptyList(), listOf(foreignKey), SpotRecordStatus.DRAFT,
                ),
            )
        }.isInstanceOf(SpotRecordForbiddenException::class.java)
        verifyNoInteractions(storage)
    }

    @Test
    fun `타인의 사진 key를 새로 첨부할 수 없다`() {
        assertThatThrownBy {
            service.update(
                UpdateSpotRecordCommand(
                    recordId = 100L,
                    userId = 1L,
                    visitedDate = null,
                    bloomStage = null,
                    memo = null,
                    contentLinkUrl = null,
                    plantIds = null,
                    photoKeys = listOf(foreignKey),
                ),
            )
        }.isInstanceOf(SpotRecordForbiddenException::class.java)
    }

    @Test
    fun `과거 타인 사진 연결이 오염돼 있어도 기록 삭제가 타인 객체를 삭제하지 않는다`() {
        `when`(photos.findBySpotRecordIdOrderBySortOrderAsc(100L))
            .thenReturn(listOf(SpotRecordPhoto(100L, foreignKey, 1)))

        service.delete(100L, 1L)

        verifyNoInteractions(storage)
    }

    @Test
    fun `탈퇴도 과거 연결만 믿고 타인의 사진을 삭제하지 않는다`() {
        `when`(records.findByUserIdForUpdate(1L)).thenReturn(listOf(record))
        `when`(photos.findBySpotRecordIdIn(listOf(100L)))
            .thenReturn(listOf(SpotRecordPhoto(100L, foreignKey, 1)))

        service.deleteAllByUser(1L)

        verifyNoInteractions(storage)
    }

    @Test
    fun `재정렬은 backfill 이전 스냅샷 대신 잠금 후 최신 등록 variant를 저장한다`() {
        val key = foreignKey.replace("spot-records/2/", "spot-records/1/")
        `when`(photos.findBySpotRecordIdOrderBySortOrderAsc(100L)).thenReturn(listOf(SpotRecordPhoto(100L, key, 1)))
        `when`(uploads.findByKeysForUpdate(listOf(key))).thenReturn(
            listOf(SpotRecordPhotoUpload(key, 1L, "thumbnail,medium,main", "SERVER_UPLOAD")),
        )

        service.update(UpdateSpotRecordCommand(100L, 1L, null, null, null, null, null, listOf(key)))

        val saved = ArgumentCaptor.forClass(SpotRecordPhoto::class.java)
        verify(photos).save(saved.capture())
        assertThat(saved.value.variantNames).isEqualTo("thumbnail,medium,main")
    }
}
