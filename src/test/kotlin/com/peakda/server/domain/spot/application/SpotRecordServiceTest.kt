package com.peakda.server.domain.spot.application

import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.exception.SpotRecordNotFoundException
import com.peakda.server.domain.spot.presentation.response.SpotRecordResponse
import com.peakda.server.domain.spot.repository.PlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordPhotoRepository
import com.peakda.server.domain.spot.repository.SpotRecordPlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant
import java.util.Optional

class SpotRecordServiceTest {

    private val spotRecordRepository = mock(SpotRecordRepository::class.java)
    private val spotRecordPhotoRepository = mock(SpotRecordPhotoRepository::class.java)
    private val spotRecordPlantRepository = mock(SpotRecordPlantRepository::class.java)
    private val plantRepository = mock(PlantRepository::class.java)
    private val spotService = mock(SpotService::class.java)
    private val spotRecordPhotoUploader = mock(SpotRecordPhotoUploader::class.java)
    private val responseAssembler = mock(SpotRecordResponseAssembler::class.java)

    private val service = SpotRecordService(
        spotRecordRepository,
        spotRecordPhotoRepository,
        spotRecordPlantRepository,
        plantRepository,
        spotService,
        spotRecordPhotoUploader,
        responseAssembler,
    )

    @Test
    fun `본인 DRAFT 기록은 조회된다`() {
        val record = record(1L, userId = OWNER_ID, status = SpotRecordStatus.DRAFT)
        `when`(spotRecordRepository.findById(1L)).thenReturn(Optional.of(record))
        `when`(responseAssembler.assemble(record)).thenReturn(response(1L))

        val result = service.get(1L, OWNER_ID)

        assertThat(result.id).isEqualTo(1L)
    }

    @Test
    fun `타인의 DRAFT 기록 조회는 404 로 처리한다`() {
        val record = record(1L, userId = OWNER_ID, status = SpotRecordStatus.DRAFT)
        `when`(spotRecordRepository.findById(1L)).thenReturn(Optional.of(record))

        assertThatThrownBy { service.get(1L, OTHER_USER_ID) }.isInstanceOf(SpotRecordNotFoundException::class.java)
    }

    @Test
    fun `타인의 PUBLISHED 기록은 조회된다`() {
        val record = record(1L, userId = OWNER_ID, status = SpotRecordStatus.PUBLISHED)
        `when`(spotRecordRepository.findById(1L)).thenReturn(Optional.of(record))
        `when`(responseAssembler.assemble(record)).thenReturn(response(1L))

        val result = service.get(1L, OTHER_USER_ID)

        assertThat(result.id).isEqualTo(1L)
    }

    private fun record(id: Long, userId: Long, status: SpotRecordStatus): SpotRecord {
        val record = SpotRecord(spotId = 100L, userId = userId, status = status)
        ReflectionTestUtils.setField(record, "id", id)
        return record
    }

    private fun response(id: Long) = SpotRecordResponse(
        id = id,
        spot = SpotRecordResponse.SpotSummary(id = 100L, type = SpotType.LOCAL, name = "남산", address = null, attractionId = null),
        user = SpotRecordResponse.UserSummary(id = OWNER_ID, nickname = "user", profileImageUrl = null),
        visitedDate = null,
        bloomStage = null,
        memo = null,
        plants = emptyList(),
        photos = emptyList(),
        status = SpotRecordStatus.PUBLISHED,
        publishedAt = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    companion object {
        private const val OWNER_ID = 1L
        private const val OTHER_USER_ID = 2L
    }
}
