package com.peakda.server.domain.spot.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(
    name = "spot_records",
    indexes = [
        Index(name = "ix_spot_records_spot_id", columnList = "spot_id"),
        Index(name = "ix_spot_records_user_id_status", columnList = "user_id,status"),
        Index(name = "ix_spot_records_visited_date", columnList = "visited_date"),
    ],
)
class SpotRecord(
    @Column(name = "spot_id", nullable = false)
    var spotId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "visited_date")
    var visitedDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "bloom_stage", columnDefinition = "TEXT")
    var bloomStage: BloomStage? = null,

    @Column(name = "memo", columnDefinition = "TEXT")
    var memo: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    var status: SpotRecordStatus,

    @Column(name = "published_at")
    var publishedAt: Instant? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
