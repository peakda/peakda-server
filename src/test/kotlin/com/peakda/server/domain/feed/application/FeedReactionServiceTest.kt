package com.peakda.server.domain.feed.application

import com.peakda.server.domain.spot.entity.ReactionType
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.exception.SpotRecordNotFoundException
import com.peakda.server.domain.spot.repository.SpotRecordReactionRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.util.ReflectionTestUtils
import java.util.Optional

class FeedReactionServiceTest {
    private val records = mock(SpotRecordRepository::class.java)
    private val reactions = mock(SpotRecordReactionRepository::class.java)
    private val events = mock(ApplicationEventPublisher::class.java)
    private val catalog = EmojiCatalog()
    private val service = FeedReactionService(records, reactions, events, catalog)

    @Test
    fun `duplicate add does not publish an event`() {
        `when`(records.findById(RECORD_ID)).thenReturn(Optional.of(record(SpotRecordStatus.PUBLISHED)))
        `when`(reactions.insertIfAbsent(USER_ID, RECORD_ID, FIRE_KEY)).thenReturn(0)
        `when`(reactions.countByRecordAndStorageKey(RECORD_ID, FIRE_KEY)).thenReturn(1)
        `when`(reactions.existsByRecordUserAndStorageKey(RECORD_ID, USER_ID, FIRE_KEY)).thenReturn(true)

        val result = service.add(USER_ID, RECORD_ID, FIRE_KEY)

        assertThat(result.reactedByMe).isTrue
        verifyNoInteractions(events)
    }

    @Test
    fun `new insert publishes canonical emoji event`() {
        `when`(records.findById(RECORD_ID)).thenReturn(Optional.of(record(SpotRecordStatus.PUBLISHED)))
        `when`(reactions.insertIfAbsent(USER_ID, RECORD_ID, FIRE_KEY)).thenReturn(1)
        `when`(reactions.countByRecordAndStorageKey(RECORD_ID, FIRE_KEY)).thenReturn(1)
        `when`(reactions.existsByRecordUserAndStorageKey(RECORD_ID, USER_ID, FIRE_KEY)).thenReturn(true)

        service.add(USER_ID, RECORD_ID, FIRE_KEY)

        val captor = ArgumentCaptor.forClass(ReactionAddedEvent::class.java)
        verify(events).publishEvent(captor.capture())
        assertThat(captor.value.emojiKey).isEqualTo(FIRE_KEY)
    }

    @Test
    fun `legacy and all supported emoji are accepted together without activation settings`() {
        `when`(records.findById(RECORD_ID)).thenReturn(Optional.of(record(SpotRecordStatus.PUBLISHED)))
        listOf(HEART_KEY, FIRE_KEY, "u:1f44d-1f3fd", "u:1f468-200d-1f469-200d-1f467-200d-1f466").forEach { key ->
            val storageKey = catalog.toStorageKey(key)
            `when`(reactions.insertIfAbsent(USER_ID, RECORD_ID, storageKey)).thenReturn(1)
            `when`(reactions.countByRecordAndStorageKey(RECORD_ID, storageKey)).thenReturn(1)
            `when`(reactions.existsByRecordUserAndStorageKey(RECORD_ID, USER_ID, storageKey)).thenReturn(true)

            val result = service.add(USER_ID, RECORD_ID, key)

            assertThat(result.key).isEqualTo(key)
            assertThat(result.reactedByMe).isTrue()
        }
    }

    @Test
    fun `invalid list size is a bad request`() {
        `when`(records.findById(RECORD_ID)).thenReturn(Optional.of(record(SpotRecordStatus.PUBLISHED)))
        assertThatThrownBy { service.list(RECORD_ID, USER_ID, null, 0, false) }
            .isInstanceOf(InvalidEmojiException::class.java)
    }

    @Test
    fun `remove missing reaction is idempotent`() {
        `when`(records.findById(RECORD_ID)).thenReturn(Optional.of(record(SpotRecordStatus.PUBLISHED)))
        `when`(reactions.countByRecordAndStorageKey(RECORD_ID, FIRE_KEY)).thenReturn(0)
        `when`(reactions.existsByRecordUserAndStorageKey(RECORD_ID, USER_ID, FIRE_KEY)).thenReturn(false)

        val result = service.remove(USER_ID, RECORD_ID, FIRE_KEY)

        assertThat(result.count).isZero
        assertThat(result.reactedByMe).isFalse
        verify(reactions).deleteByUserIdAndSpotRecordIdAndReactionType(USER_ID, RECORD_ID, FIRE_KEY)
    }

    @Test
    fun `draft and missing records remain hidden even for an invalid emoji key`() {
        listOf(Optional.of(record(SpotRecordStatus.DRAFT)), Optional.empty<SpotRecord>()).forEach { hidden ->
            `when`(records.findById(RECORD_ID)).thenReturn(hidden)
            assertThatThrownBy { service.add(USER_ID, RECORD_ID, "invalid") }
                .isInstanceOf(SpotRecordNotFoundException::class.java)
            assertThatThrownBy { service.remove(USER_ID, RECORD_ID, "invalid") }
                .isInstanceOf(SpotRecordNotFoundException::class.java)
        }
        verifyNoInteractions(reactions, events)
    }

    @Test
    fun `list cursor uses last returned key and does not skip`() {
        `when`(records.findById(RECORD_ID)).thenReturn(Optional.of(record(SpotRecordStatus.PUBLISHED)))
        val first = row("u:1f44d", 2, false)
        val second = row("u:1f525", 1, true)
        `when`(reactions.emojiList(RECORD_ID, USER_ID, null, false, 2)).thenReturn(listOf(first, second))

        val page = service.list(RECORD_ID, USER_ID, null, 1, false)

        assertThat(page.items).extracting<String> { it.key }.containsExactly("u:1f44d")
        assertThat(page.hasMore).isTrue
        assertThat(page.nextAfterKey).isEqualTo("u:1f44d")
    }

    private fun row(key: String, count: Long, mine: Boolean) = object : com.peakda.server.domain.spot.repository.EmojiReactionRow {
        override val storageKey = key
        override val count = count
        override val reactedByMe = mine
    }

    private fun record(status: SpotRecordStatus): SpotRecord = SpotRecord(spotId = 100L, userId = 999L, status = status).also {
        ReflectionTestUtils.setField(it, "id", RECORD_ID)
    }

    companion object {
        private const val USER_ID = 1L
        private const val RECORD_ID = 500L
        private const val HEART_KEY = "u:2764-fe0f"
        private const val SMILE_KEY = "u:1f600"
        private const val FIRE_KEY = "u:1f525"
    }
}
