package com.peakda.server.domain.feed.presentation.response

import com.peakda.server.domain.spot.presentation.response.SpotRecordResponse
import com.peakda.server.domain.spot.entity.BloomStage
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate

@Schema(description = "v1 피드 기록 상세 응답")
data class LegacyFeedRecordResponse(
    @field:Schema(description = "기록 PK", example = "201")
    val id: Long,

    @field:Schema(description = "스팟 요약")
    val spot: SpotRecordResponse.SpotSummary,

    @field:Schema(description = "작성자 요약")
    val user: SpotRecordResponse.UserSummary,

    @field:Schema(description = "방문 일자", example = "2026-05-22")
    val visitedDate: LocalDate?,

    @field:Schema(description = "개화/단풍 상태", example = "PEAK")
    val bloomStage: BloomStage?,

    @field:Schema(description = "메모", example = "벚꽃이 정말 만개했어요.")
    val memo: String?,

    @field:Schema(description = "관련 콘텐츠 링크 (없으면 null)")
    val contentLink: SpotRecordResponse.ContentLink? = null,

    @field:Schema(description = "기록된 식물 목록")
    val plants: List<SpotRecordResponse.PlantSummary>,

    @field:Schema(description = "첨부 사진 목록 (sortOrder 오름차순)")
    val photos: List<SpotRecordResponse.PhotoEntry>,

    @field:Schema(description = "기록 상태", example = "PUBLISHED")
    val status: SpotRecordStatus,

    @field:Schema(description = "게시 시각 (DRAFT 이면 null)")
    val publishedAt: Instant?,

    @field:Schema(description = "최초 생성 시각")
    val createdAt: Instant,

    @field:Schema(description = "최종 수정 시각")
    val updatedAt: Instant,

    @field:Schema(description = "리액션 요약")
    val reactions: ReactionSummary = ReactionSummary(emptyList(), emptySet()),
)
