package com.peakda.server.domain.festival.repository

import com.peakda.server.domain.festival.entity.Festival
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

private const val FESTIVAL_UPSERT_SQL = """
    INSERT INTO festivals (
        name, venue, start_date, end_date, starts_on, ends_on, host_organization, organizing_institution,
        supporting_institution, phone_number, homepage_url, road_address, land_lot_address,
        latitude, longitude, reference_date, provider_institution_code, provider_institution_name,
        created_at, updated_at
    ) VALUES (
        :#{#command.name}, :#{#command.venue}, :#{#command.startDate}, :#{#command.endDate},
        :#{#command.startsOn}, :#{#command.endsOn},
        :#{#command.hostOrganization}, :#{#command.organizingInstitution}, :#{#command.supportingInstitution},
        :#{#command.phoneNumber}, :#{#command.homepageUrl}, :#{#command.roadAddress},
        :#{#command.landLotAddress}, :#{#command.latitude}, :#{#command.longitude},
        :#{#command.referenceDate}, :#{#command.providerInstitutionCode},
        :#{#command.providerInstitutionName}, now(), now()
    )
    ON CONFLICT ON CONSTRAINT uk_festivals_name_venue_start DO UPDATE SET
        end_date = COALESCE(EXCLUDED.end_date, festivals.end_date),
        starts_on = COALESCE(EXCLUDED.starts_on, festivals.starts_on),
        ends_on = COALESCE(EXCLUDED.ends_on, festivals.ends_on),
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

    fun findAllByOrderByIdDesc(pageable: Pageable): Page<Festival>

    fun findByNameContainingIgnoreCaseOrderByIdDesc(name: String, pageable: Pageable): Page<Festival>

    /** 좌표가 있는 축제 (꽃 태깅 매칭 대상). */
    fun findByLatitudeIsNotNullAndLongitudeIsNotNull(): List<Festival>

    /**
     * [today] 에 진행 중인 축제를 종료가 임박한 순으로. 종료일이 없으면 시작일을 종료일로 본다.
     * "꽃축제인지" 판정은 호출측(application)이 축제명 매칭으로 수행한다.
     * 정규화가 불가했던 행(`starts_on IS NULL`)은 비교에서 자연히 제외된다.
     */
    @Query(
        """
            SELECT f FROM Festival f
            WHERE f.startsOn <= :today
              AND COALESCE(f.endsOn, f.startsOn) >= :today
            ORDER BY COALESCE(f.endsOn, f.startsOn) ASC, f.id ASC
        """,
    )
    fun findOngoing(@Param("today") today: LocalDate, pageable: Pageable): List<Festival>

    @Modifying
    @Query(value = FESTIVAL_UPSERT_SQL, nativeQuery = true)
    fun upsert(@Param("command") command: FestivalUpsertCommand): Int
}

data class FestivalUpsertCommand(
    val name: String,
    val venue: String,
    val startDate: String,
    val endDate: String?,
    val startsOn: LocalDate?,
    val endsOn: LocalDate?,
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
