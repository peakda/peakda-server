package com.peakda.server.domain.spot.application

import com.peakda.server.domain.spot.entity.ContentLinkPlatform
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.exception.SpotRecordInvalidContentLinkException
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
        `when`(responseAssembler.assemble(record, OWNER_ID)).thenReturn(response(1L))

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
        `when`(responseAssembler.assemble(record, OTHER_USER_ID)).thenReturn(response(1L))

        val result = service.get(1L, OTHER_USER_ID)

        assertThat(result.id).isEqualTo(1L)
    }

    @Test
    fun `비로그인 사용자는 게시된 기록을 조회한다`() {
        val record = record(1L, OWNER_ID, SpotRecordStatus.PUBLISHED)
        `when`(spotRecordRepository.findById(1L)).thenReturn(Optional.of(record))
        `when`(responseAssembler.assemble(record, null)).thenReturn(response(1L))

        assertThat(service.get(1L, null).id).isEqualTo(1L)
    }

    @Test
    fun `비로그인 사용자는 임시저장과 숨김 기록을 조회할 수 없다`() {
        for (status in listOf(SpotRecordStatus.DRAFT, SpotRecordStatus.HIDDEN)) {
            `when`(spotRecordRepository.findById(1L)).thenReturn(Optional.of(record(1L, OWNER_ID, status)))

            assertThatThrownBy { service.get(1L, null) }
                .isInstanceOf(SpotRecordNotFoundException::class.java)
        }
        org.mockito.Mockito.verifyNoInteractions(responseAssembler)
    }

    @Test
    fun `기록 생성 시 관련 콘텐츠 링크를 보정해 저장하고 플랫폼을 분류한다`() {
        val draft = record(1L, OWNER_ID, SpotRecordStatus.DRAFT)
        val spotInput = spotInput()
        `when`(spotService.findOrCreate(spotInput)).thenReturn(spot(100L))
        `when`(spotRecordRepository.findByUserIdAndStatus(OWNER_ID, SpotRecordStatus.DRAFT)).thenReturn(draft)
        `when`(responseAssembler.assemble(draft, OWNER_ID)).thenReturn(response(1L))

        service.create(createCommand(spotInput, contentLinkUrl = " instagram.com/p/C5y7abc "))

        assertThat(draft.contentLinkUrl).isEqualTo("https://instagram.com/p/C5y7abc")
        assertThat(draft.contentLinkPlatform).isEqualTo(ContentLinkPlatform.INSTAGRAM)
    }

    @Test
    fun `임시저장을 덮어쓸 때 링크를 비워 보내면 기존 링크를 지운다`() {
        val draft = record(1L, OWNER_ID, SpotRecordStatus.DRAFT).apply {
            contentLinkUrl = "https://youtu.be/x"
            contentLinkPlatform = ContentLinkPlatform.YOUTUBE
        }
        val spotInput = spotInput()
        `when`(spotService.findOrCreate(spotInput)).thenReturn(spot(100L))
        `when`(spotRecordRepository.findByUserIdAndStatus(OWNER_ID, SpotRecordStatus.DRAFT)).thenReturn(draft)
        `when`(responseAssembler.assemble(draft, OWNER_ID)).thenReturn(response(1L))

        service.create(createCommand(spotInput, contentLinkUrl = null))

        assertThat(draft.contentLinkUrl).isNull()
        assertThat(draft.contentLinkPlatform).isNull()
    }

    @Test
    fun `잘못된 링크로 생성하면 스팟을 만들기 전에 거절한다`() {
        assertThatThrownBy { service.create(createCommand(spotInput(), contentLinkUrl = "ftp://example.com/a")) }
            .isInstanceOf(SpotRecordInvalidContentLinkException::class.java)
        org.mockito.Mockito.verifyNoInteractions(spotService, spotRecordRepository)
    }

    @Test
    fun `수정 시 링크를 바꾸면 플랫폼을 다시 분류하고 빈 문자열이면 지우며 null 이면 유지한다`() {
        val record = record(1L, OWNER_ID, SpotRecordStatus.PUBLISHED).apply {
            contentLinkUrl = "https://www.instagram.com/p/old"
            contentLinkPlatform = ContentLinkPlatform.INSTAGRAM
        }
        `when`(spotRecordRepository.findById(1L)).thenReturn(Optional.of(record))
        `when`(responseAssembler.assemble(record, OWNER_ID)).thenReturn(response(1L))

        service.update(updateCommand(contentLinkUrl = null))
        assertThat(record.contentLinkUrl).isEqualTo("https://www.instagram.com/p/old")
        assertThat(record.contentLinkPlatform).isEqualTo(ContentLinkPlatform.INSTAGRAM)

        service.update(updateCommand(contentLinkUrl = "https://blog.naver.com/louin/1"))
        assertThat(record.contentLinkUrl).isEqualTo("https://blog.naver.com/louin/1")
        assertThat(record.contentLinkPlatform).isEqualTo(ContentLinkPlatform.NAVER_BLOG)

        service.update(updateCommand(contentLinkUrl = "https://tistory.com/entry/1"))
        assertThat(record.contentLinkPlatform).isEqualTo(ContentLinkPlatform.UNSUPPORTED)

        service.update(updateCommand(contentLinkUrl = ""))
        assertThat(record.contentLinkUrl).isNull()
        assertThat(record.contentLinkPlatform).isNull()
    }

    private fun spotInput() = SpotResolveInput(
        existingSpotId = 100L,
        type = SpotType.LOCAL,
        attractionId = null,
        name = "남산",
        address = null,
        latitude = 37.55,
        longitude = 126.99,
        kakaoPlaceId = null,
        userId = OWNER_ID,
    )

    private fun spot(id: Long): Spot {
        val spot = Spot(type = SpotType.LOCAL, name = "남산", latitude = 37.55, longitude = 126.99)
        ReflectionTestUtils.setField(spot, "id", id)
        return spot
    }

    private fun createCommand(spotInput: SpotResolveInput, contentLinkUrl: String?) = CreateSpotRecordCommand(
        userId = OWNER_ID,
        spot = spotInput,
        visitedDate = null,
        bloomStage = null,
        memo = null,
        contentLinkUrl = contentLinkUrl,
        plantIds = emptyList(),
        photoKeys = emptyList(),
        status = SpotRecordStatus.DRAFT,
    )

    private fun updateCommand(contentLinkUrl: String?) = UpdateSpotRecordCommand(
        recordId = 1L,
        userId = OWNER_ID,
        visitedDate = null,
        bloomStage = null,
        memo = null,
        contentLinkUrl = contentLinkUrl,
        plantIds = null,
        photoKeys = null,
    )

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
