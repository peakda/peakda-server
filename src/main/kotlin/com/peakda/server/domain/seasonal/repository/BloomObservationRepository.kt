package com.peakda.server.domain.seasonal.repository

import com.peakda.server.domain.seasonal.entity.BloomObservation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

private const val BLOOM_OBSERVATION_UPSERT_SQL = """
    INSERT INTO bloom_observations (
        tree_type, obs_place, obs_year, obs_place_detail, flower_status,
        budding_on, flowering_on, full_bloom_on, source_modified_at, created_at, updated_at
    ) VALUES (
        :#{#command.treeType}, :#{#command.obsPlace}, :#{#command.obsYear},
        :#{#command.obsPlaceDetail}, :#{#command.flowerStatus}, :#{#command.buddingOn},
        :#{#command.floweringOn}, :#{#command.fullBloomOn}, :#{#command.sourceModifiedAt},
        now(), now()
    )
    ON CONFLICT ON CONSTRAINT uk_bloom_observations_tree_place_year DO UPDATE SET
        obs_place_detail = EXCLUDED.obs_place_detail,
        flower_status = EXCLUDED.flower_status,
        budding_on = EXCLUDED.budding_on,
        flowering_on = EXCLUDED.flowering_on,
        full_bloom_on = EXCLUDED.full_bloom_on,
        source_modified_at = EXCLUDED.source_modified_at,
        updated_at = now()
"""

interface BloomObservationRepository : JpaRepository<BloomObservation, Long> {
    @Modifying
    @Query(value = BLOOM_OBSERVATION_UPSERT_SQL, nativeQuery = true)
    fun upsert(@Param("command") command: BloomObservationUpsertCommand): Int
}

data class BloomObservationUpsertCommand(
    val treeType: String,
    val obsPlace: String,
    val obsYear: Int,
    val obsPlaceDetail: String?,
    val flowerStatus: String?,
    val buddingOn: LocalDate?,
    val floweringOn: LocalDate?,
    val fullBloomOn: LocalDate?,
    val sourceModifiedAt: String?,
)
