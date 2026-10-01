package com.peakda.server.domain.congestion.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version

/**
 * 관광지 집중률의 관광지(자연키: 지역·시군구·관광지명)와 명소의 연결.
 *
 * 집중률 응답에는 관광지 코드가 없어 이름으로만 명소와 맞출 수 있다. 이름 매칭은 배치에서 한 번 하고,
 * 조회 경로는 이 테이블만 본다.
 */
@Entity
@Table(
    name = "congestion_attraction_links",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_congestion_attraction_links_key",
            columnNames = ["area_code", "sigungu_code", "tourist_attraction_name"],
        ),
    ],
    indexes = [
        Index(name = "idx_congestion_attraction_links_attraction", columnList = "attraction_id,status"),
        Index(name = "idx_congestion_attraction_links_status", columnList = "status,id"),
    ],
)
class CongestionAttractionLink(
    @Column(name = "area_code", nullable = false, columnDefinition = "TEXT")
    val areaCode: String,

    @Column(name = "sigungu_code", nullable = false, columnDefinition = "TEXT")
    val sigunguCode: String,

    @Column(name = "tourist_attraction_name", nullable = false, columnDefinition = "TEXT")
    val touristAttractionName: String,

    @Column(name = "attraction_id")
    var attractionId: Long? = null,

    /** 검토용 후보 명소 id 목록(콤마 구분). 후보가 없으면 null. */
    @Column(name = "candidate_attraction_ids", columnDefinition = "TEXT")
    var candidateAttractionIds: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", columnDefinition = "TEXT")
    var matchType: CongestionMatchType? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    var status: CongestionLinkStatus,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set

    /** 매칭 잡과 관리자 검토가 같은 행을 동시에 바꿀 때 나중 쓰기가 앞선 결정을 덮지 않도록 한다. */
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    val candidateIds: List<Long>
        get() = candidateAttractionIds.orEmpty().split(',').mapNotNull { it.trim().toLongOrNull() }

    fun apply(
        status: CongestionLinkStatus,
        matchType: CongestionMatchType?,
        attractionId: Long?,
        candidateIds: List<Long>,
    ) {
        this.status = status
        this.matchType = matchType
        this.attractionId = attractionId
        this.candidateAttractionIds = candidateIds.takeIf { it.isNotEmpty() }?.joinToString(",")
    }
}
