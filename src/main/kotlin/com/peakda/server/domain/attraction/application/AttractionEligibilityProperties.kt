package com.peakda.server.domain.attraction.application

import org.springframework.boot.context.properties.ConfigurationProperties

/** 서비스(Spot 노출·꽃 태깅) 대상 TourAPI contentTypeId. 수집 범위와는 별개다. */
@ConfigurationProperties(prefix = "peakda.attraction")
data class AttractionEligibilityProperties(
    val eligibleContentTypes: Set<String> = setOf("12"),
)
