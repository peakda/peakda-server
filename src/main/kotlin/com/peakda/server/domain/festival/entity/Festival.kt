package com.peakda.server.domain.festival.entity

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
    name = "festivals",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_festivals_name_opar_start",
            columnNames = ["fstvl_nm", "opar", "fstvl_start_date"],
        ),
    ],
)
class Festival(
    @Column(name = "fstvl_nm", nullable = false, columnDefinition = "TEXT")
    val fstvlNm: String,

    @Column(name = "opar", nullable = false, columnDefinition = "TEXT")
    val opar: String,

    @Column(name = "fstvl_start_date", nullable = false, columnDefinition = "TEXT")
    val fstvlStartDate: String,

    @Column(name = "fstvl_end_date", columnDefinition = "TEXT")
    var fstvlEndDate: String? = null,

    @Column(name = "mnnst_nm", columnDefinition = "TEXT")
    var mnnstNm: String? = null,

    @Column(name = "auspc_instt_nm", columnDefinition = "TEXT")
    var auspcInsttNm: String? = null,

    @Column(name = "suprt_instt_nm", columnDefinition = "TEXT")
    var suprtInsttNm: String? = null,

    @Column(name = "phone_number", columnDefinition = "TEXT")
    var phoneNumber: String? = null,

    @Column(name = "homepage_url", columnDefinition = "TEXT")
    var homepageUrl: String? = null,

    @Column(name = "rdnmadr", columnDefinition = "TEXT")
    var rdnmadr: String? = null,

    @Column(name = "lnmadr", columnDefinition = "TEXT")
    var lnmadr: String? = null,

    @Column(name = "latitude")
    var latitude: Double? = null,

    @Column(name = "longitude")
    var longitude: Double? = null,

    @Column(name = "reference_date", columnDefinition = "TEXT")
    var referenceDate: String? = null,

    @Column(name = "instt_code", columnDefinition = "TEXT")
    var insttCode: String? = null,

    @Column(name = "instt_nm", columnDefinition = "TEXT")
    var insttNm: String? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
