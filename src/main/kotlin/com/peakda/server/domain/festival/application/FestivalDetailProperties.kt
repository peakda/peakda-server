package com.peakda.server.domain.festival.application

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "peakda.festival")
data class FestivalDetailProperties(
    /** 종료 며칠 전부터 "종료 D-N" 뱃지를 붙일지. */
    val endingSoonDays: Long = 7,
)
