package com.peakda.server.common.security

import com.peakda.server.common.security.filter.JwtAuthenticationFilter
import com.peakda.server.common.security.cookie.CookieProperties
import com.peakda.server.common.security.jwt.JwtTokenProvider
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.domain.feed.application.FeedReactionService
import com.peakda.server.domain.feed.application.FeedService
import com.peakda.server.domain.feed.entity.FeedFilter
import com.peakda.server.domain.feed.presentation.FeedController
import com.peakda.server.domain.auth.signup.application.SignupSessionService
import com.peakda.server.domain.user.repository.UserRepository
import com.peakda.server.domain.spot.presentation.response.SpotRecordSummaryResponse
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.context.annotation.Import
import org.springframework.http.ResponseEntity
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.cors.CorsConfigurationSource

@WebMvcTest(useDefaultFilters = false)
@Import(
    SecurityConfig::class,
    SecurityExceptionConfig::class,
    JwtAuthenticationFilter::class,
    com.peakda.server.common.exception.GlobalExceptionHandler::class,
    FeedController::class,
    SecurityConfigMockMvcTest.SecurityProbeController::class,
)
class SecurityConfigMockMvcTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean(name = "corsConfigurationSource")
    lateinit var corsConfigurationSource: CorsConfigurationSource

    @MockitoBean
    lateinit var clientRegistrationRepository: org.springframework.security.oauth2.client.registration.ClientRegistrationRepository

    @MockitoBean
    lateinit var oAuth2SecurityConfig: OAuth2SecurityConfig

    @MockitoBean
    lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    lateinit var cookieProperties: CookieProperties

    @MockitoBean
    lateinit var userRepository: UserRepository

    @MockitoBean
    lateinit var signupSessionService: SignupSessionService

    @MockitoBean
    lateinit var feedService: FeedService

    @MockitoBean
    lateinit var feedReactionService: FeedReactionService

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        `when`(feedService.list(null, FeedFilter.ALL, PageRequest()))
            .thenReturn(PageResponse(emptyList<SpotRecordSummaryResponse>(), 0, 20, 0, 0, false))
    }

    @Test
    fun `anonymous users can browse public detail pages`() {
        mockMvc.get("/api/spots/123")
            .andExpect { status { isOk() } }
        mockMvc.get("/api/spots/records/123")
            .andExpect { status { isOk() } }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = [
        "/api/home/suggestion", "/api/explore", "/api/explore/spots", "/api/explore/festivals",
        "/api/search/spots", "/api/search/trending", "/api/seasonal/blooms",
        "/api/seasonal/blooms/peak", "/api/seasonal/blooms/calendar", "/api/plants",
        "/api/plants/search", "/api/spots/preview", "/api/spots/records",
    ])
    fun `public browsing GET routes are permitted`(path: String) {
        mockMvc.get(path).andExpect { status { isOk() } }
        mockMvc.post(path).andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `invalid bearer token still allows browsing but cannot access personal data`() {
        mockMvc.get("/api/feed") { header("Authorization", "Bearer invalid") }
            .andExpect { status { isOk() } }
        mockMvc.get("/api/users/me") { header("Authorization", "Bearer invalid") }
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `public feed binds a null principal for anonymous requests`() {
        mockMvc.get("/api/feed").andExpect { status { isOk() } }

        verify(feedService).list(null, FeedFilter.ALL, PageRequest())
    }

    @Test
    fun `detail matcher only permits numeric ids`() {
        mockMvc.get("/api/spots/not-a-number")
            .andExpect { status { isUnauthorized() } }
        mockMvc.get("/api/spots/records/me")
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `anonymous users cannot access personal or mutation endpoints`() {
        mockMvc.get("/api/users/me")
            .andExpect { status { isUnauthorized() } }
        for (path in listOf("/api/spots/favorites", "/api/search/users", "/api/users/me/blocks")) {
            mockMvc.get(path).andExpect { status { isUnauthorized() } }
        }
        mockMvc.post("/api/spots/records").andExpect { status { isUnauthorized() } }
        mockMvc.post("/api/spots/favorites")
            .andExpect { status { isUnauthorized() } }
        mockMvc.post("/api/feed/123/reactions")
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `authenticated users can access personal endpoints`() {
        mockMvc.get("/api/users/me")
            .andExpect { status { isOk() } }
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `regular users cannot access admin endpoints`() {
        mockMvc.get("/api/admin/ping")
            .andExpect { status { isForbidden() } }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `admins can access admin endpoints`() {
        mockMvc.get("/api/admin/ping")
            .andExpect { status { isOk() } }
    }

    @RestController
    @RequestMapping
    class SecurityProbeController {

        @GetMapping(
            "/api/home/suggestion", "/api/explore", "/api/explore/spots", "/api/explore/festivals",
            "/api/search/spots", "/api/search/trending", "/api/seasonal/blooms",
            "/api/seasonal/blooms/peak", "/api/seasonal/blooms/calendar", "/api/plants",
            "/api/plants/search", "/api/spots/preview", "/api/spots/records",
        )
        fun browse(): ResponseEntity<Unit> = ResponseEntity.ok().build()

        @GetMapping("/api/spots/{id}")
        fun spot(@PathVariable id: String): ResponseEntity<Unit> = ResponseEntity.ok().build()

        @GetMapping("/api/spots/records/{id}")
        fun record(@PathVariable id: String): ResponseEntity<Unit> = ResponseEntity.ok().build()

        @GetMapping("/api/users/me", "/api/auth/me")
        fun me(): ResponseEntity<Unit> = ResponseEntity.ok().build()

        @PostMapping("/api/spots/favorites")
        fun mutate(): ResponseEntity<Unit> = ResponseEntity.ok().build()

        @GetMapping("/api/admin/ping")
        fun admin(): ResponseEntity<Unit> = ResponseEntity.ok().build()
    }
}
