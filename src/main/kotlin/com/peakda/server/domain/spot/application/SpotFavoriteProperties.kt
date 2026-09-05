package com.peakda.server.domain.spot.application

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "peakda.spot.favorite")
data class SpotFavoriteProperties(
    /** 만개 시작 며칠 전까지를 배너·카드의 "임박"으로 볼지. (오늘 기준 1..leadDays 일 이내) */
    val bannerLeadDays: Long = 7,

    /** 카드에 노출할 사진 최대 장수. */
    val photoLimit: Int = 4,
)
