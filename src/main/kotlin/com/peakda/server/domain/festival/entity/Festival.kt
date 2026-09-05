package com.peakda.server.domain.festival.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

/**
 * 공공데이터 원천 축제.
 *
 * 원천 문자열 [startDate]·[endDate]는 포맷이 혼재하므로 보존만 한다.
 * 날짜 질의·판정은 동기화 시점에 파싱해 채운 [startsOn]·[endsOn]을 사용한다.
 * 파싱할 수 없는 값은 null이다.
 */
@Entity
@Table(
    name = "festivals",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_festivals_name_venue_start",
            columnNames = ["name", "venue", "start_date"],
        ),
    ],
)
class Festival(
    @Column(name = "name", nullable = false, columnDefinition = "TEXT")
    val name: String,

    @Column(name = "venue", nullable = false, columnDefinition = "TEXT")
    val venue: String,

    @Column(name = "start_date", nullable = false, columnDefinition = "TEXT")
    val startDate: String,

    @Column(name = "end_date", columnDefinition = "TEXT")
    var endDate: String? = null,

    @Column(name = "starts_on")
    var startsOn: LocalDate? = null,

    @Column(name = "ends_on")
    var endsOn: LocalDate? = null,

    @Column(name = "host_organization", columnDefinition = "TEXT")
    var hostOrganization: String? = null,

    @Column(name = "organizing_institution", columnDefinition = "TEXT")
    var organizingInstitution: String? = null,

    @Column(name = "supporting_institution", columnDefinition = "TEXT")
    var supportingInstitution: String? = null,

    @Column(name = "phone_number", columnDefinition = "TEXT")
    var phoneNumber: String? = null,

    @Column(name = "homepage_url", columnDefinition = "TEXT")
    var homepageUrl: String? = null,

    @Column(name = "road_address", columnDefinition = "TEXT")
    var roadAddress: String? = null,

    @Column(name = "land_lot_address", columnDefinition = "TEXT")
    var landLotAddress: String? = null,

    @Column(name = "latitude")
    var latitude: Double? = null,

    @Column(name = "longitude")
    var longitude: Double? = null,

    @Column(name = "reference_date", columnDefinition = "TEXT")
    var referenceDate: String? = null,

    @Column(name = "provider_institution_code", columnDefinition = "TEXT")
    var providerInstitutionCode: String? = null,

    @Column(name = "provider_institution_name", columnDefinition = "TEXT")
    var providerInstitutionName: String? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
