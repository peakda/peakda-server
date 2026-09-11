package com.peakda.server.common.security.oauth2

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.Mockito.eq
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import java.time.Duration

class RedisOAuth2AuthorizationRequestRepositoryTest {

    private val redis = mock(StringRedisTemplate::class.java)
    private val values = mock(ValueOperations::class.java) as ValueOperations<String, String>
    private val repository = RedisOAuth2AuthorizationRequestRepository(redis, ObjectMapper().findAndRegisterModules())
    private val store = mutableMapOf<String, String>()

    @BeforeEach
    fun setUp() {
        `when`(redis.opsForValue()).thenReturn(values)
        doAnswer { invocation ->
            store[invocation.getArgument(0)] = invocation.getArgument(1)
            null
        }.`when`(values).set(anyString(), anyString(), any(Duration::class.java))
        `when`(values.get(anyString())).thenAnswer { invocation -> store[invocation.getArgument(0)] }
        `when`(values.getAndDelete(anyString())).thenAnswer { invocation -> store.remove(invocation.getArgument(0)) }
    }

    @Test
    fun `authorization request round trips through redis and callback consumes it once`() {
        val start = MockHttpServletRequest().apply { isSecure = true }
        val saveResponse = MockHttpServletResponse()
        val original = authorizationRequest()

        repository.saveAuthorizationRequest(original, start, saveResponse)
        val key = store.keys.single()
        verify(values).set(eq(key), anyString(), eq(Duration.ofMinutes(10)))
        val cookie = requireNotNull(saveResponse.getHeader("Set-Cookie"))
        assertThat(cookie).startsWith("__Host-PEAKDA_OAUTH2_")
            .contains("HttpOnly", "SameSite=Lax", "Secure", "Max-Age=600")
            .doesNotContain("Domain=")

        val callback = MockHttpServletRequest().apply {
            isSecure = true
            addParameter("state", original.state)
        }
        callback.setCookies(jakarta.servlet.http.Cookie(cookie.substringBefore('='), cookie.substringAfter('=').substringBefore(';')))
        val removalResponse = MockHttpServletResponse()
        val loaded = repository.removeAuthorizationRequest(callback, removalResponse)
        assertThat(loaded).usingRecursiveComparison().isEqualTo(original)
        assertThat(removalResponse.getHeader("Set-Cookie"))
            .startsWith(cookie.substringBefore('=') + "=")
            .contains("Max-Age=0", "Secure", "Path=/")
        assertThat(repository.removeAuthorizationRequest(callback, MockHttpServletResponse())).isNull()
        verify(values, org.mockito.Mockito.times(2)).getAndDelete(eq(key))
    }

    @Test
    fun `missing or wrong browser cookie cannot retrieve request`() {
        val originalCallback = saveAndCallback(authorizationRequest())
        val callback = MockHttpServletRequest().apply { addParameter("state", "state") }
        assertThat(repository.removeAuthorizationRequest(callback, MockHttpServletResponse())).isNull()
        val cookieName = originalCallback.cookies!!.single().name
        callback.setCookies(jakarta.servlet.http.Cookie(cookieName, "a".repeat(43)))
        assertThat(repository.loadAuthorizationRequest(callback)).isNull()
        assertThat(repository.removeAuthorizationRequest(callback, MockHttpServletResponse())).isNull()
        // A foreign browser must not consume the legitimate browser's request.
        assertThat(repository.removeAuthorizationRequest(originalCallback, MockHttpServletResponse())).isNotNull
    }

    @Test
    fun `save null removes callback request and clears cookie`() {
        val response = MockHttpServletResponse()
        val request = saveAndCallback(authorizationRequest())
        repository.saveAuthorizationRequest(null, request, response)
        assertThat(response.getHeader("Set-Cookie")).contains("Max-Age=0", "HttpOnly", "SameSite=Lax")
        assertThat(store).isEmpty()
    }

    @Test
    fun `separate repository instances share the same redis request`() {
        val original = authorizationRequest("shared")
        val callback = saveAndCallback(original)
        val second = RedisOAuth2AuthorizationRequestRepository(redis, ObjectMapper().findAndRegisterModules())

        assertThat(second.removeAuthorizationRequest(callback, MockHttpServletResponse()))
            .usingRecursiveComparison().isEqualTo(original)
        assertThat(repository.loadAuthorizationRequest(callback)).isNull()
    }

    @Test
    fun `valid nonce for another state cannot be used to swap flows`() {
        val firstCallback = saveAndCallback(authorizationRequest("first"))
        val secondCallback = saveAndCallback(authorizationRequest("second"))
        val firstCookie = firstCallback.cookies!!.single()
        val secondCookie = secondCallback.cookies!!.single()
        firstCallback.setCookies(jakarta.servlet.http.Cookie(
            firstCookie.name,
            secondCookie.value,
        ))
        assertThat(repository.loadAuthorizationRequest(firstCallback)).isNull()
        assertThat(store).hasSize(2)
    }

    @Test
    fun `load does not consume request and missing record behaves as expired`() {
        val original = authorizationRequest("persistent")
        val callback = saveAndCallback(original)
        assertThat(repository.loadAuthorizationRequest(callback)).isNotNull
        assertThat(repository.loadAuthorizationRequest(callback)).isNotNull
        store.clear()
        assertThat(repository.loadAuthorizationRequest(callback)).isNull()
    }

    @Test
    fun `parallel tabs retain independent requests`() {
        val first = saveAndCallback(authorizationRequest("tab-one"))
        val second = saveAndCallback(authorizationRequest("tab-two"))
        val allCookies = first.cookies!! + second.cookies!!
        first.setCookies(*allCookies)
        second.setCookies(*allCookies)
        assertThat(repository.removeAuthorizationRequest(first, MockHttpServletResponse())?.state).isEqualTo("tab-one")
        assertThat(repository.removeAuthorizationRequest(second, MockHttpServletResponse())?.state).isEqualTo("tab-two")
    }

    @Test
    fun `app state and pkce values survive serialization`() {
        val original = authorizationRequest("state.app")
        val callback = saveAndCallback(original)
        val loaded = requireNotNull(repository.loadAuthorizationRequest(callback))
        assertThat(loaded.state).isEqualTo("state.app")
        assertThat(loaded.attributes["code_verifier"]).isEqualTo("verifier")
        assertThat(loaded.additionalParameters["code_challenge"]).isEqualTo("challenge")
        assertThat(loaded.attributes["registration_id"]).isEqualTo("google")
    }

    @Test
    fun `malformed and out of bounds state requests are rejected`() {
        val callback = saveAndCallback(authorizationRequest())
        for (state in listOf("", "x".repeat(513), "different")) {
            callback.setParameter("state", state)
            assertThat(repository.loadAuthorizationRequest(callback)).isNull()
        }
        assertThat(store).hasSize(1)
    }

    @Test
    fun `malformed stored json fails closed and is consumed`() {
        val callback = saveAndCallback(authorizationRequest())
        store[store.keys.single()] = "not-json"
        assertThat(repository.loadAuthorizationRequest(callback)).isNull()
        assertThat(repository.removeAuthorizationRequest(callback, MockHttpServletResponse())).isNull()
        assertThat(store).isEmpty()
    }

    @Test
    fun `stored state mismatch fails closed`() {
        val callback = saveAndCallback(authorizationRequest())
        val key = store.keys.single()
        store[key] = store.getValue(key).replace("\"state\":\"state\"", "\"state\":\"other\"")
        assertThat(repository.removeAuthorizationRequest(callback, MockHttpServletResponse())).isNull()
    }

    @Test
    fun `http local flow does not use host prefix or secure flag`() {
        val response = MockHttpServletResponse()
        repository.saveAuthorizationRequest(authorizationRequest(), MockHttpServletRequest(), response)
        assertThat(response.getHeader("Set-Cookie")).startsWith("PEAKDA_OAUTH2_")
            .doesNotContain("Secure", "Domain=")
    }

    private fun saveAndCallback(request: OAuth2AuthorizationRequest): MockHttpServletRequest {
        val response = MockHttpServletResponse()
        repository.saveAuthorizationRequest(request, MockHttpServletRequest(), response)
        val cookie = requireNotNull(response.getHeader("Set-Cookie"))
        return MockHttpServletRequest().apply {
            addParameter("state", request.state)
            setCookies(jakarta.servlet.http.Cookie(cookie.substringBefore('='), cookie.substringAfter('=').substringBefore(';')))
        }
    }

    private fun authorizationRequest(state: String = "state") = OAuth2AuthorizationRequest.authorizationCode()
        .authorizationUri("https://accounts.example/authorize")
        .clientId("client")
        .redirectUri("https://api.example/login/oauth2/code/google")
        .scopes(setOf("openid", "email"))
        .state(state)
        .additionalParameters(mapOf("prompt" to "none", "code_challenge" to "challenge"))
        .attributes(mapOf("registration_id" to "google", "nonce" to "n", "code_verifier" to "verifier"))
        .build()
}
