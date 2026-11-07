package com.peakda.server.domain.spot.repository

import com.peakda.server.common.test.IntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.peakda.server.domain.feed.application.EmojiCatalog

class SpotRecordReactionRepositoryIntegrationTest : IntegrationTestSupport() {
    @Autowired lateinit var repository: SpotRecordReactionRepository
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var transactions: TransactionTemplate

    @BeforeEach
    fun clean() {
        jdbc.update("DELETE FROM spot_record_reactions")
    }

    @AfterEach
    fun cleanup() {
        jdbc.update("DELETE FROM spot_record_reactions")
    }

    @Test
    fun `concurrent same reaction inserts one row`() {
        val pool = Executors.newFixedThreadPool(20)
        val ready = CountDownLatch(20)
        val start = CountDownLatch(1)
        try {
            val results = (1..20).map {
                pool.submit(Callable {
                    ready.countDown()
                    check(start.await(30, TimeUnit.SECONDS))
                    transactions.execute { repository.insertIfAbsent(1L, 700L, "u:1f525") } ?: 0
                })
            }
            assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue()
            start.countDown()
            val inserted = results.sumOf { it.get(30, TimeUnit.SECONDS) }
            assertThat(inserted).isEqualTo(1)
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM spot_record_reactions", Long::class.java)).isEqualTo(1)
        } finally {
            start.countDown()
            pool.shutdownNow()
        }
    }

    @Test
    fun `emoji list uses canonical legacy ordering cursor and mine filter`() {
        insert(1L, 701L, "HEART")
        insert(2L, 701L, "SMILE")
        insert(1L, 701L, "u:1f525")
        insert(3L, 701L, "u:1f525")
        val first = repository.emojiList(701L, 1L, null, false, 2)
        assertThat(first).extracting<String> { it.storageKey }
            .containsExactly("u:1f525", "SMILE")

        val after = repository.emojiList(701L, 1L, "u:1f600", false, 2)
        assertThat(after).extracting<String> { it.storageKey }.containsExactly("HEART")

        val mineOnly = repository.emojiList(701L, 1L, null, true, 10)
        assertThat(mineOnly).extracting<String> { it.storageKey }
            .containsExactly("u:1f525", "HEART")
        assertThat(mineOnly.first().count).isEqualTo(2L)
        assertThat(repository.emojiList(701L, null, null, true, 10)).isEmpty()
        assertThat(repository.countsBySpotRecordId(701L).map { it.reactionType })
            .containsExactlyInAnyOrder("HEART", "SMILE")
        assertThat(repository.findBySpotRecordIdAndUserId(701L, 1L).map { it.reactionType })
            .containsExactly("HEART")
    }

    @Test
    fun `preview returns ninth row as has more boundary`() {
        (1..9).forEach { index -> insert(index.toLong(), 702L, "u:1f${500 + index}") }
        val rows = repository.emojiPreviews(listOf(702L), null)
        assertThat(rows).hasSize(9)
        assertThat(rows.map { it.spotRecordId }.distinct()).containsExactly(702L)
    }

    @Test
    fun `preview bounds the returned groups for a batch of records with thousands of emoji`() {
        val catalog = EmojiCatalog()
        val rows = listOf(800L, 801L).flatMap { recordId ->
            catalog.items.take(1000).map { arrayOf<Any>(1L, recordId, catalog.toStorageKey(it.key)) }
        }
        jdbc.batchUpdate(
            "INSERT INTO spot_record_reactions (user_id, spot_record_id, reaction_type, created_at, updated_at) VALUES (?, ?, ?, now(), now())",
            rows,
        )
        val preview = repository.emojiPreviews(listOf(800L, 801L), 1L)
        assertThat(preview).hasSize(18)
        assertThat(preview.groupingBy { it.spotRecordId }.eachCount()).containsEntry(800L, 9).containsEntry(801L, 9)
        assertThat(preview).allMatch { it.count == 1L && it.reactedByMe }
    }

    private fun insert(userId: Long, recordId: Long, key: String) {
        transactions.executeWithoutResult { repository.insertIfAbsent(userId, recordId, key) }
    }
}
