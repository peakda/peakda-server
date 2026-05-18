package com.peakda.server.domain.festival.entity

import com.peakda.server.common.persistence.BaseTimeEntity
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
