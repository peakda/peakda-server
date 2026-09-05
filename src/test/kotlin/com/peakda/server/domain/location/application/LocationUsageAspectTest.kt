package com.peakda.server.domain.location.application

import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.location.entity.LocationAccessChannel
import com.peakda.server.domain.location.entity.LocationServiceType
import com.peakda.server.domain.user.entity.User
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.`when`
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

class LocationUsageAspectTest {

    private val locationUsageRecorder = mock(LocationUsageRecorder::class.java)
    private val aspect = LocationUsageAspect(locationUsageRecorder)

    @BeforeEach
    fun setUp() {
        authenticate()
        bindRequest(userAgent = "Mozilla/5.0 (Linux; Android 14; SM-S911N) Chrome/120.0.0.0 Mobile")
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    fun `좌표 조건이 없는 엔드포인트는 호출 자체를 이용으로 기록한다`() {
        aspect.record(
            joinPoint(parameterNames = arrayOf("minLat"), args = arrayOf(37.4)),
            RecordLocationUsage(service = LocationServiceType.BLOOM_MAP),
        )

        assertThat(recordedCommands()).singleElement().satisfies({ command ->
            assertThat(command.userId).isEqualTo(USER_ID)
            assertThat(command.service).isEqualTo(LocationServiceType.BLOOM_MAP)
            assertThat(command.channel).isEqualTo(LocationAccessChannel.ANDROID)
        })
    }

    @Test
    fun `선택 좌표를 모두 보낸 요청만 기록한다`() {
        aspect.record(
            joinPoint(parameterNames = arrayOf("id", "lat", "lng"), args = arrayOf(101L, 37.5, 127.0)),
            curationDetailUsage(),
        )

        assertThat(recordedCommands()).singleElement().satisfies({ command ->
            assertThat(command.service).isEqualTo(LocationServiceType.CURATION_DETAIL)
        })
    }

    @Test
    fun `선택 좌표를 보내지 않은 요청은 기록하지 않는다`() {
        aspect.record(
            joinPoint(parameterNames = arrayOf("id", "lat", "lng"), args = arrayOf(101L, null, null)),
            curationDetailUsage(),
        )

        assertThat(recordedCommands()).isEmpty()
    }

    @Test
    fun `좌표를 일부만 보낸 요청은 기록하지 않는다`() {
        aspect.record(
            joinPoint(parameterNames = arrayOf("id", "lat", "lng"), args = arrayOf(101L, 37.5, null)),
            curationDetailUsage(),
        )

        assertThat(recordedCommands()).isEmpty()
    }

    @Test
    fun `인증 주체가 없으면 기록하지 않는다`() {
        SecurityContextHolder.clearContext()

        aspect.record(
            joinPoint(parameterNames = arrayOf("minLat"), args = arrayOf(37.4)),
            RecordLocationUsage(service = LocationServiceType.BLOOM_MAP),
        )

        assertThat(recordedCommands()).isEmpty()
    }

    @Test
    fun `User-Agent 가 없으면 취득경로를 UNKNOWN 으로 남긴다`() {
        bindRequest(userAgent = null)

        aspect.record(
            joinPoint(parameterNames = arrayOf("minLat"), args = arrayOf(37.4)),
            RecordLocationUsage(service = LocationServiceType.BLOOM_MAP),
        )

        assertThat(recordedCommands()).singleElement().satisfies({ command ->
            assertThat(command.channel).isEqualTo(LocationAccessChannel.UNKNOWN)
        })
    }

    @Test
    fun `iOS 앱 요청은 취득경로를 IOS 로 남긴다`() {
        bindRequest(userAgent = "PEAKDA/1.2.0 CFNetwork/1494.0.7 Darwin/23.4.0")

        aspect.record(
            joinPoint(parameterNames = arrayOf("minLat"), args = arrayOf(37.4)),
            RecordLocationUsage(service = LocationServiceType.BLOOM_MAP),
        )

        assertThat(recordedCommands()).singleElement().satisfies({ command ->
            assertThat(command.channel).isEqualTo(LocationAccessChannel.IOS)
        })
    }

    /**
     * Mockito 의 ArgumentCaptor·matcher 는 Kotlin non-null 파라미터에서 `capture(...) must not be null` 로 터진다.
     * 기록된 호출을 직접 읽어 검증한다.
     */
    private fun recordedCommands(): List<RecordLocationUsageCommand> =
        mockingDetails(locationUsageRecorder).invocations.map { it.getArgument(0) }

    private fun curationDetailUsage(): RecordLocationUsage = RecordLocationUsage(
        service = LocationServiceType.CURATION_DETAIL,
        coordinateParams = arrayOf("lat", "lng"),
    )

    private fun joinPoint(parameterNames: Array<String>, args: Array<Any?>): JoinPoint {
        val signature = mock(MethodSignature::class.java)
        `when`(signature.parameterNames).thenReturn(parameterNames)
        val joinPoint = mock(JoinPoint::class.java)
        `when`(joinPoint.signature).thenReturn(signature)
        `when`(joinPoint.args).thenReturn(args)
        return joinPoint
    }

    private fun authenticate() {
        val user = User(
            provider = OAuth2LoginType.KAKAO,
            providerId = "provider-$USER_ID",
            nickname = "피크다",
            email = "ex1@xxx.com",
        ).also { ReflectionTestUtils.setField(it, "id", USER_ID) }
        val principal = PrincipalDetails(user)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    private fun bindRequest(userAgent: String?) {
        val request = MockHttpServletRequest()
        userAgent?.let { request.addHeader(HttpHeaders.USER_AGENT, it) }
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    }

    companion object {
        private const val USER_ID = 7L
    }
}
