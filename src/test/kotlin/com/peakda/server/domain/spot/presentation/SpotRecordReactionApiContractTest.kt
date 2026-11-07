package com.peakda.server.domain.spot.presentation

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.feed.application.ReactionOverview
import com.peakda.server.domain.feed.application.ReactionTally
import com.peakda.server.domain.spot.application.LegacyReactionOverview
import com.peakda.server.domain.spot.application.SpotDetailService
import com.peakda.server.domain.spot.application.SpotMatcher
import com.peakda.server.domain.spot.application.SpotPreviewService
import com.peakda.server.domain.spot.application.SpotRecordPhotoUploader
import com.peakda.server.domain.spot.application.SpotRecordService
import com.peakda.server.domain.spot.application.SpotRecordView
import com.peakda.server.domain.spot.application.SpotService
import com.peakda.server.domain.spot.application.SpotView
import com.peakda.server.domain.spot.application.UserView
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.presentation.response.SpotDetailResponse
import com.peakda.server.domain.spot.presentation.response.toRecordResponse
import com.peakda.server.domain.spot.presentation.response.toRecordSummaryResponse
import com.peakda.server.domain.user.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.accept.ContentNegotiationManager
import org.springframework.web.accept.FixedContentNegotiationStrategy
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.time.Instant

class SpotRecordReactionApiContractTest {
    private val recordService = mock(SpotRecordService::class.java)
    private val detailService = mock(SpotDetailService::class.java)
    private val principal = mock(PrincipalDetails::class.java)
    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setUp() {
        val user = mock(User::class.java)
        `when`(user.id).thenReturn(7L)
        `when`(principal.getUser()).thenReturn(user)
        mvc = MockMvcBuilders.standaloneSetup(
            SpotRecordController(mock(SpotRecordPhotoUploader::class.java), recordService),
            SpotController(mock(SpotMatcher::class.java), mock(SpotService::class.java), detailService, mock(SpotPreviewService::class.java)),
        )
            .setCustomArgumentResolvers(object : HandlerMethodArgumentResolver {
                override fun supportsParameter(parameter: MethodParameter) = parameter.parameterType == PrincipalDetails::class.java
                override fun resolveArgument(parameter: MethodParameter, container: ModelAndViewContainer?, request: NativeWebRequest, factory: WebDataBinderFactory?): Any? =
                    if (request.getHeader("X-Test-Auth") == "user") principal else null
            })
            .setContentNegotiationManager(ContentNegotiationManager(FixedContentNegotiationStrategy(MediaType.APPLICATION_JSON)))
            .build()
    }

    @Test
    fun `세 스팟 조회 API가 로그인 사용자에게 v2 이모지 응답을 반환한다`() {
        assertContracts(7L)
    }

    @Test
    fun `세 스팟 조회 API가 익명 사용자에게도 이모지 집계를 반환한다`() {
        assertContracts(null)
    }

    @Test
    fun `반응이 없는 기록은 빈 items와 false hasMore를 반환한다`() {
        val response = view().toRecordResponse(ReactionOverview(emptyList(), false))
        `when`(recordService.get(101L, null)).thenReturn(response)

        mvc.perform(get("/api/spots/records/101"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.reactions.items").isEmpty)
            .andExpect(jsonPath("$.data.reactions.hasMore").value(false))
    }

    private fun assertContracts(viewerId: Long?) {
        val reactions = ReactionOverview(listOf(ReactionTally("u:1f525", "🔥", 3L, viewerId != null)), true)
        val summary = view().toRecordSummaryResponse(reactions)
        `when`(recordService.get(101L, viewerId)).thenReturn(view().toRecordResponse(reactions))
        `when`(recordService.listBySpot(501L, viewerId, PageRequest()))
            .thenReturn(PageResponse(listOf(summary), 0, 20, 1, 1, false))
        `when`(detailService.getDetail(501L, viewerId)).thenReturn(
            SpotDetailResponse(
                id = 501L, type = SpotType.LOCAL, name = "남산", address = null,
                latitude = 37.55, longitude = 126.98, attractionId = null,
                representativeImageUrl = null, bloom = null, visitTiming = null, operatingInfo = null,
                recordCount = 1, recordPreview = listOf(summary), favorite = SpotDetailResponse.FavoriteState(false, false),
            ),
        )

        val endpoints = listOf(
            "/api/spots/501" to "$.data.recordPreview[0].reactions",
            "/api/spots/records?spotId=501" to "$.data.content[0].reactions",
            "/api/spots/records/101" to "$.data.reactions",
        )
        for ((url, path) in endpoints) {
            val request = get(url)
            if (viewerId != null) request.header("X-Test-Auth", "user")
            mvc.perform(request)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$path.items[0].key").value("u:1f525"))
                .andExpect(jsonPath("$path.items[0].emoji").value("🔥"))
                .andExpect(jsonPath("$path.items[0].count").value(3))
                .andExpect(jsonPath("$path.items[0].reactedByMe").value(viewerId != null))
                .andExpect(jsonPath("$path.hasMore").value(true))
                .andExpect(jsonPath("$path.counts").doesNotExist())
                .andExpect(jsonPath("$path.myReactions").doesNotExist())
        }
        verify(recordService).get(101L, viewerId)
        verify(recordService).listBySpot(501L, viewerId, PageRequest())
        verify(detailService).getDetail(501L, viewerId)
    }

    private fun view() = SpotRecordView(
        id = 101L, spot = SpotView(501L, SpotType.LOCAL, "남산", null, null), user = UserView(7L, "여행자", null),
        visitedDate = null, bloomStage = null, memo = null, contentLink = null, plants = emptyList(), photos = emptyList(),
        status = SpotRecordStatus.PUBLISHED, publishedAt = Instant.EPOCH, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
        legacyReactions = LegacyReactionOverview(emptyList(), emptySet()),
    )
}
