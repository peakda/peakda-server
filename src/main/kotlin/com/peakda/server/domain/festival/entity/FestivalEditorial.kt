package com.peakda.server.domain.festival.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

/**
 * 원천 축제 데이터에 없는 운영·에디토리얼 정보를 사람이 채우는 축제 상세.
 *
 * `festivals`는 공공데이터 동기화가 값을 덮어쓰는 영역이다.
 * 사람 입력의 소유권이 섞이지 않도록 별도 테이블로 분리한다.
 */
@Entity
@Table(
    name = "festival_editorials",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_festival_editorials_festival_id",
            columnNames = ["festival_id"],
        ),
    ],
)
class FestivalEditorial(
    @Column(name = "festival_id", nullable = false)
    val festivalId: Long,

    @Column(name = "hook", columnDefinition = "TEXT")
    var hook: String? = null,

    @Column(name = "period_note", columnDefinition = "TEXT")
    var periodNote: String? = null,

    @Column(name = "place_note", columnDefinition = "TEXT")
    var placeNote: String? = null,

    @Column(name = "admission_fee", columnDefinition = "TEXT")
    var admissionFee: String? = null,

    @Column(name = "admission_fee_note", columnDefinition = "TEXT")
    var admissionFeeNote: String? = null,

    @Column(name = "operating_hours", columnDefinition = "TEXT")
    var operatingHours: String? = null,

    @Column(name = "operating_hours_note", columnDefinition = "TEXT")
    var operatingHoursNote: String? = null,

    @Column(name = "caution", columnDefinition = "TEXT")
    var caution: String? = null,

    @Column(name = "caution_note", columnDefinition = "TEXT")
    var cautionNote: String? = null,

    @Column(name = "directions_transit", columnDefinition = "TEXT")
    var directionsTransit: String? = null,

    @Column(name = "directions_car", columnDefinition = "TEXT")
    var directionsCar: String? = null,

    @Column(name = "hero_image_url", columnDefinition = "TEXT")
    var heroImageUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    var status: FestivalEditorialStatus,

    @Column(name = "published_at")
    var publishedAt: Instant? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
