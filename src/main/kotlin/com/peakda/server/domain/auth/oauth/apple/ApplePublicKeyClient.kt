package com.peakda.server.domain.auth.oauth.apple

import io.jsonwebtoken.security.JwkSet
import io.jsonwebtoken.security.Jwks
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.security.Key
import java.util.concurrent.atomic.AtomicReference

/**
 * Apple 공개키(JWKS)를 조회하고 `kid` 로 서명 키를 해석한다.
 *
 * Apple 은 키를 주기적으로 회전하므로 결과를 캐시하되, 요청한 `kid` 가 캐시에 없으면
 * (= 키 회전 직후) 한 번 강제로 다시 조회한다.
 */
@Component
class ApplePublicKeyClient(
    restClientBuilder: RestClient.Builder,
) {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val restClient: RestClient = restClientBuilder.build()
    private val cachedKeys = AtomicReference<Map<String, Key>>(emptyMap())

    /**
     * 주어진 [kid] 에 해당하는 Apple 공개키를 반환한다. 캐시 미스 시 JWKS 를 강제 갱신해 한 번 더 시도한다.
     * 해당 키를 끝내 찾지 못하면 null 을 반환한다.
     */
    fun findPublicKey(kid: String): Key? {
        cachedKeys.get()[kid]?.let { return it }
        return refresh()[kid]
    }

    private fun refresh(): Map<String, Key> {
        val keys = fetchKeys()
        cachedKeys.set(keys)
        return keys
    }

    private fun fetchKeys(): Map<String, Key> {
        val body = restClient.get()
            .uri(AppleProperties.JWKS_URL)
            .retrieve()
            .body(String::class.java)
            ?: throw IllegalStateException("Apple JWKS 응답이 비어 있습니다.")

        val jwkSet: JwkSet = Jwks.setParser().build().parse(body)
        return jwkSet.getKeys().associate { jwk ->
            requireNotNull(jwk.getId()) { "Apple JWK 에 kid 가 없습니다." } to jwk.toKey()
        }.also { log.debug("Apple JWKS refreshed. kids={}", it.keys) }
    }
}
