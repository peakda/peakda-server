package com.peakda.server.domain.seasonal.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

/** 기상청 군락단지 계절관측이며 현재 시즌만 제공되므로 매년 수집해 축적한다. */
@Entity
@Table(
    name = "bloom_observations",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_bloom_observations_tree_place_year",
            columnNames = ["tree_type", "obs_place", "obs_year"],
        ),
    ],
)
class BloomObservation(
    @Column(name = "tree_type", nullable = false, columnDefinition = "TEXT")
    val treeType: String,

    @Column(name = "obs_place", nullable = false, columnDefinition = "TEXT")
    val obsPlace: String,

    @Column(name = "obs_year", nullable = false)
    val obsYear: Int,

    @Column(name = "obs_place_detail", columnDefinition = "TEXT")
    var obsPlaceDetail: String? = null,

    @Column(name = "flower_status", columnDefinition = "TEXT")
    var flowerStatus: String? = null,

    @Column(name = "budding_on")
    var buddingOn: LocalDate? = null,

    @Column(name = "flowering_on")
    var floweringOn: LocalDate? = null,

    @Column(name = "full_bloom_on")
    var fullBloomOn: LocalDate? = null,

    @Column(name = "source_modified_at", columnDefinition = "TEXT")
    var sourceModifiedAt: String? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
