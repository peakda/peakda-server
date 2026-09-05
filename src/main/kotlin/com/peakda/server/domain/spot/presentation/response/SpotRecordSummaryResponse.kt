package com.peakda.server.domain.spot.presentation.response

import com.peakda.server.domain.feed.presentation.response.ReactionSummary
import com.peakda.server.domain.spot.entity.BloomStage
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate

@Schema(description = "스팟 기록 목록용 요약 — 대표 사진 1장만 포함")
data class SpotRecordSummaryResponse(
    val id: Long,
    val spotId: Long,
    val spotName: String,
    val user: SpotRecordResponse.UserSummary,
    val visitedDate: LocalDate?,
    val bloomStage: BloomStage?,
    val memo: String?,
    val plants: List<SpotRecordResponse.PlantSummary>,
    @field:Schema(description = "대표(첫 번째) 사진 — 없으면 null")
    val coverPhoto: SpotRecordResponse.PhotoEntry?,
    val status: SpotRecordStatus,
    val publishedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,

    @field:Schema(description = "리액션 요약")
    val reactions: ReactionSummary = ReactionSummary(emptyList(), emptySet()),
)
