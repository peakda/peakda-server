package com.peakda.server.infrastructure.external.apple

import com.fasterxml.jackson.annotation.JsonProperty
import com.peakda.server.domain.auth.apple.application.AppleProperties
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClient

/**
 * 애플 인증 REST API. 실패하면 [org.springframework.web.client.RestClientException] 을 던진다.
 * https://developer.apple.com/documentation/sign_in_with_apple/sign_in_with_apple_rest_api
 */
@Component
class AppleAuthClient(
    builder: RestClient.Builder,
    properties: AppleProperties,
) {
    private val restClient = builder.clone()
        .baseUrl(BASE_URL)
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(properties.connectTimeout)
                setReadTimeout(properties.readTimeout)
            },
        )
        .build()

    /** 앱이 받은 authorization code(5분 안에 한 번만 쓸 수 있다)를 애플 토큰으로 바꾼다. */
    fun exchangeCode(clientId: String, clientSecret: String, code: String): AppleTokenResponse? =
        restClient.post()
            .uri("/auth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                form(
                    "client_id" to clientId,
                    "client_secret" to clientSecret,
                    "code" to code,
                    "grant_type" to "authorization_code",
                ),
            )
            .retrieve()
            .body(AppleTokenResponse::class.java)

    /** 애플 리프레시 토큰을 폐기한다. 사용자의 "Apple로 로그인" 연결도 함께 끊긴다. */
    fun revokeRefreshToken(clientId: String, clientSecret: String, refreshToken: String) {
        restClient.post()
            .uri("/auth/revoke")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                form(
                    "client_id" to clientId,
                    "client_secret" to clientSecret,
                    "token" to refreshToken,
                    "token_type_hint" to "refresh_token",
                ),
            )
            .retrieve()
            .toBodilessEntity()
    }

    private fun form(vararg pairs: Pair<String, String>): MultiValueMap<String, String> =
        LinkedMultiValueMap<String, String>().apply { pairs.forEach { (key, value) -> add(key, value) } }

    data class AppleTokenResponse(
        @JsonProperty("refresh_token")
        val refreshToken: String? = null,
    )

    companion object {
        private const val BASE_URL = "https://appleid.apple.com"
    }
}
