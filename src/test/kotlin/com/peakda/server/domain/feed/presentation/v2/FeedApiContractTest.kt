package com.peakda.server.domain.feed.presentation.v2

import com.peakda.server.common.exception.GlobalExceptionHandler
import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.feed.application.EmojiCatalog
import com.peakda.server.domain.feed.application.FeedReactionService
import com.peakda.server.domain.feed.application.FeedService
import com.peakda.server.domain.feed.application.InvalidEmojiException
import com.peakda.server.domain.feed.application.ReactionOverview
import com.peakda.server.domain.feed.application.ReactionState
import com.peakda.server.domain.feed.application.ReactionTally
import com.peakda.server.domain.feed.presentation.FeedController
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
import com.peakda.server.domain.spot.exception.SpotRecordNotFoundException
import com.peakda.server.domain.user.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.core.MethodParameter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.http.MediaType
import org.springframework.web.accept.ContentNegotiationManager
import org.springframework.web.accept.FixedContentNegotiationStrategy
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import java.time.LocalDate

class FeedApiContractTest {
    private val feedService = Mockito.mock(FeedService::class.java)
    private val reactionService = Mockito.mock(FeedReactionService::class.java)
    private val catalog = EmojiCatalog()
    private val user = Mockito.mock(User::class.java)
    private val principal = Mockito.mock(PrincipalDetails::class.java)
    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setUp() {
        Mockito.`when`(user.id).thenReturn(7L)
        Mockito.`when`(principal.getUser()).thenReturn(user)
        val view = view()
        Mockito.`when`(feedService.list(null, com.peakda.server.domain.feed.entity.FeedFilter.ALL, com.peakda.server.common.page.PageRequest()))
            .thenReturn(PageResponse(listOf(view), 0, 20, 1, 1, false))
        Mockito.`when`(feedService.detail(1024L, null)).thenReturn(view)
        Mockito.`when`(feedService.detail(1024L, 7L)).thenReturn(view)
        Mockito.`when`(reactionService.previews(listOf(1024L), null)).thenReturn(
            mapOf(1024L to ReactionOverview(listOf(ReactionTally("u:1f44d", "👍", 12, true)), false)),
        )
        Mockito.`when`(reactionService.add(7L, 1024L, "u:1f44d"))
            .thenReturn(ReactionState(1024L, "u:1f44d", "👍", 13, true))
        Mockito.`when`(reactionService.add(7L, 1024L, "u:2764-fe0f"))
            .thenReturn(ReactionState(1024L, "u:2764-fe0f", "❤️", 1, true))
        Mockito.`when`(reactionService.remove(7L, 1024L, "u:1f44d"))
            .thenReturn(ReactionState(1024L, "u:1f44d", "👍", 12, false))
        Mockito.`when`(reactionService.list(1024L, null, null, 50, true))
            .thenReturn(com.peakda.server.domain.feed.application.ReactionPage(emptyList(), false, null))

        val v1 = FeedController(feedService, reactionService, catalog)
        val v2 = FeedV2Controller(feedService, reactionService)
        mvc = MockMvcBuilders.standaloneSetup(v1, v2)
            .setCustomArgumentResolvers(PrincipalResolver(principal))
            .setContentNegotiationManager(ContentNegotiationManager(FixedContentNegotiationStrategy(MediaType.APPLICATION_JSON)))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `v1 list keeps legacy reaction envelope while v2 list exposes emoji reaction envelope`() {
        mvc.perform(get("/api/feed").param("filter", "ALL"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].reactions.counts").exists())
            .andExpect(jsonPath("$.data.content[0].reactions.myReactions").exists())
            .andExpect(jsonPath("$.data.content[0].reactions.items").doesNotExist())

        mvc.perform(get("/api/v2/feed").param("filter", "ALL"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].reactions.items[0].key").value("u:1f44d"))
            .andExpect(jsonPath("$.data.content[0].reactions.items[0].emoji").value("👍"))
            .andExpect(jsonPath("$.data.content[0].reactions.items[0].reactedByMe").value(true))
            .andExpect(jsonPath("$.data.content[0].reactions.counts").doesNotExist())
    }

    @Test
    fun `v1 detail keeps legacy reaction envelope`() {
        mvc.perform(get("/api/feed/1024"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.spot.id").value(11))
            .andExpect(jsonPath("$.data.reactions.counts").exists())
            .andExpect(jsonPath("$.data.reactions.myReactions").exists())
            .andExpect(jsonPath("$.data.reactions.items").doesNotExist())
    }

    @Test
    fun `v1 and v2 mutation contracts map to the same canonical key`() {
        Mockito.`when`(reactionService.legacySummary(1024L, 7L)).thenReturn(
            ReactionOverview(listOf(ReactionTally("u:2764-fe0f", "❤️", 1, true)), false),
        )

        mvc.perform(post("/api/feed/1024/reactions").param("reactionType", "HEART").header("X-Test-Auth", "user"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.counts[0].reactionType").value("HEART"))
        mvc.perform(put("/api/v2/feed/1024/reactions/u:2764-fe0f").header("X-Test-Auth", "user"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.key").value("u:2764-fe0f"))
            .andExpect(jsonPath("$.data.emoji").value("❤️"))

        Mockito.verify(reactionService, Mockito.times(2)).add(7L, 1024L, "u:2764-fe0f")
    }

    @Test
    fun `v2 reactions list returns keyset page and anonymous mineOnly is passed as anonymous`() {
        mvc.perform(get("/api/v2/feed/1024/reactions").param("size", "50").param("mineOnly", "true"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items").isEmpty)
            .andExpect(jsonPath("$.data.nextAfterKey").doesNotExist())

        Mockito.verify(reactionService).list(1024L, null, null, 50, true)
    }

    @Test
    fun `v2 mutation maps invalid key, and unpublished record to their API errors`() {
        Mockito.doThrow(InvalidEmojiException()).`when`(reactionService).add(7L, 1024L, "u:not-real")
        mvc.perform(put("/api/v2/feed/1024/reactions/u:not-real").header("X-Test-Auth", "user"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.name))

        Mockito.doThrow(SpotRecordNotFoundException()).`when`(reactionService).add(7L, 1024L, "u:1f44d")
        mvc.perform(put("/api/v2/feed/1024/reactions/u:1f44d").header("X-Test-Auth", "user"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value(ErrorCode.SPOT_RECORD_NOT_FOUND.name))

    }


    private fun view() = SpotRecordView(
        id = 1024L,
        spot = SpotView(11L, SpotType.ATTRACTION, "남산", "서울", 55L),
        user = UserView(22L, "여행자", null),
        visitedDate = LocalDate.of(2026, 4, 12),
        bloomStage = null,
        memo = "좋습니다",
        contentLink = ContentLinkView("https://example.com", ContentLinkPlatform.INSTAGRAM),
        plants = listOf(PlantView(31L, "벚나무")),
        photos = listOf(PhotoView("main.jpg", "https://cdn/main.jpg", 0, emptyMap())),
        status = SpotRecordStatus.PUBLISHED,
        publishedAt = Instant.parse("2026-04-12T09:00:00Z"),
        createdAt = Instant.parse("2026-04-12T08:00:00Z"),
        updatedAt = Instant.parse("2026-04-12T08:30:00Z"),
        legacyReactions = LegacyReactionOverview(emptyList(), emptySet()),
    )

    private class PrincipalResolver(private val principal: PrincipalDetails) : HandlerMethodArgumentResolver {
        override fun supportsParameter(parameter: MethodParameter): Boolean = parameter.parameterType == PrincipalDetails::class.java
        override fun resolveArgument(parameter: MethodParameter, mavContainer: ModelAndViewContainer?, webRequest: NativeWebRequest, binderFactory: WebDataBinderFactory?): Any? =
            if (webRequest.getHeader("X-Test-Auth") == "user") principal else null
    }
}
