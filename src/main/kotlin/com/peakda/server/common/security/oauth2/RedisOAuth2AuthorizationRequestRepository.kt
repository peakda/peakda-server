package com.peakda.server.common.security.oauth2

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.ResponseCookie
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.util.Base64

/** Stores OAuth authorization requests in shared Redis and binds them to the browser that started the flow. */
@Component
class RedisOAuth2AuthorizationRequestRepository(
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    override fun loadAuthorizationRequest(request: HttpServletRequest): OAuth2AuthorizationRequest? {
        val state = validState(request.getParameter(OAuth2ParameterNames.STATE)) ?: return null
        val nonce = validNonce(cookieValue(request, cookieName(state, request.isSecure))) ?: return null
        val stored = redis.opsForValue().get(redisKey(state, nonce)) ?: return null
        return deserialize(stored)?.takeIf { it.state == state }
    }

    override fun saveAuthorizationRequest(
        authorizationRequest: OAuth2AuthorizationRequest?,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response)
            return
        }
        val state = validState(authorizationRequest.state)
            ?: throw IllegalArgumentException("authorizationRequest.state cannot be empty or invalid")
        val nonce = randomNonce()
        redis.opsForValue().set(
            redisKey(state, nonce),
            objectMapper.writeValueAsString(StoredAuthorizationRequest.from(authorizationRequest)),
            TTL,
        )
        response.addHeader("Set-Cookie", browserCookie(cookieName(state, request.isSecure), nonce, request.isSecure(), TTL))
    }

    override fun removeAuthorizationRequest(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): OAuth2AuthorizationRequest? {
        val state = validState(request.getParameter(OAuth2ParameterNames.STATE)) ?: return null
        val name = cookieName(state, request.isSecure)
        val nonce = validNonce(cookieValue(request, name))
        val authorizationRequest = nonce?.let {
            redis.opsForValue().getAndDelete(redisKey(state, it))
                ?.let(::deserialize)
                ?.takeIf { stored -> stored.state == state }
        }
        response.addHeader("Set-Cookie", browserCookie(name, "", request.isSecure(), Duration.ZERO))
        return authorizationRequest
    }

    private fun deserialize(value: String): OAuth2AuthorizationRequest? = try {
        objectMapper.readValue(value, StoredAuthorizationRequest::class.java).toAuthorizationRequest()
    } catch (_: JsonProcessingException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun redisKey(state: String, nonce: String): String =
        "$REDIS_PREFIX${sha256(state)}:${sha256(nonce)}"

    private fun cookieValue(request: HttpServletRequest, name: String): String? =
        request.cookies?.firstOrNull { it.name == name }?.value

    private fun browserCookie(name: String, value: String, secure: Boolean, maxAge: Duration): String =
        ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(secure)
            .sameSite("Lax")
            .path("/")
            .maxAge(maxAge)
            .build()
            .toString()

    private fun validState(state: String?): String? =
        state?.takeIf { it.length in 1..MAX_STATE_LENGTH }

    private fun validNonce(nonce: String?): String? =
        nonce?.takeIf { it.length in MIN_NONCE_LENGTH..MAX_NONCE_LENGTH && NONCE_PATTERN.matches(it) }

    private fun randomNonce(): String = ByteArray(NONCE_BYTES).also(RANDOM::nextBytes).let {
        Base64.getUrlEncoder().withoutPadding().encodeToString(it)
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun cookieName(state: String, secure: Boolean): String =
        "${if (secure) HOST_COOKIE_PREFIX else COOKIE_PREFIX}${sha256(state).take(32)}"

    companion object {
        private const val REDIS_PREFIX = "oauth2:authorization-request:"
        private const val COOKIE_PREFIX = "PEAKDA_OAUTH2_"
        private const val HOST_COOKIE_PREFIX = "__Host-PEAKDA_OAUTH2_"
        private const val MAX_STATE_LENGTH = 512
        private const val MIN_NONCE_LENGTH = 32
        private const val MAX_NONCE_LENGTH = 128
        private const val NONCE_BYTES = 32
        private val NONCE_PATTERN = Regex("[A-Za-z0-9_-]+")
        private val RANDOM = SecureRandom()
        private val TTL = Duration.ofMinutes(10)
    }
}

private data class StoredAuthorizationRequest(
    val authorizationUri: String,
    val clientId: String,
    val redirectUri: String?,
    val scopes: Set<String>,
    val state: String,
    val additionalParameters: Map<String, Any?>,
    val attributes: Map<String, Any?>,
    val authorizationRequestUri: String,
) {
    fun toAuthorizationRequest(): OAuth2AuthorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
        .authorizationUri(authorizationUri)
        .clientId(clientId)
        .redirectUri(redirectUri)
        .scopes(scopes)
        .state(state)
        .additionalParameters(additionalParameters)
        .attributes(attributes)
        .authorizationRequestUri(authorizationRequestUri)
        .build()

    companion object {
        fun from(request: OAuth2AuthorizationRequest) = StoredAuthorizationRequest(
            request.authorizationUri,
            request.clientId,
            request.redirectUri,
            request.scopes,
            request.state,
            request.additionalParameters,
            request.attributes,
            request.authorizationRequestUri,
        )
    }
}
