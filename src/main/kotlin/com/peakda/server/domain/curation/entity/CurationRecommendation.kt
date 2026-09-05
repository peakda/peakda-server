package com.peakda.server.domain.curation.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 큐레이션 당일치기 추천 카드.
 *
 * 화면 스펙의 "추후 복수 스팟 확장"을 보존하기 위해 큐레이션과 1:N인 독립 행으로 저장한다.
 */
@Entity
@Table(
    name = "curation_recommendations",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_curation_recommendations_curation_sort",
            columnNames = ["curation_id", "sort_order"],
        ),
    ],
    indexes = [
        Index(name = "ix_curation_recommendations_curation_id", columnList = "curation_id"),
    ],
)
class CurationRecommendation(
    @Column(name = "curation_id", nullable = false)
    val curationId: Long,

    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int,

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    val title: String,

    @Column(name = "spot_id")
    val spotId: Long? = null,

    @Column(name = "place_name", nullable = false, columnDefinition = "TEXT")
    val placeName: String,

    @Column(name = "latitude")
    val latitude: Double? = null,

    @Column(name = "longitude")
    val longitude: Double? = null,

    @Column(name = "photo_url", columnDefinition = "TEXT")
    val photoUrl: String? = null,

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    val body: String,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
