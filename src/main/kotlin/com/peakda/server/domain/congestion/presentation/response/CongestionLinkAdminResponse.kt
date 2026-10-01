package com.peakda.server.domain.congestion.presentation.response

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.congestion.entity.CongestionAttractionLink
import com.peakda.server.domain.congestion.entity.CongestionLinkStatus
import com.peakda.server.domain.congestion.entity.CongestionMatchType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "관리자 혼잡도 연결 검토 응답")
data class CongestionLinkAdminResponse(
    @field:Schema(description = "연결 id", example = "42")
    val id: Long,
    @field:Schema(description = "집중률 지역 코드 (개편 전 법정동 시도)", example = "11")
    val areaCode: String,
    @field:Schema(description = "집중률 시군구 코드 (개편 전 법정동 시군구)", example = "11710")
    val sigunguCode: String,
    @field:Schema(description = "집중률 관광지명", example = "석촌호수")
    val touristAttractionName: String,
    @field:Schema(description = "연결 상태", example = "PENDING_REVIEW")
    val status: CongestionLinkStatus,
    @field:Schema(description = "연결 근거", example = "CONTAINS", nullable = true)
    val matchType: CongestionMatchType?,
    @field:Schema(description = "현재 연결(또는 제안)된 명소. 후보가 여럿이면 null", nullable = true)
    val attraction: AttractionSummary?,
    @field:Schema(description = "검토용 후보 명소 (최대 5건)")
    val candidates: List<AttractionSummary>,
    @field:Schema(description = "수정 시각", example = "2026-09-30T00:40:00Z")
    val updatedAt: Instant,
) {
    @Schema(description = "후보 명소 요약")
    data class AttractionSummary(
        @field:Schema(description = "명소 id", example = "501")
        val id: Long,
        @field:Schema(description = "명소명", example = "석촌호수")
        val title: String,
        @field:Schema(description = "주소", example = "서울특별시 송파구 잠실동", nullable = true)
        val address: String?,
    ) {
        companion object {
            fun from(attraction: Attraction) = AttractionSummary(
                id = requireNotNull(attraction.id),
                title = attraction.title,
                address = attraction.addressMain,
            )
        }
    }

    companion object {
        fun from(link: CongestionAttractionLink, attractions: Map<Long, Attraction>) = CongestionLinkAdminResponse(
            id = requireNotNull(link.id),
            areaCode = link.areaCode,
            sigunguCode = link.sigunguCode,
            touristAttractionName = link.touristAttractionName,
            status = link.status,
            matchType = link.matchType,
            attraction = link.attractionId?.let(attractions::get)?.let(AttractionSummary::from),
            candidates = link.candidateIds.mapNotNull(attractions::get).map(AttractionSummary::from),
            updatedAt = link.updatedAt,
        )
    }
}
