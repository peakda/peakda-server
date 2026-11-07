package com.peakda.server.domain.feed.presentation.v2

import com.peakda.server.domain.spot.application.LegacyReactionCount
import com.peakda.server.domain.feed.application.ReactionOverview
import com.peakda.server.domain.feed.application.ReactionTally
import com.peakda.server.domain.feed.presentation.response.toLegacyResponse
import com.peakda.server.domain.feed.presentation.response.toLegacyFeedRecordResponse
import com.peakda.server.domain.feed.presentation.response.toLegacyFeedRecordSummaryResponse
import com.peakda.server.domain.feed.presentation.v2.response.toFeedRecordResponse
import com.peakda.server.domain.feed.presentation.v2.response.toFeedRecordSummaryResponse
import com.peakda.server.domain.spot.application.ContentLinkView
import com.peakda.server.domain.spot.application.LegacyReactionOverview
import com.peakda.server.domain.spot.application.PhotoView
import com.peakda.server.domain.spot.application.PlantView
import com.peakda.server.domain.spot.application.SpotRecordView
import com.peakda.server.domain.spot.application.SpotView
import com.peakda.server.domain.spot.application.UserView
import com.peakda.server.domain.spot.entity.ContentLinkPlatform
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.feed.application.EmojiCatalog
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class FeedResponseMapperTest {
    private val catalog = EmojiCatalog()
    private val view = SpotRecordView(
        id = 1024L,
        spot = SpotView(11L, SpotType.ATTRACTION, "남산", "서울", 55L),
        user = UserView(22L, "여행자", "https://cdn.example/avatar.jpg"),
        visitedDate = LocalDate.of(2026, 4, 12),
        bloomStage = null,
        memo = "좋았습니다.",
        contentLink = ContentLinkView("https://example.com/post", ContentLinkPlatform.INSTAGRAM),
        plants = listOf(PlantView(31L, "벚나무")),
        photos = listOf(PhotoView("record/main.jpg", "https://cdn.example/main.jpg", 0, mapOf("main" to "https://cdn.example/main.jpg"))),
        status = SpotRecordStatus.PUBLISHED,
        publishedAt = Instant.parse("2026-04-12T09:00:00Z"),
        createdAt = Instant.parse("2026-04-12T08:00:00Z"),
        updatedAt = Instant.parse("2026-04-12T08:30:00Z"),
        legacyReactions = LegacyReactionOverview(
            counts = listOf(LegacyReactionCount("HEART", 2)),
            myReactions = setOf("HEART"),
        ),
    )

    @Test
    fun `v2 detailed and summary responses preserve neutral record content and expose only emoji reactions`() {
        val overview = ReactionOverview(
            items = listOf(
                ReactionTally("u:1f44d", "👍", 12, true),
                ReactionTally("u:1f44d-1f3fd", "👍🏽", 3, false),
            ),
            hasMore = true,
        )

        val detail = view.toFeedRecordResponse(overview)
        val summary = view.toFeedRecordSummaryResponse(overview)

        assertThat(detail.id).isEqualTo(1024L)
        assertThat(detail.spot.name).isEqualTo("남산")
        assertThat(detail.contentLink?.platform).isEqualTo(ContentLinkPlatform.INSTAGRAM)
        assertThat(detail.photos).hasSize(1)
        assertThat(detail.reactions.items).extracting<String> { it.key }
            .containsExactly("u:1f44d", "u:1f44d-1f3fd")
        assertThat(detail.reactions.items[0].reactedByMe).isTrue()
        assertThat(detail.reactions.hasMore).isTrue()
        assertThat(summary.coverPhoto?.objectKey).isEqualTo("record/main.jpg")
        assertThat(summary.reactions).isEqualTo(detail.reactions)
    }

    @Test
    fun `v1 record adapters preserve content and legacy reaction counts`() {
        val detail = view.toLegacyFeedRecordResponse()
        val summary = view.toLegacyFeedRecordSummaryResponse()

        assertThat(detail.spot.id).isEqualTo(view.spot.id)
        assertThat(detail.user.nickname).isEqualTo(view.user.nickname)
        assertThat(detail.contentLink?.url).isEqualTo(view.contentLink?.url)
        assertThat(detail.photos.single().variants).isEqualTo(view.photos.single().variants)
        assertThat(detail.reactions.counts.single().reactionType.name).isEqualTo("HEART")
        assertThat(detail.reactions.counts.single().count).isEqualTo(2L)
        assertThat(detail.reactions.myReactions.single().name).isEqualTo("HEART")
        assertThat(summary.reactions).isEqualTo(detail.reactions)
        assertThat(summary.coverPhoto).isEqualTo(detail.photos.first())
        assertThat(summary.photos).isEqualTo(detail.photos)
    }

    @Test
    fun `v1 adapter keeps legacy enum response and never exposes arbitrary v2 emoji`() {
        val overview = ReactionOverview(
            items = listOf(
                ReactionTally("u:2764-fe0f", "❤️", 5, true),
                ReactionTally("u:1f44d", "👍", 99, true),
            ),
            hasMore = true,
        )

        val response = overview.toLegacyResponse(1024L, catalog)

        assertThat(response.recordId).isEqualTo(1024L)
        assertThat(response.counts).hasSize(1)
        assertThat(response.counts.single().reactionType.name).isEqualTo("HEART")
        assertThat(response.myReactions).containsExactly(com.peakda.server.domain.spot.entity.ReactionType.HEART)
    }
}
