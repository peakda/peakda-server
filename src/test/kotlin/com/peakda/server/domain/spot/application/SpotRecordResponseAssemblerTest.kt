package com.peakda.server.domain.spot.application

import com.peakda.server.common.storage.ObjectKeyUrlResolver
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.spot.entity.ReactionType
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordReaction
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
    private val spotRecordPhotoUploader = mock(SpotRecordPhotoUploader::class.java)
    private val objectKeyUrlResolver = mock(ObjectKeyUrlResolver::class.java)

    private val assembler = SpotRecordResponseAssembler(
        spotRepository,
        userRepository,
        plantRepository,
        spotRecordPhotoRepository,
        spotRecordPlantRepository,
        spotRecordReactionRepository,
        spotRecordPhotoUploader,
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
