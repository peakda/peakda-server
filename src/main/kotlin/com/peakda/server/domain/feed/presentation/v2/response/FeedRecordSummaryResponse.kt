package com.peakda.server.domain.feed.presentation.v2.response

import com.peakda.server.domain.spot.entity.BloomStage
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate

@Schema(description = "v2 피드 목록 기록 요약")
data class FeedRecordSummaryResponse(
    @field:Schema(description = "기록 또는 항목 ID", example = "1024")
    val id: Long,
    @field:Schema(description = "스팟 ID", example = "11")
    val spotId: Long,
    @field:Schema(description = "스팟 이름", example = "남산")
    val spotName: String,
    @field:Schema(description = "작성자 요약")
    val user: FeedRecordResponse.UserSummary,
    @field:Schema(description = "방문 날짜", example = "2026-04-12")
    val visitedDate: LocalDate?,
    @field:Schema(description = "개화·단풍 상태", example = "PEAK")
    val bloomStage: BloomStage?,
    @field:Schema(description = "기록 메모", example = "벚꽃이 만개했어요.")
    val memo: String?,
    @field:Schema(description = "관련 콘텐츠 링크 (없으면 null)")
    val contentLink: FeedRecordResponse.ContentLink? = null,
    @field:Schema(description = "기록된 식물")
    val plants: List<FeedRecordResponse.PlantSummary>,
    @field:Schema(description = "대표(첫 번째) 사진 — 없으면 null")
    val coverPhoto: FeedRecordResponse.PhotoEntry?,
    @field:Schema(
        description = "첨부 사진 전체 목록 (sortOrder 오름차순, 사진이 없으면 빈 배열)",
        example = "[{\"objectKey\":\"spot-records/7/2026/09/9b1deb4d/main.jpg\"," +
            "\"url\":\"https://cdn.peakda.com/spot-records/7/2026/09/9b1deb4d/main.jpg\",\"sortOrder\":1," +
            "\"variants\":{\"thumbnail\":\"https://cdn.peakda.com/spot-records/7/2026/09/9b1deb4d/thumbnail.jpg\"," +
            "\"medium\":\"https://cdn.peakda.com/spot-records/7/2026/09/9b1deb4d/medium.jpg\"," +
            "\"main\":\"https://cdn.peakda.com/spot-records/7/2026/09/9b1deb4d/main.jpg\"}}]",
    )
    val photos: List<FeedRecordResponse.PhotoEntry> = emptyList(),
    @field:Schema(description = "기록 상태", example = "PUBLISHED")
    val status: SpotRecordStatus,
    @field:Schema(description = "게시 시각", example = "2026-04-12T09:00:00Z")
    val publishedAt: Instant?,
    @field:Schema(description = "생성 시각", example = "2026-04-12T08:00:00Z")
    val createdAt: Instant,
    @field:Schema(description = "수정 시각", example = "2026-04-12T08:30:00Z")
    val updatedAt: Instant,

    @field:Schema(description = "리액션 요약")
    val reactions: ReactionOverviewResponse,
)
