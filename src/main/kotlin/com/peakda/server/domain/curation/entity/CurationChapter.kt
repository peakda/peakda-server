package com.peakda.server.domain.curation.entity

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
 * 큐레이션의 장소 챕터.
 *
 * [spotId]가 있으면 개화 뱃지·거리·스팟 상세 링크를 스팟에서 채운다.
 * 없으면 저장된 [placeName]·좌표만 사용한다.
 */
@Entity
@Table(
    name = "curation_chapters",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_curation_chapters_curation_sort",
            columnNames = ["curation_id", "sort_order"],
        ),
    ],
    indexes = [
        Index(name = "ix_curation_chapters_curation_id", columnList = "curation_id"),
    ],
)
class CurationChapter(
    @Column(name = "curation_id", nullable = false)
    val curationId: Long,

    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "layout", nullable = false, columnDefinition = "TEXT")
    val layout: CurationLayout,

    @Column(name = "heading", nullable = false, columnDefinition = "TEXT")
    val heading: String,

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

    @Column(name = "pull_quote", columnDefinition = "TEXT")
    val pullQuote: String? = null,

    @Column(name = "lead_text", columnDefinition = "TEXT")
    val leadText: String? = null,

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    val body: String,

    /**
     * 운영기간·입장료·주의사항이 한 줄 자유 텍스트로 들어온다.
     * 화면 실물이 단일 텍스트이므로 분해하지 않는다.
     */
    @Column(name = "fact_note", columnDefinition = "TEXT")
    val factNote: String? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
