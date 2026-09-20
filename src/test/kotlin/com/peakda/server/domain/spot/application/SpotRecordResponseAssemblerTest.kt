package com.peakda.server.domain.spot.application

import com.peakda.server.common.storage.ObjectKeyUrlResolver
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.spot.entity.ReactionType
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordReaction
import com.peakda.server.domain.spot.entity.SpotRecordPhoto
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.repository.PlantRepository
import com.peakda.server.domain.spot.repository.RecordReactionTypeCount
import com.peakda.server.domain.spot.repository.SpotRecordPhotoRepository
import com.peakda.server.domain.spot.repository.SpotRecordPlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordReactionRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers.anyLong
import org.springframework.test.util.ReflectionTestUtils
import org.junit.jupiter.api.Test
import java.time.Instant

class SpotRecordResponseAssemblerTest {

    private val spotRepository = mock(SpotRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val plantRepository = mock(PlantRepository::class.java)
    private val spotRecordPhotoRepository = mock(SpotRecordPhotoRepository::class.java)
    private val spotRecordPlantRepository = mock(SpotRecordPlantRepository::class.java)
    private val spotRecordReactionRepository = mock(SpotRecordReactionRepository::class.java)
    private val spotRecordPhotoUrlResolver = mock(SpotRecordPhotoUrlResolver::class.java)
    private val objectKeyUrlResolver = mock(ObjectKeyUrlResolver::class.java)

    private val assembler = SpotRecordResponseAssembler(
        spotRepository,
        userRepository,
        plantRepository,
        spotRecordPhotoRepository,
        spotRecordPlantRepository,
        spotRecordReactionRepository,
        spotRecordPhotoUrlResolver,
        objectKeyUrlResolver,
    )

    @Test
    fun `기록 상세에 리액션 집계와 내 리액션을 채운다`() {
        val record = record(101L)
        stubCommon(listOf(record))
        val count = object : RecordReactionTypeCount {
            override val spotRecordId = 101L
            override val reactionType = ReactionType.HEART
            override val count = 3L
        }
        `when`(spotRecordReactionRepository.countsBySpotRecordIdIn(listOf(101L))).thenReturn(listOf(count))
        `when`(spotRecordReactionRepository.findByUserIdAndSpotRecordIdIn(VIEWER_ID, listOf(101L)))
            .thenReturn(listOf(SpotRecordReaction(VIEWER_ID, 101L, ReactionType.HEART)))

        val response = assembler.assemble(record, VIEWER_ID)

        assertThat(response.reactions.counts).extracting<Long> { it.count }.containsExactly(3L)
        assertThat(response.reactions.myReactions).containsExactly(ReactionType.HEART)
    }

    @Test
    fun `목록 리액션 조회는 기록 수와 무관하게 배치 쿼리를 한 번씩 사용한다`() {
        val records = listOf(record(101L), record(102L), record(103L))
        stubCommon(records)
        `when`(spotRecordReactionRepository.countsBySpotRecordIdIn(listOf(101L, 102L, 103L))).thenReturn(emptyList())
        `when`(spotRecordReactionRepository.findByUserIdAndSpotRecordIdIn(VIEWER_ID, listOf(101L, 102L, 103L)))
            .thenReturn(emptyList())

        val responses = assembler.assembleSummaries(records, VIEWER_ID)

        assertThat(responses).hasSize(3)
        assertThat(responses).allSatisfy { response ->
            assertThat(response.reactions.counts).isEmpty()
            assertThat(response.reactions.myReactions).isEmpty()
        }
        verify(spotRecordReactionRepository).countsBySpotRecordIdIn(listOf(101L, 102L, 103L))
        verify(spotRecordReactionRepository).findByUserIdAndSpotRecordIdIn(VIEWER_ID, listOf(101L, 102L, 103L))
        verify(spotRecordReactionRepository, never()).countsBySpotRecordId(anyLong())
    }

    @Test
    fun `익명 조회는 집계만 반환하고 내 리액션을 조회하지 않는다`() {
        val record = record(101L)
        stubCommon(listOf(record))
        `when`(spotRecordReactionRepository.countsBySpotRecordIdIn(listOf(101L))).thenReturn(emptyList())

        val response = assembler.assembleSummaries(listOf(record), null).single()

        assertThat(response.reactions.myReactions).isEmpty()
        verify(spotRecordReactionRepository, never()).findByUserIdAndSpotRecordIdIn(anyLong(), org.mockito.ArgumentMatchers.anyList())
    }

    @Test
    fun `목록 사진은 기록별로 정렬해 모두 반환하고 대표 사진과 같은 항목을 공유한다`() {
        val records = listOf(record(102L), record(101L))
        stubCommon(records)
        val photos = listOf(
            SpotRecordPhoto(102L, "second-2", 2),
            SpotRecordPhoto(101L, "first-2", 2),
            SpotRecordPhoto(102L, "second-1", 1),
            SpotRecordPhoto(101L, "first-1", 1),
        )
        `when`(spotRecordPhotoRepository.findBySpotRecordIdIn(listOf(102L, 101L))).thenReturn(photos)
        `when`(spotRecordPhotoUrlResolver.mainUrl("first-1")).thenReturn("url-first-1")
        `when`(spotRecordPhotoUrlResolver.mainUrl("first-2")).thenReturn("url-first-2")
        `when`(spotRecordPhotoUrlResolver.mainUrl("second-1")).thenReturn("url-second-1")
        `when`(spotRecordPhotoUrlResolver.mainUrl("second-2")).thenReturn("url-second-2")
        listOf("first-1", "first-2", "second-1", "second-2").forEach { key ->
            `when`(spotRecordPhotoUrlResolver.variantUrls(key, null))
                .thenReturn(mapOf("thumbnail" to "thumb-$key", "medium" to "url-$key", "main" to "url-$key"))
        }
        `when`(spotRecordReactionRepository.countsBySpotRecordIdIn(listOf(102L, 101L))).thenReturn(emptyList())
        `when`(spotRecordReactionRepository.findByUserIdAndSpotRecordIdIn(VIEWER_ID, listOf(102L, 101L)))
            .thenReturn(emptyList())

        val responses = assembler.assembleSummaries(records, VIEWER_ID)

        assertThat(responses.map { it.id }).containsExactly(102L, 101L)
        assertThat(responses[0].photos.map { it.objectKey }).containsExactly("second-1", "second-2")
        assertThat(responses[1].photos.map { it.objectKey }).containsExactly("first-1", "first-2")
        assertThat(responses[0].coverPhoto).isSameAs(responses[0].photos.first())
        assertThat(responses[1].coverPhoto).isSameAs(responses[1].photos.first())
        verify(spotRecordPhotoRepository).findBySpotRecordIdIn(listOf(102L, 101L))
        verify(spotRecordPhotoUrlResolver).mainUrl("first-1")
        verify(spotRecordPhotoUrlResolver).mainUrl("first-2")
        verify(spotRecordPhotoUrlResolver).mainUrl("second-1")
        verify(spotRecordPhotoUrlResolver).mainUrl("second-2")
        assertThat(responses[1].photos.first().variants)
            .containsEntry("thumbnail", "thumb-first-1")
            .containsEntry("main", "url-first-1")
    }

    @Test
    fun `사진이 없는 목록 기록은 빈 사진 목록과 null 대표 사진을 반환한다`() {
        val record = record(101L)
        stubCommon(listOf(record))
        `when`(spotRecordReactionRepository.countsBySpotRecordIdIn(listOf(101L))).thenReturn(emptyList())
        `when`(spotRecordReactionRepository.findByUserIdAndSpotRecordIdIn(VIEWER_ID, listOf(101L)))
            .thenReturn(emptyList())

        val response = assembler.assembleSummaries(listOf(record), VIEWER_ID).single()

        assertThat(response.photos).isEmpty()
        assertThat(response.coverPhoto).isNull()
    }

    private fun stubCommon(records: List<SpotRecord>) {
        val recordIds = records.mapNotNull { it.id }
        val spot = Spot(SpotType.LOCAL, name = "남산", latitude = 37.55, longitude = 126.98)
        ReflectionTestUtils.setField(spot, "id", records.first().spotId)
        val user = User(OAuth2LoginType.KAKAO, "provider-${records.first().userId}", "tester")
        ReflectionTestUtils.setField(user, "id", records.first().userId)
        `when`(spotRepository.findAllById(setOf(records.first().spotId))).thenReturn(listOf(spot))
        `when`(userRepository.findAllById(setOf(records.first().userId))).thenReturn(listOf(user))
        `when`(spotRecordPhotoRepository.findBySpotRecordIdIn(recordIds)).thenReturn(emptyList())
        `when`(spotRecordPlantRepository.findByIdSpotRecordIdIn(recordIds)).thenReturn(emptyList())
        `when`(plantRepository.findAllById(emptySet())).thenReturn(emptyList())
        `when`(objectKeyUrlResolver.resolve(null)).thenReturn(null)
    }

    private fun record(id: Long): SpotRecord {
        val record = SpotRecord(spotId = 501L, userId = 7L, status = SpotRecordStatus.PUBLISHED)
        ReflectionTestUtils.setField(record, "id", id)
        ReflectionTestUtils.setField(record, "createdAt", Instant.parse("2026-01-01T00:00:00Z"))
        ReflectionTestUtils.setField(record, "updatedAt", Instant.parse("2026-01-01T00:00:00Z"))
        return record
    }

    companion object {
        private const val VIEWER_ID = 42L
    }
}
