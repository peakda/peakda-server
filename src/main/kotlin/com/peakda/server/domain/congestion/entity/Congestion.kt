package com.peakda.server.domain.congestion.entity

import com.peakda.server.global.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "congestions",
    uniqueConstraints = [UniqueConstraint(name = "uk_congestions_ymd_tats", columnNames = ["base_ymd", "t_ats_cd"])],
)
class Congestion(
    @Column(name = "base_ymd", nullable = false, columnDefinition = "TEXT")
    val baseYmd: String,

    @Column(name = "t_ats_cd", nullable = false, columnDefinition = "TEXT")
    val tAtsCd: String,

    @Column(name = "t_ats_nm", columnDefinition = "TEXT")
    var tAtsNm: String? = null,

    @Column(name = "area_cd", columnDefinition = "TEXT")
    var areaCd: String? = null,

    @Column(name = "signgu_cd", columnDefinition = "TEXT")
    var signguCd: String? = null,

    @Column(name = "cnctr_rate", columnDefinition = "TEXT")
    var cnctrRate: String? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
