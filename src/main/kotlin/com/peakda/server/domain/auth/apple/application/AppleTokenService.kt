package com.peakda.server.domain.auth.apple.application

import com.peakda.server.domain.auth.apple.repository.AppleTokenRepository
import com.peakda.server.infrastructure.external.apple.AppleAuthClient
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException

/**
 * 애플 리프레시 토큰을 받아 두었다가 회원 탈퇴 때 폐기한다.
 * 애플은 Sign in with Apple 을 쓰는 앱이 계정 삭제 시 토큰 폐기 API 를 부르도록 요구한다.
 *
 * 둘 다 애플 API 장애로 로그인·탈퇴를 막지 않는다. 신원 확인은 identity token 검증으로 이미 끝났고,
 * 탈퇴는 사용자의 권리라 외부 장애로 미룰 수 없다. 실패는 로그로 남긴다.
 */
@Service
class AppleTokenService(
    private val clientSecretGenerator: AppleClientSecretGenerator?,
    private val properties: AppleProperties,
    private val appleAuthClient: AppleAuthClient,
    private val appleTokenRepository: AppleTokenRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 로그인 때 받은 authorization code 를 교환해 애플 리프레시 토큰을 저장한다. */
    fun store(appleUserId: String, authorizationCode: String) {
        val generator = clientSecretGenerator ?: return
        val refreshToken = try {
            appleAuthClient.exchangeCode(properties.bundleId, generator.generate(), authorizationCode)?.refreshToken
        } catch (e: RestClientException) {
            log.warn("Apple authorization code exchange failed: {}", describe(e))
            return
        }
        if (refreshToken.isNullOrBlank()) {
            log.warn("Apple authorization code exchange returned no refresh token")
            return
        }
        appleTokenRepository.upsert(appleUserId, refreshToken)
    }

    /** 탈퇴한 애플 회원의 토큰을 폐기한다. 실패하면 행을 남겨 다시 시도할 수 있게 한다. */
    fun revoke(userId: Long, appleUserId: String) {
        val token = appleTokenRepository.findByIdOrNull(appleUserId)
        if (token == null) {
            log.warn("No Apple refresh token to revoke. userId={}", userId)
            return
        }
        val generator = clientSecretGenerator
        if (generator == null) {
            log.error("Apple login is disabled; cannot revoke Apple token. userId={}", userId)
            return
        }
        try {
            appleAuthClient.revokeRefreshToken(properties.bundleId, generator.generate(), token.refreshToken)
        } catch (e: RestClientException) {
            log.error("Apple token revoke failed; keeping the token for retry. userId={}, {}", userId, describe(e))
            return
        }
        appleTokenRepository.deleteById(appleUserId)
        log.info("Apple token revoked. userId={}", userId)
    }

    private fun describe(e: RestClientException): String = when (e) {
        is RestClientResponseException -> "status=${e.statusCode.value()} body=${e.responseBodyAsString}"
        else -> "${e.javaClass.simpleName}: ${e.message}"
    }
}
