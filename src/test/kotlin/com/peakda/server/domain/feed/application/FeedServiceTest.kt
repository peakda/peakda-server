package com.peakda.server.domain.feed.application

import com.peakda.server.common.page.PageRequest
import com.peakda.server.domain.feed.entity.FeedFilter
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.spot.application.SpotRecordResponseAssembler
import com.peakda.server.domain.spot.entity.Plant
import com.peakda.server.domain.spot.entity.PlantStatus
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordPlant
import com.peakda.server.domain.spot.entity.SpotRecordPlantId
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.exception.SpotRecordNotFoundException
import com.peakda.server.domain.spot.presentation.response.SpotRecordResponse
import com.peakda.server.domain.spot.presentation.response.SpotRecordSummaryResponse
import com.peakda.server.domain.spot.repository.PlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordPlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import com.peakda.server.domain.user.entity.UserFavoriteCategory
import com.peakda.server.domain.user.entity.UserFavoriteCategoryId
import com.peakda.server.domain.user.repository.FollowRepository
import com.peakda.server.domain.user.repository.UserFavoriteCategoryRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Sort
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant
import java.util.Optional
import org.springframework.data.domain.PageRequest as SpringPageRequest

class FeedServiceTest {

    private val spotRecordRepository = mock(SpotRecordRepository::class.java)
    private val spotRecordPlantRepository = mock(SpotRecordPlantRepository::class.java)
    private val plantRepository = mock(PlantRepository::class.java)
    private val followRepository = mock(FollowRepository::class.java)
    private val userFavoriteCategoryRepository = mock(UserFavoriteCategoryRepository::class.java)
    private val responseAssembler = mock(SpotRecordResponseAssembler::class.java)

    private val service = FeedService(
        spotRecordRepository,
        spotRecordPlantRepository,
        plantRepository,
        followRepository,
        userFavoriteCategoryRepository,
        responseAssembler,
    )

    private val sort = Sort.by(Sort.Direction.DESC, "createdAt")

    @Test
    fun `all 필터는 게시된 모든 기록을 최신순으로 조회한다`() {
        val record = record(1L)
        val pageable = SpringPageRequest.of(0, 20, sort)
        `when`(spotRecordRepository.findByStatus(SpotRecordStatus.PUBLISHED, pageable))
            .thenReturn(PageImpl(listOf(record), pageable, 1))
        `when`(responseAssembler.assembleSummaries(listOf(record))).thenReturn(listOf(summary(1L)))

        val response = service.list(USER_ID, FeedFilter.ALL, PageRequest(page = 0, size = 20))

        assertThat(response.content).extracting<Long> { it.id }.containsExactly(1L)
    }

    @Test
    fun `following 필터는 팔로잉이 없으면 빈 페이지를 반환한다`() {
        `when`(followRepository.findFollowingIds(USER_ID)).thenReturn(emptyList())

        val response = service.list(USER_ID, FeedFilter.FOLLOWING, PageRequest(page = 0, size = 20))

        assertThat(response.content).isEmpty()
    }

    @Test
    fun `following 필터는 팔로잉 대상의 게시 기록을 조회한다`() {
        val record = record(2L)
        val pageable = SpringPageRequest.of(0, 20, sort)
        `when`(followRepository.findFollowingIds(USER_ID)).thenReturn(listOf(FOLLOWING_ID))
        `when`(spotRecordRepository.findByUserIdInAndStatus(listOf(FOLLOWING_ID), SpotRecordStatus.PUBLISHED, pageable))
            .thenReturn(PageImpl(listOf(record), pageable, 1))
        `when`(responseAssembler.assembleSummaries(listOf(record))).thenReturn(listOf(summary(2L)))

        val response = service.list(USER_ID, FeedFilter.FOLLOWING, PageRequest(page = 0, size = 20))

        assertThat(response.content).extracting<Long> { it.id }.containsExactly(2L)
    }

    @Test
    fun `interest 필터는 관심 카테고리가 없으면 빈 페이지를 반환한다`() {
        `when`(userFavoriteCategoryRepository.findByIdUserId(USER_ID)).thenReturn(emptyList())

        val response = service.list(USER_ID, FeedFilter.INTEREST, PageRequest(page = 0, size = 20))

        assertThat(response.content).isEmpty()
    }

    @Test
    fun `interest 필터는 관심 카테고리를 Plant로, Plant를 기록 id로 확장해 조회한다`() {
        val record = record(3L)
        val pageable = SpringPageRequest.of(0, 20, sort)
        `when`(userFavoriteCategoryRepository.findByIdUserId(USER_ID))
            .thenReturn(listOf(favoriteCategory(USER_ID, BloomCategory.CHERRY)))
        `when`(plantRepository.findByBloomCategoryIn(setOf(BloomCategory.CHERRY)))
            .thenReturn(listOf(plant(10L, BloomCategory.CHERRY)))
        `when`(spotRecordPlantRepository.findByIdPlantIdIn(listOf(10L)))
            .thenReturn(listOf(SpotRecordPlant(SpotRecordPlantId(3L, 10L))))
        `when`(spotRecordRepository.findByIdInAndStatus(listOf(3L), SpotRecordStatus.PUBLISHED, pageable))
            .thenReturn(PageImpl(listOf(record), pageable, 1))
        `when`(responseAssembler.assembleSummaries(listOf(record))).thenReturn(listOf(summary(3L)))

        val response = service.list(USER_ID, FeedFilter.INTEREST, PageRequest(page = 0, size = 20))

        assertThat(response.content).extracting<Long> { it.id }.containsExactly(3L)
    }

    @Test
    fun `게시된 기록 상세는 정상 조회된다`() {
        val record = record(1L, status = SpotRecordStatus.PUBLISHED)
        `when`(spotRecordRepository.findById(1L)).thenReturn(Optional.of(record))
        `when`(responseAssembler.assemble(record)).thenReturn(detail(1L))

        val response = service.detail(1L)

        assertThat(response.id).isEqualTo(1L)
    }

    @Test
    fun `DRAFT 기록 상세 조회는 404 로 처리한다`() {
        val record = record(1L, status = SpotRecordStatus.DRAFT)
        `when`(spotRecordRepository.findById(1L)).thenReturn(Optional.of(record))

        assertThatThrownBy { service.detail(1L) }.isInstanceOf(SpotRecordNotFoundException::class.java)
    }

    @Test
    fun `존재하지 않는 기록 상세 조회는 404 로 처리한다`() {
        `when`(spotRecordRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { service.detail(99L) }.isInstanceOf(SpotRecordNotFoundException::class.java)
    }

    // --- fixtures ---

    private fun record(id: Long, status: SpotRecordStatus = SpotRecordStatus.PUBLISHED): SpotRecord {
        val record = SpotRecord(spotId = 100L, userId = FOLLOWING_ID, status = status)
        ReflectionTestUtils.setField(record, "id", id)
        return record
    }

    private fun summary(id: Long) = SpotRecordSummaryResponse(
        id = id,
        spotId = 100L,
        spotName = "남산",
        user = SpotRecordResponse.UserSummary(id = FOLLOWING_ID, nickname = "user", profileImageUrl = null),
        visitedDate = null,
        bloomStage = null,
        memo = null,
        plants = emptyList(),
        coverPhoto = null,
        status = SpotRecordStatus.PUBLISHED,
        publishedAt = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    private fun detail(id: Long) = SpotRecordResponse(
        id = id,
        spot = SpotRecordResponse.SpotSummary(id = 100L, type = SpotType.LOCAL, name = "남산", address = null, attractionId = null),
        user = SpotRecordResponse.UserSummary(id = FOLLOWING_ID, nickname = "user", profileImageUrl = null),
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

    private fun favoriteCategory(userId: Long, category: BloomCategory) =
        UserFavoriteCategory(UserFavoriteCategoryId(userId, category))

    private fun plant(id: Long, category: BloomCategory): Plant {
        val plant = Plant(name = "p-$id", status = PlantStatus.ACTIVE, bloomCategory = category)
        ReflectionTestUtils.setField(plant, "id", id)
        return plant
    }

    companion object {
        private const val USER_ID = 1L
        private const val FOLLOWING_ID = 2L
    }
}
