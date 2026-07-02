package com.peakda.server.domain.home.presentation.response

import com.peakda.server.domain.seasonal.entity.BloomCategory
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "홈 검색바 보조 카피 (시즌 추천어)")
data class HomeSuggestionResponse(
    @field:Schema(description = "추천할 절정 시즌 데이터가 있는지 여부", example = "true")
    val available: Boolean,

    @field:Schema(description = "검색바에 노출할 카피 (없으면 null)", example = "요즘 절정인 벚꽃, 남산에서 만나보세요")
    val message: String?,

    @field:Schema(description = "꽃 카테고리", example = "CHERRY")
    val category: BloomCategory?,

    @field:Schema(description = "카테고리 표시명", example = "벚꽃")
    val displayName: String?,

    @field:Schema(description = "명소 id", example = "501")
    val attractionId: Long?,

    @field:Schema(description = "명소명", example = "남산")
    val attractionTitle: String?,

    @field:Schema(description = "상태 산출 기준일", example = "2026-06-06")
    val baseDate: LocalDate?,
)
