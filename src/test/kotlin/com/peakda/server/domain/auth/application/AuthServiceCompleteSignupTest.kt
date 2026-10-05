package com.peakda.server.domain.auth.application

import com.peakda.server.common.image.ImageResizer
import com.peakda.server.common.security.cookie.CookieProperties
import com.peakda.server.common.security.jwt.JwtProperties
import com.peakda.server.common.security.jwt.TokenResponse
import com.peakda.server.common.security.principal.SignupSessionPrincipal
import com.peakda.server.common.storage.ObjectKeyUrlResolver
import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.auth.signup.entity.SignupSession
import com.peakda.server.domain.auth.signup.presentation.request.SignupCompleteRequest
import com.peakda.server.domain.auth.signup.repository.SignupSessionRepository
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.user.application.ProfileImageUrlResolver
import com.peakda.server.domain.user.application.UserFavoriteCategoryService
import com.peakda.server.domain.user.entity.SignupChannel
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant

class AuthServiceCompleteSignupTest {

    private val userRepository = mock(UserRepository::class.java)
    private val tokenIssueService = mock(TokenIssueService::class.java)
    private val signupSessionRepository = mock(SignupSessionRepository::class.java)

    private val service = AuthService(
        userRepository = userRepository,
        cookieProperties = CookieProperties(domain = null, secure = false, sameSite = "Lax"),
        jwtProperties = JwtProperties(
            secret = "test-secret",
            accessTokenValidityInSeconds = 1800,
            refreshTokenValidityInSeconds = 1_209_600,
        ),
        refreshTokenService = mock(RefreshTokenService::class.java),
        tokenIssueService = tokenIssueService,
        signupSessionRepository = signupSessionRepository,
        imageResizer = mock(ImageResizer::class.java),
        objectStorage = mock(ObjectStorage::class.java),
        objectKeyUrlResolver = mock(ObjectKeyUrlResolver::class.java),
        profileImageUrlResolver = mock(ProfileImageUrlResolver::class.java),
        userFavoriteCategoryService = mock(UserFavoriteCategoryService::class.java),
    )

    private var savedUser: User? = null

    @BeforeEach
    fun setUp() {
        `when`(userRepository.save(anyUser())).thenAnswer { invocation ->
            invocation.getArgument<User>(0).also {
                ReflectionTestUtils.setField(it, "id", USER_ID)
                savedUser = it
            }
        }
        `when`(tokenIssueService.issue(anyUser())).thenReturn(TokenResponse("Bearer", "access", "refresh"))
    }

    @Test
    fun `가입 세션 토큰을 Bearer 로 보내면 앱 가입으로 남긴다`() {
        val issued = service.completeSignup(principal(), request(), MockHttpServletResponse(), bearerAuthenticated = true)

        assertThat(issued).isNotNull
        assertThat(savedUser?.signupChannel).isEqualTo(SignupChannel.APP)
    }

    @Test
    fun `signup-token 쿠키로 보내면 웹 가입으로 남긴다`() {
        val response = MockHttpServletResponse()

        val issued = service.completeSignup(principal(), request(), response, bearerAuthenticated = false)

        assertThat(issued).isNull()
        assertThat(response.getHeaders("Set-Cookie")).hasSize(3)
        assertThat(savedUser?.signupChannel).isEqualTo(SignupChannel.WEB)
    }

    private fun principal(): SignupSessionPrincipal {
        val session = SignupSession(
            token = "signup-token",
            provider = OAuth2LoginType.KAKAO,
            providerId = "kakao-123",
            email = "traveler@example.com",
            profileImageUrl = null,
            expiresAt = Instant.now().plusSeconds(900),
        )
        ReflectionTestUtils.setField(session, "id", SESSION_ID)
        return SignupSessionPrincipal(session)
    }

    private fun request() = SignupCompleteRequest(
        nickname = "꽃길여행자",
        favoriteCategories = setOf(BloomCategory.CHERRY),
    )

    private fun anyUser(): User = any(User::class.java) ?: PLACEHOLDER_USER

    companion object {
        private const val USER_ID = 31L
        private const val SESSION_ID = 42L
        private val PLACEHOLDER_USER = User(
            provider = OAuth2LoginType.KAKAO,
            providerId = "placeholder",
            nickname = "placeholder",
        )
    }
}
