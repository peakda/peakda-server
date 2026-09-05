package com.peakda.server.domain.spot.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import com.peakda.server.domain.seasonal.entity.BloomCategory
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "plants",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_plants_name", columnNames = ["name"]),
    ],
)
class Plant(
    @Column(name = "name", nullable = false, columnDefinition = "TEXT")
    var name: String,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    var status: PlantStatus,

    @Column(name = "suggested_by_user_id")
    var suggestedByUserId: Long? = null,

    @Column(name = "approved_at")
    var approvedAt: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "bloom_category", columnDefinition = "TEXT")
    var bloomCategory: BloomCategory? = null,

    @ElementCollection(targetClass = Season::class, fetch = FetchType.LAZY)
    @CollectionTable(
        name = "plant_seasons",
        joinColumns = [JoinColumn(name = "plant_id", nullable = false)],
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "season", nullable = false, columnDefinition = "TEXT")
    var seasons: MutableSet<Season> = mutableSetOf(),
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
