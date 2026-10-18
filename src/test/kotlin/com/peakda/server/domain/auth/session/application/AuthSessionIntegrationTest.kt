package com.peakda.server.domain.auth.session.application

import com.peakda.server.common.exception.AuthorizationException
import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.security.jwt.JwtProperties
import com.peakda.server.common.security.jwt.TokenResponse
import com.peakda.server.common.test.IntegrationTestSupport
import com.peakda.server.domain.auth.application.RotatedTokens
import com.peakda.server.domain.auth.application.TokenIssueService
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.auth.session.entity.SessionClientType
import com.peakda.server.domain.auth.session.entity.SessionPlatform
import com.peakda.server.domain.auth.session.repository.AuthSessionRepository
import com.peakda.server.common.security.jwt.JwtTokenProvider
import com.peakda.server.domain.user.entity.SignupChannel
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 세션 단위 발급·회전을 실제 DB 로 확인한다. Redis 는 [AuthSessionCache] 목을 메모리 맵으로 대신한다.
 */
@RecordApplicationEvents
class AuthSessionIntegrationTest : IntegrationTestSupport() {

    @Autowired
    lateinit var tokenIssueService: TokenIssueService

    @Autowired
    lateinit var authSessionService: AuthSessionService

    @Autowired
    lateinit var authSessionRepository: AuthSessionRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    lateinit var jwtProperties: JwtProperties

    @Autowired
    lateinit var events: ApplicationEvents

    private val rotations = ConcurrentHashMap<String, TokenResponse>()
    private val migrations = ConcurrentHashMap<String, RotatedTokens>()
    private lateinit var user: User

    @BeforeEach
    fun setUp() {
        authSessionRepository.deleteAll()
        userRepository.deleteAll()
        user = userRepository.saveAndFlush(
            User.create(
                provider = OAuth2LoginType.KAKAO,
                providerId = "session-user-${UUID.randomUUID()}",
                nickname = "세션사용자",
                email = null,
                profileImageUrl = null,
                signupChannel = SignupChannel.APP,
            ),
        )
        rotations.clear()
        migrations.clear()
        doAnswer { invocation ->
            rotations["${invocation.getArgument<UUID>(0)}:${invocation.getArgument<String>(1)}"] =
                invocation.getArgument(2)
            null
        }.`when`(authSessionCache).saveRotation(anyUuid(), anyString(), anyTokens(), anyDuration())
        doAnswer { invocation ->
            rotations["${invocation.getArgument<UUID>(0)}:${invocation.getArgument<String>(1)}"]
        }.`when`(authSessionCache).findRotation(anyUuid(), anyString())
        doAnswer { invocation ->
            migrations["${invocation.getArgument<Long>(0)}:${invocation.getArgument<String>(1)}"] =
                invocation.getArgument(2)
            null
        }.`when`(authSessionCache).saveLegacyMigration(anyLong(), anyString(), anyRotated(), anyDuration())
        doAnswer { invocation ->
            migrations["${invocation.getArgument<Long>(0)}:${invocation.getArgument<String>(1)}"]
        }.`when`(authSessionCache).findLegacyMigration(anyLong(), anyString())
    }

    @Test
    fun `다른 기기에서 로그인해도 기존 기기의 재발급은 유지된다`() {
        val android = tokenIssueService.issue(user, app("android-1"))
        tokenIssueService.issue(user, SessionDevice.web(null, CHROME_MAC))

        val rotated = tokenIssueService.rotate(android.refreshToken, app("android-1")).tokens

        assertThat(rotated.refreshToken).isNotEqualTo(android.refreshToken)
        assertThat(authSessionRepository.findByUserId(userId())).hasSize(2)
    }

    @Test
    fun `앱 세션은 30일, 웹 세션은 7일짜리 리프레시 토큰을 받는다`() {
        val app = tokenIssueService.issue(user, app("android-1"))
        val web = tokenIssueService.issue(user, SessionDevice.web(null, CHROME_MAC))

        assertThat(app.refreshTokenExpiresIn).isEqualTo(jwtProperties.appRefreshTokenValidityInSeconds)
        assertThat(web.refreshTokenExpiresIn).isEqualTo(jwtProperties.refreshTokenValidityInSeconds)
        val claims = jwtTokenProvider.parse(app.accessToken)!!
        assertThat(claims.refresh).isFalse()
        assertThat(jwtTokenProvider.parse(app.refreshToken)!!.refresh).isTrue()
        assertThat(authSessionRepository.findById(claims.sessionId!!).get().clientType).isEqualTo(SessionClientType.APP)
    }

    @Test
    fun `직전 토큰으로 곧바로 다시 재발급하면 같은 토큰 쌍을 돌려주고 세션을 유지한다`() {
        val issued = tokenIssueService.issue(user, app("android-1"))

        val first = tokenIssueService.rotate(issued.refreshToken, app("android-1")).tokens
        val retried = tokenIssueService.rotate(issued.refreshToken, app("android-1")).tokens

        assertThat(retried).isEqualTo(first)
        assertThat(tokenIssueService.rotate(first.refreshToken, app("android-1")).tokens.refreshToken)
            .isNotEqualTo(first.refreshToken)
    }

    @Test
    fun `같은 리프레시 토큰으로 동시에 재발급해도 모두 같은 토큰 쌍을 받는다`() {
        val issued = tokenIssueService.issue(user, app("android-1"))
        val taskCount = 5
        val ready = CountDownLatch(taskCount)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(taskCount)

        val results = try {
            val futures = (1..taskCount).map {
                executor.submit<TokenResponse> {
                    ready.countDown()
                    start.await()
                    tokenIssueService.rotate(issued.refreshToken, app("android-1")).tokens
                }
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue()
            start.countDown()
            futures.map { it.get(10, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }

        assertThat(results.map { it.refreshToken }.toSet()).hasSize(1)
        assertThat(authSessionRepository.findByUserId(userId())).hasSize(1)
    }

    @Test
    fun `두 번 이상 지난 토큰을 다시 쓰면 그 세션만 폐기한다`() {
        val android = tokenIssueService.issue(user, app("android-1"))
        val web = tokenIssueService.issue(user, SessionDevice.web(null, CHROME_MAC))
        val second = tokenIssueService.rotate(android.refreshToken, app("android-1")).tokens
        tokenIssueService.rotate(second.refreshToken, app("android-1")).tokens

        assertThatThrownBy { tokenIssueService.rotate(android.refreshToken, app("android-1")).tokens }
            .isInstanceOf(AuthorizationException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_INVALID)

        val remaining = authSessionRepository.findByUserId(userId())
        assertThat(remaining).hasSize(1)
        assertThat(remaining.single().id).isEqualTo(jwtTokenProvider.parse(web.refreshToken)!!.sessionId)
        assertThat(events.stream(AuthSessionRevokedEvent::class.java).map { it.deviceId }).containsExactly("android-1")
    }

    @Test
    fun `액세스 토큰으로는 재발급하지 못한다`() {
        val issued = tokenIssueService.issue(user, app("android-1"))

        assertThatThrownBy { tokenIssueService.rotate(issued.accessToken, app("android-1")).tokens }
            .isInstanceOf(AuthorizationException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_INVALID)
        assertThat(authSessionRepository.findByUserId(userId())).hasSize(1)
    }

    @Test
    fun `여섯 번째 기기로 로그인하면 가장 오래 안 쓴 기기가 해제된다`() {
        val first = tokenIssueService.issue(user, app("device-1"))
        (2..5).forEach { tokenIssueService.issue(user, app("device-$it")) }
        // device-1 을 다시 쓰면 가장 오래 안 쓴 기기는 device-2 가 된다.
        tokenIssueService.rotate(first.refreshToken, app("device-1")).tokens

        tokenIssueService.issue(user, app("device-6"))

        val deviceIds = authSessionRepository.findByUserId(userId()).map { it.deviceId }
        assertThat(deviceIds).containsExactlyInAnyOrder("device-1", "device-3", "device-4", "device-5", "device-6")
        assertThat(events.stream(AuthSessionRevokedEvent::class.java).map { it.deviceId }).containsExactly("device-2")
    }

    @Test
    fun `같은 기기로 다시 로그인하면 기기 수가 늘지 않고 이전 토큰은 쓸 수 없다`() {
        val before = tokenIssueService.issue(user, app("android-1"))

        tokenIssueService.issue(user, app("android-1"))

        assertThat(authSessionRepository.findByUserId(userId())).hasSize(1)
        assertThatThrownBy { tokenIssueService.rotate(before.refreshToken, app("android-1")).tokens }
            .isInstanceOf(AuthorizationException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_INVALID)
        assertThat(events.stream(AuthSessionRevokedEvent::class.java)).isEmpty()
    }

    @Test
    fun `세션 도입 전 리프레시 토큰은 저장값과 같으면 새 세션으로 옮긴다`() {
        val legacy = legacyRefreshToken(userId())
        `when`(authSessionCache.findLegacyRefreshToken(userId())).thenReturn(legacy)

        val migrated = tokenIssueService.rotate(legacy, app("android-1")).tokens

        val session = authSessionRepository.findByUserId(userId()).single()
        assertThat(jwtTokenProvider.parse(migrated.refreshToken)!!.sessionId).isEqualTo(session.id)
        assertThat(session.deviceId).isEqualTo("android-1")
    }

    @Test
    fun `세션 도입 전 리프레시 토큰이 저장값과 다르면 거절한다`() {
        `when`(authSessionCache.findLegacyRefreshToken(anyLong())).thenReturn("other-token")

        assertThatThrownBy { tokenIssueService.rotate(legacyRefreshToken(userId()), app("android-1")) }
            .isInstanceOf(AuthorizationException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_INVALID)
        assertThat(authSessionRepository.findByUserId(userId())).isEmpty()
    }

    @Test
    fun `로그아웃은 지금 기기만 끊고 기기 목록에 현재 기기를 표시한다`() {
        val android = tokenIssueService.issue(user, app("android-1"))
        val web = tokenIssueService.issue(user, SessionDevice.web("browser-1", CHROME_MAC))
        val webSessionId = jwtTokenProvider.parse(web.accessToken)!!.sessionId

        val listed = authSessionService.list(userId(), webSessionId)
        assertThat(listed.count).isEqualTo(2)
        assertThat(listed.maxCount).isEqualTo(5)
        assertThat(listed.devices.single { it.current }.deviceName).isEqualTo("Chrome (macOS)")

        authSessionService.revokeCurrent(userId(), webSessionId)

        val remaining = authSessionService.list(userId(), null).devices
        assertThat(remaining.map { it.sessionId }).containsExactly(jwtTokenProvider.parse(android.accessToken)!!.sessionId)
        assertThat(remaining.single().deviceName).isEqualTo("SM-S918N")
    }

    @Test
    fun `다른 사용자의 기기는 해제할 수 없다`() {
        val issued = tokenIssueService.issue(user, app("android-1"))
        val sessionId = jwtTokenProvider.parse(issued.accessToken)!!.sessionId!!

        assertThatThrownBy { authSessionService.revoke(userId() + 1, sessionId) }
            .isInstanceOf(DeviceSessionNotFoundException::class.java)
        assertThat(authSessionRepository.findByUserId(userId())).hasSize(1)
    }

    @Test
    fun `회전 결과가 사라진 뒤 직전 토큰이 와도 세션을 지우지 않고 다시 회전한다`() {
        val issued = tokenIssueService.issue(user, app("android-1"))
        val lost = tokenIssueService.rotate(issued.refreshToken, app("android-1")).tokens
        rotations.clear()

        val retried = tokenIssueService.rotate(issued.refreshToken, app("android-1")).tokens

        assertThat(retried.refreshToken).isNotEqualTo(lost.refreshToken)
        assertThat(tokenIssueService.rotate(retried.refreshToken, app("android-1")).tokens.refreshToken)
            .isNotEqualTo(retried.refreshToken)
        assertThat(events.stream(AuthSessionRevokedEvent::class.java)).isEmpty()
    }

    @Test
    fun `세션 도입 전 토큰 이전을 다시 요청하면 같은 토큰 쌍을 돌려준다`() {
        val legacy = legacyRefreshToken(userId())
        `when`(authSessionCache.findLegacyRefreshToken(userId())).thenReturn(legacy)

        val first = tokenIssueService.rotate(legacy, app("android-1"))
        `when`(authSessionCache.findLegacyRefreshToken(userId())).thenReturn(null)
        val retried = tokenIssueService.rotate(legacy, app("android-1"))

        assertThat(retried).isEqualTo(first)
        assertThat(authSessionRepository.findByUserId(userId())).hasSize(1)
    }

    @Test
    fun `웹 재발급은 요청 쿠키가 아니라 세션의 기기 식별자를 돌려준다`() {
        val web = tokenIssueService.issue(user, SessionDevice.web("browser-1", CHROME_MAC))

        val rotated = tokenIssueService.rotate(web.refreshToken, SessionDevice.web(null, CHROME_MAC))

        assertThat(rotated.deviceId).isEqualTo("browser-1")
    }

    private fun userId(): Long = requireNotNull(user.id)

    private fun app(deviceId: String) = SessionDevice.app(deviceId, "SM-S918N", SessionPlatform.ANDROID)

    /** 세션 도입 전 형식: sid·typ·jti 없이 subject 와 만료만 있다. */
    private fun legacyRefreshToken(userId: Long): String {
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret))
        val now = Instant.now()
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(600)))
            .signWith(key)
            .compact()
    }

    private fun anyUuid(): UUID = org.mockito.ArgumentMatchers.any(UUID::class.java) ?: PLACEHOLDER_UUID

    private fun anyTokens(): TokenResponse =
        org.mockito.ArgumentMatchers.any(TokenResponse::class.java) ?: PLACEHOLDER_TOKENS

    private fun anyRotated(): RotatedTokens =
        org.mockito.ArgumentMatchers.any(RotatedTokens::class.java) ?: RotatedTokens(PLACEHOLDER_TOKENS, "")

    private fun anyDuration(): Duration = org.mockito.ArgumentMatchers.any(Duration::class.java) ?: Duration.ZERO

    companion object {
        private const val CHROME_MAC =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/141.0.0.0 Safari/537.36"
        private val PLACEHOLDER_UUID = UUID(0, 0)
        private val PLACEHOLDER_TOKENS = TokenResponse("Bearer", "", "", 0)
    }
}
