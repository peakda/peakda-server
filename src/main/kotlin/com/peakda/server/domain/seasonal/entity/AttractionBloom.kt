package com.peakda.server.domain.seasonal.entity

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

/**
 * 명소 ↔ 꽃·계절 카테고리 매핑 (M:N). 자동 태깅의 출처별로 행이 분리된다.
 *
 * UK `(attraction_id, bloom_category, source)` — 한 명소가 같은 카테고리에 대해 KEYWORD·FESTIVAL 등
 * 서로 다른 출처로 여러 태그를 가질 수 있다. 조회 시 출처 우선순위/합산은 application 단 정책.
 */
@Entity
@Table(
    name = "attraction_blooms",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_attraction_blooms_attraction_category_source",
            columnNames = ["attraction_id", "bloom_category", "source"],
        ),
    ],
    indexes = [
        Index(name = "ix_attraction_blooms_category", columnList = "bloom_category"),
        Index(name = "ix_attraction_blooms_attraction", columnList = "attraction_id"),
    ],
)
class AttractionBloom(
    @Column(name = "attraction_id", nullable = false)
    val attractionId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "bloom_category", nullable = false, columnDefinition = "TEXT")
    val bloomCategory: BloomCategory,

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, columnDefinition = "TEXT")
    val source: TagSource,

    @Column(name = "confidence", nullable = false)
    var confidence: Double,

    @Column(name = "evidence", columnDefinition = "TEXT")
    var evidence: String? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
