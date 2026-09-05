package com.peakda.server.domain.feed.application

import com.peakda.server.domain.spot.entity.ReactionType
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordReaction
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.exception.SpotRecordNotFoundException
import com.peakda.server.domain.spot.repository.ReactionTypeCount
import com.peakda.server.domain.spot.repository.SpotRecordReactionRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.util.ReflectionTestUtils
import java.util.Optional

class FeedReactionServiceTest {

    private val spotRecordRepository = mock(SpotRecordRepository::class.java)
    private val spotRecordReactionRepository = mock(SpotRecordReactionRepository::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)

    private val service = FeedReactionService(spotRecordRepository, spotRecordReactionRepository, eventPublisher)

    @Test
    fun `게시된 기록에 리액션을 추가하고 갱신된 요약을 반환한다`() {
        `when`(spotRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record(SpotRecordStatus.PUBLISHED)))
        `when`(spotRecordReactionRepository.countsBySpotRecordId(RECORD_ID))
            .thenReturn(listOf(count(ReactionType.HEART, 3), count(ReactionType.SMILE, 1)))
        `when`(spotRecordReactionRepository.findBySpotRecordIdAndUserId(RECORD_ID, USER_ID))
            .thenReturn(listOf(reaction(ReactionType.HEART)))

        val response = service.add(USER_ID, RECORD_ID, ReactionType.HEART)

        assertThat(response.recordId).isEqualTo(RECORD_ID)
        assertThat(response.counts).extracting<ReactionType> { it.reactionType }
            .containsExactlyInAnyOrder(ReactionType.HEART, ReactionType.SMILE)
        assertThat(response.myReactions).containsExactly(ReactionType.HEART)
    }

    @Test
    fun `DRAFT 기록에는 리액션을 추가할 수 없다`() {
        `when`(spotRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record(SpotRecordStatus.DRAFT)))

        assertThatThrownBy { service.add(USER_ID, RECORD_ID, ReactionType.HEART) }
            .isInstanceOf(SpotRecordNotFoundException::class.java)
    }

    @Test
    fun `존재하지 않는 기록에는 리액션을 추가할 수 없다`() {
        `when`(spotRecordRepository.findById(RECORD_ID)).thenReturn(Optional.empty())

        assertThatThrownBy { service.add(USER_ID, RECORD_ID, ReactionType.HEART) }
            .isInstanceOf(SpotRecordNotFoundException::class.java)
    }

    @Test
    fun `리액션을 취소하면 저장소에서 삭제하고 갱신된 요약을 반환한다`() {
        `when`(spotRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record(SpotRecordStatus.PUBLISHED)))
        `when`(spotRecordReactionRepository.countsBySpotRecordId(RECORD_ID)).thenReturn(emptyList())
        `when`(spotRecordReactionRepository.findBySpotRecordIdAndUserId(RECORD_ID, USER_ID)).thenReturn(emptyList())

        val response = service.remove(USER_ID, RECORD_ID, ReactionType.HEART)

        assertThat(response.counts).isEmpty()
        assertThat(response.myReactions).isEmpty()
    }

    // --- fixtures ---

    private fun record(status: SpotRecordStatus): SpotRecord {
        val record = SpotRecord(spotId = 100L, userId = 999L, status = status)
        ReflectionTestUtils.setField(record, "id", RECORD_ID)
        return record
    }

    private fun reaction(type: ReactionType): SpotRecordReaction {
        val reaction = SpotRecordReaction(userId = USER_ID, spotRecordId = RECORD_ID, reactionType = type)
        ReflectionTestUtils.setField(reaction, "id", 1L)
        return reaction
    }

    private fun count(type: ReactionType, value: Long) = object : ReactionTypeCount {
        override val reactionType: ReactionType = type
        override val count: Long = value
    }

    companion object {
        private const val USER_ID = 1L
        private const val RECORD_ID = 500L
    }
}
