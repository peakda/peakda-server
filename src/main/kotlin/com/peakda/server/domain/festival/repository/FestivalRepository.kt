package com.peakda.server.domain.festival.repository

import com.peakda.server.domain.festival.entity.Festival
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

private const val FESTIVAL_UPSERT_SQL = """
    INSERT INTO festivals (
        name, venue, start_date, end_date, host_organization, organizing_institution,
        supporting_institution, phone_number, homepage_url, road_address, land_lot_address,
        latitude, longitude, reference_date, provider_institution_code, provider_institution_name,
        created_at, updated_at
    ) VALUES (
        :#{#command.name}, :#{#command.venue}, :#{#command.startDate}, :#{#command.endDate},
        :#{#command.hostOrganization}, :#{#command.organizingInstitution}, :#{#command.supportingInstitution},
        :#{#command.phoneNumber}, :#{#command.homepageUrl}, :#{#command.roadAddress},
        :#{#command.landLotAddress}, :#{#command.latitude}, :#{#command.longitude},
        :#{#command.referenceDate}, :#{#command.providerInstitutionCode},
        :#{#command.providerInstitutionName}, now(), now()
    )
    ON CONFLICT ON CONSTRAINT uk_festivals_name_venue_start DO UPDATE SET
        end_date = COALESCE(EXCLUDED.end_date, festivals.end_date),
        host_organization = COALESCE(EXCLUDED.host_organization, festivals.host_organization),
        organizing_institution = COALESCE(EXCLUDED.organizing_institution, festivals.organizing_institution),
        supporting_institution = COALESCE(EXCLUDED.supporting_institution, festivals.supporting_institution),
        phone_number = COALESCE(EXCLUDED.phone_number, festivals.phone_number),
        homepage_url = COALESCE(EXCLUDED.homepage_url, festivals.homepage_url),
        road_address = COALESCE(EXCLUDED.road_address, festivals.road_address),
        land_lot_address = COALESCE(EXCLUDED.land_lot_address, festivals.land_lot_address),
        latitude = COALESCE(EXCLUDED.latitude, festivals.latitude),
        longitude = COALESCE(EXCLUDED.longitude, festivals.longitude),
        reference_date = COALESCE(EXCLUDED.reference_date, festivals.reference_date),
        provider_institution_code = COALESCE(EXCLUDED.provider_institution_code, festivals.provider_institution_code),
        provider_institution_name = COALESCE(EXCLUDED.provider_institution_name, festivals.provider_institution_name),
        updated_at = now()
"""

interface FestivalRepository : JpaRepository<Festival, Long> {
    fun findByNameAndVenueAndStartDate(name: String, venue: String, startDate: String): Festival?

    /** 좌표가 있는 축제 (꽃 태깅 매칭 대상). */
    fun findByLatitudeIsNotNullAndLongitudeIsNotNull(): List<Festival>

    @Modifying
    @Query(value = FESTIVAL_UPSERT_SQL, nativeQuery = true)
    fun upsert(@Param("command") command: FestivalUpsertCommand): Int
}

data class FestivalUpsertCommand(
    val name: String,
    val venue: String,
    val startDate: String,
    val endDate: String?,
    val hostOrganization: String?,
    val organizingInstitution: String?,
    val supportingInstitution: String?,
    val phoneNumber: String?,
    val homepageUrl: String?,
    val roadAddress: String?,
    val landLotAddress: String?,
    val latitude: Double?,
    val longitude: Double?,
    val referenceDate: String?,
    val providerInstitutionCode: String?,
    val providerInstitutionName: String?,
)
