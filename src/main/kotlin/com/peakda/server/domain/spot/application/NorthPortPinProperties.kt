package com.peakda.server.domain.spot.application

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI

@ConfigurationProperties(prefix = "peakda.spot.north-port-pin")
data class NorthPortPinProperties(
    val enabled: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val linkUrl: String = "",
) {
    init {
        if (enabled) {
            require(latitude != null && latitude.isFinite() && latitude in -90.0..90.0) {
                "북항 핀을 활성화하려면 유효한 위도가 필요합니다."
            }
            require(longitude != null && longitude.isFinite() && longitude in -180.0..180.0) {
                "북항 핀을 활성화하려면 유효한 경도가 필요합니다."
            }
            val uri = URI(linkUrl)
            require(uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank() && uri.rawUserInfo == null) {
                "북항 핀을 활성화하려면 유효한 HTTP(S) 링크가 필요합니다."
            }
        }
    }
}
