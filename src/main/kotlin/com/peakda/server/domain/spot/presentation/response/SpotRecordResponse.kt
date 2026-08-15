package com.peakda.server.domain.spot.presentation.response

import com.peakda.server.domain.feed.presentation.response.ReactionSummary
import com.peakda.server.domain.spot.entity.BloomStage
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate

@Schema(description = "스팟 기록 상세 응답")
data class SpotRecordResponse(
    @field:Schema(description = "기록 PK", example = "201")
    val id: Long,

    @field:Schema(description = "스팟 요약")
    val spot: SpotSummary,

    @field:Schema(description = "작성자 요약")
    val user: UserSummary,

    @field:Schema(description = "방문 일자", example = "2026-05-22")
    val visitedDate: LocalDate?,

    @field:Schema(description = "개화/단풍 상태", example = "PEAK")
    val bloomStage: BloomStage?,

    @field:Schema(description = "메모", example = "벚꽃이 정말 만개했어요.")
    val memo: String?,

    @field:Schema(description = "기록된 식물 목록")
    val plants: List<PlantSummary>,

    @field:Schema(description = "첨부 사진 목록 (sortOrder 오름차순)")
    val photos: List<PhotoEntry>,

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
) {
    @Schema(description = "스팟 요약")
    data class SpotSummary(
        val id: Long,
        val type: SpotType,
        val name: String,
        val address: String?,
        val attractionId: Long?,
    )

    @Schema(description = "작성자 요약")
    data class UserSummary(
        val id: Long,
        val nickname: String,
        @field:Schema(description = "프로필 이미지 URL (key 인 경우 presigned URL 로 변환된 값)")
        val profileImageUrl: String?,
    )

    @Schema(description = "식물 요약")
    data class PlantSummary(
        val id: Long,
        val name: String,
    )

    @Schema(description = "사진 항목 — url 은 응답 시점 발급 presigned URL")
    data class PhotoEntry(
        val objectKey: String,
        val url: String,
        val sortOrder: Int,
    )
}
