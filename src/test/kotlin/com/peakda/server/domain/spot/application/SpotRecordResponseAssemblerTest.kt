package com.peakda.server.domain.spot.application

import com.peakda.server.domain.feed.application.EmojiCatalog
import com.peakda.server.domain.feed.application.FeedReactionService
import com.peakda.server.domain.spot.repository.EmojiReactionCount
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import org.springframework.context.ApplicationEventPublisher
import com.peakda.server.domain.user.application.ProfileImageUrlResolver
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
    private val profileImageUrlResolver = mock(ProfileImageUrlResolver::class.java)

    private val assembler = SpotRecordResponseAssembler(
        spotRepository,
        userRepository,
        plantRepository,
        spotRecordPhotoRepository,
        spotRecordPlantRepository,
        spotRecordReactionRepository,
        spotRecordPhotoUrlResolver,
        profileImageUrlResolver,
        FeedReactionService(
            mock(SpotRecordRepository::class.java),
            spotRecordReactionRepository,
            mock(ApplicationEventPublisher::class.java),
            EmojiCatalog(),
        ),
    )

    @Test
    fun `기록 상세에 리액션 집계와 내 리액션을 채운다`() {
        val record = record(101L)
        stubCommon(listOf(record))
        `when`(spotRecordReactionRepository.emojiPreviews(listOf(101L), VIEWER_ID)).thenReturn(
            listOf(reaction(101L, "u:1f525", 3, true), reaction(101L, "HEART", 2, false)),
        )

        val response = assembler.assemble(record, VIEWER_ID)

        assertThat(response.reactions.items.map { it.key }).containsExactly("u:1f525", "u:2764-fe0f")
        assertThat(response.reactions.items.map { it.emoji }).containsExactly("🔥", "❤️")
        assertThat(response.reactions.items.map { it.count }).containsExactly(3L, 2L)
        assertThat(response.reactions.items.map { it.reactedByMe }).containsExactly(true, false)
        assertThat(response.reactions.hasMore).isFalse()
    }

    @Test
    fun `목록 리액션은 배치 조회하고 다른 이모지만 달린 기록도 반환한다`() {
        val records = listOf(record(101L), record(102L), record(103L))
        stubCommon(records)
        `when`(spotRecordReactionRepository.emojiPreviews(listOf(101L, 102L, 103L), VIEWER_ID)).thenReturn(
            listOf(reaction(101L, "u:1f525", 2, true), reaction(102L, "u:1f44d-1f3fd", 1, false)),
        )

        val responses = assembler.assembleSummaries(records, VIEWER_ID)

        assertThat(responses).hasSize(3)
        assertThat(responses[0].reactions.items.single().emoji).isEqualTo("🔥")
        assertThat(responses[1].reactions.items.single().emoji).isEqualTo("👍🏽")
        assertThat(responses[2].reactions.items).isEmpty()
        assertThat(responses[2].reactions.hasMore).isFalse()
        verify(spotRecordReactionRepository).emojiPreviews(listOf(101L, 102L, 103L), VIEWER_ID)
        verify(spotRecordReactionRepository, never()).countsBySpotRecordIdIn(org.mockito.ArgumentMatchers.anyList())
        verify(spotRecordReactionRepository, never()).findByUserIdAndSpotRecordIdIn(anyLong(), org.mockito.ArgumentMatchers.anyList())
    }

    @Test
    fun `익명 조회도 이모지 집계를 반환하고 reactedByMe는 false다`() {
        val record = record(101L)
        stubCommon(listOf(record))
        `when`(spotRecordReactionRepository.emojiPreviews(listOf(101L), null)).thenReturn(
            listOf(reaction(101L, "u:1f525", 1, false)),
        )

        val response = assembler.assembleSummaries(listOf(record), null).single()

        assertThat(response.reactions.items.single().key).isEqualTo("u:1f525")
        assertThat(response.reactions.items.single().reactedByMe).isFalse()
        verify(spotRecordReactionRepository).emojiPreviews(listOf(101L), null)
    }

    @Test
    fun `반응 종류가 8개를 넘으면 최대 8개와 hasMore를 반환한다`() {
        val record = record(101L)
        stubCommon(listOf(record))
        val keys = listOf("HEART", "SMILE", "u:1f525", "u:1f44d", "u:1f44d-1f3fd", "u:1f600", "u:1f601", "u:1f602", "u:1f603")
        `when`(spotRecordReactionRepository.emojiPreviews(listOf(101L), VIEWER_ID))
            .thenReturn(keys.mapIndexed { index, key -> reaction(101L, key, (10 - index).toLong(), false) })

        val detail = assembler.assemble(record, VIEWER_ID)
        val summary = assembler.assembleSummaries(listOf(record), VIEWER_ID).single()

        assertThat(detail.reactions.items).hasSize(8)
        assertThat(detail.reactions.hasMore).isTrue()
        assertThat(summary.reactions).isEqualTo(detail.reactions)
    }

    @Test
    fun `빈 목록은 리액션을 조회하지 않는다`() {
        assertThat(assembler.assembleSummaries(emptyList(), VIEWER_ID)).isEmpty()
        org.mockito.Mockito.verifyNoInteractions(spotRecordReactionRepository)
    }

    @Test
    fun `v1 피드용 중립 뷰는 기존 하트 웃음 집계를 유지한다`() {
        val record = record(101L)
        stubCommon(listOf(record))
        val count = object : RecordReactionTypeCount {
            override val spotRecordId = 101L
            override val reactionType = "HEART"
            override val count = 3L
        }
        `when`(spotRecordReactionRepository.countsBySpotRecordIdIn(listOf(101L))).thenReturn(listOf(count))
        `when`(spotRecordReactionRepository.findByUserIdAndSpotRecordIdIn(VIEWER_ID, listOf(101L)))
            .thenReturn(listOf(SpotRecordReaction(VIEWER_ID, 101L, ReactionType.HEART)))

        val view = assembler.assembleView(record, VIEWER_ID)

        assertThat(view.legacyReactions.counts.single().count).isEqualTo(3L)
        assertThat(view.legacyReactions.myReactions).containsExactly("HEART")
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
        `when`(profileImageUrlResolver.thumbnailUrl(null)).thenReturn(null)
    }

    private fun reaction(recordId: Long, key: String, count: Long, mine: Boolean) = object : EmojiReactionCount {
        override val spotRecordId = recordId
        override val storageKey = key
        override val count = count
        override val reactedByMe = mine
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
