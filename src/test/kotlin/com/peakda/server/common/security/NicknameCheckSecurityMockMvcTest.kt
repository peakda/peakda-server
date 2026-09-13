package com.peakda.server.common.security

import com.peakda.server.common.exception.GlobalExceptionHandler
import com.peakda.server.common.security.cookie.CookieProperties
import com.peakda.server.common.security.filter.JwtAuthenticationFilter
import com.peakda.server.common.security.jwt.JwtTokenProvider
import com.peakda.server.domain.auth.app.application.AppAuthService
import com.peakda.server.domain.auth.application.AuthService
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.auth.presentation.AuthController
import com.peakda.server.domain.auth.signup.application.SignupSessionService
import com.peakda.server.domain.auth.signup.entity.SignupSession
import com.peakda.server.domain.auth.signup.presentation.response.NicknameCheckResponse
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.post
import org.springframework.web.cors.CorsConfigurationSource
import java.time.Instant
import java.util.Optional

@WebMvcTest(useDefaultFilters = false)
@Import(
    SecurityConfig::class,
    SecurityExceptionConfig::class,
    JwtAuthenticationFilter::class,
    GlobalExceptionHandler::class,
    AuthController::class,
)
class NicknameCheckSecurityMockMvcTest {

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
    lateinit var authService: AuthService

    @MockitoBean
    lateinit var appAuthService: AppAuthService

    private val user = User(
        provider = OAuth2LoginType.KAKAO,
        providerId = "provider-user-42",
        nickname = "existing",
    )

    private val signupSession = SignupSession(
        token = "valid-signup-token",
        provider = OAuth2LoginType.KAKAO,
        providerId = "provider-signup-43",
        expiresAt = Instant.now().plusSeconds(300),
    )

    @BeforeEach
    fun setUp() {
        `when`(cookieProperties.accessTokenName).thenReturn("access-token")
        `when`(cookieProperties.signupTokenName).thenReturn("signup-token")
        `when`(jwtTokenProvider.validateToken("valid-user-token")).thenReturn(true)
        `when`(jwtTokenProvider.getUserIdFromToken("valid-user-token")).thenReturn(42L)
        `when`(userRepository.findById(42L)).thenReturn(Optional.of(user))
        `when`(signupSessionService.findValidByToken("valid-signup-token")).thenReturn(signupSession)
        `when`(authService.checkNickname("peakda")).thenReturn(NicknameCheckResponse(available = true))
    }

    @Test
    fun `nickname check accepts a signed-in user bearer token`() {
        mockMvc.get("/api/auth/signup/nickname/check") {
            param("value", "peakda")
            header("Authorization", "Bearer valid-user-token")
        }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.available") { value(true) } }

        verify(authService).checkNickname("peakda")
    }

    @Test
    fun `nickname check accepts an access token cookie`() {
        mockMvc.get("/api/auth/signup/nickname/check") {
            param("value", "peakda")
            cookie(Cookie("access-token", "valid-user-token"))
        }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.available") { value(true) } }

        verify(authService).checkNickname("peakda")
    }

    @Test
    fun `nickname check accepts a signup bearer token`() {
        mockMvc.get("/api/auth/signup/nickname/check") {
            param("value", "peakda")
            header("Authorization", "Bearer valid-signup-token")
        }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.available") { value(true) } }

        verify(authService).checkNickname("peakda")
    }

    @Test
    fun `nickname check accepts a signup token cookie`() {
        mockMvc.get("/api/auth/signup/nickname/check") {
            param("value", "peakda")
            cookie(Cookie("signup-token", "valid-signup-token"))
        }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.data.available") { value(true) } }

        verify(authService).checkNickname("peakda")
    }

    @Test
    fun `nickname check rejects anonymous and invalid bearer requests`() {
        mockMvc.get("/api/auth/signup/nickname/check") {
            param("value", "peakda")
        }.andExpect { status { isUnauthorized() } }

        mockMvc.get("/api/auth/signup/nickname/check") {
            param("value", "peakda")
            header("Authorization", "Bearer invalid-token")
        }.andExpect { status { isUnauthorized() } }

        verify(authService, never()).checkNickname("peakda")
    }

    @Test
    fun `signed-in users cannot use signup-only completion or image upload endpoints`() {
        mockMvc.post("/api/auth/signup/complete") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer valid-user-token")
            content = """{"nickname":"peakda","favoriteCategories":["CHERRY"]}"""
        }.andExpect { status { isUnauthorized() } }

        val image = MockMultipartFile("image", "avatar.png", "image/png", byteArrayOf(1, 2, 3))
        mockMvc.multipart("/api/auth/signup/profile-image") {
            file(image)
            header("Authorization", "Bearer valid-user-token")
        }.andExpect { status { isUnauthorized() } }

        verifyNoInteractions(authService)
    }
}
