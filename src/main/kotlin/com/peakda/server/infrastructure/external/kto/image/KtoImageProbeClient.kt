package com.peakda.server.infrastructure.external.kto.image

import com.peakda.server.infrastructure.external.common.ExternalApiException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.net.URI

/**
 * 관광공사 이미지 서버(tong.visitkorea.or.kr)에 이미지 파일이 실제로 있는지 확인한다.
 *
 * TourAPI 는 썸네일을 만들지 못한 콘텐츠에도 썸네일 URL(firstimage2)을 내려 주므로, 목록 카드에 쓰기 전에 확인이 필요하다.
 * 이 서버는 HEAD 에 405 를 주므로 첫 1바이트만 요청하는 GET 으로 상태 코드만 본다. API 가 아니라 serviceKey·호출 한도가 없다.
 */
@Component
class KtoImageProbeClient(
    @param:Qualifier("ktoImageRestClient") private val restClient: RestClient,
) {
    fun probe(url: String): KtoImageProbeResult {
        val uri = parseKtoImageUri(url) ?: return KtoImageProbeResult.UNSUPPORTED
        return try {
            restClient.get()
                .uri(uri)
                .header(HttpHeaders.RANGE, "bytes=0-0")
                .exchange { _, response -> resultOf(response.statusCode.value()) }
                ?: KtoImageProbeResult.UNKNOWN
        } catch (e: RestClientException) {
            log.debug("[ktoImageProbe] failed url={} error={}", url, e.message)
            KtoImageProbeResult.UNKNOWN
        } catch (e: ExternalApiException) {
            log.debug("[ktoImageProbe] failed url={} error={}", url, e.message)
            KtoImageProbeResult.UNKNOWN
        }
    }

    /** 관광공사 이미지 서버 주소만 연다. DB 값이 어떤 주소든 서버가 그대로 요청하지 않게 한다. */
    private fun parseKtoImageUri(url: String): URI? {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() !in ALLOWED_SCHEMES) return null
        if (!uri.host.equals(HOST, ignoreCase = true)) return null
        return uri
    }

    private fun resultOf(status: Int): KtoImageProbeResult = when (status) {
        in 200..299 -> KtoImageProbeResult.AVAILABLE
        404, 410 -> KtoImageProbeResult.MISSING
        else -> KtoImageProbeResult.UNKNOWN
    }

    companion object {
        const val HOST = "tong.visitkorea.or.kr"
        private val ALLOWED_SCHEMES = setOf("http", "https")
        private val log = LoggerFactory.getLogger(KtoImageProbeClient::class.java)
    }
}
