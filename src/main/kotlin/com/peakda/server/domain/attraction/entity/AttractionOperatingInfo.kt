package com.peakda.server.domain.attraction.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 스팟 상세(SCR-025a)의 운영 정보. 한국관광공사 소개정보·반복정보 원문을 줄바꿈만 정리해 담는다.
 *
 * 관광공사가 비워 둔 항목은 null 이다. 모든 항목이 비어도 행을 남겨 같은 명소를 매일 다시 받지 않게 한다.
 */
@Entity
@Table(
    name = "attraction_operating_infos",
    uniqueConstraints = [UniqueConstraint(name = "uk_attraction_operating_infos_attraction", columnNames = ["attraction_id"])],
)
class AttractionOperatingInfo(
    @Column(name = "attraction_id", nullable = false)
    val attractionId: Long,

    @Column(name = "operating_hours", columnDefinition = "TEXT")
    var operatingHours: String? = null,

    @Column(name = "closed_days", columnDefinition = "TEXT")
    var closedDays: String? = null,

    @Column(name = "admission_fee", columnDefinition = "TEXT")
    var admissionFee: String? = null,

    @Column(name = "parking", columnDefinition = "TEXT")
    var parking: String? = null,

    /** 받을 때의 명소 `external_modified_at`. 관광공사가 명소를 수정하면 값이 달라져 다시 받는다. */
    @Column(name = "source_modified_at", columnDefinition = "TEXT")
    var sourceModifiedAt: String? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
