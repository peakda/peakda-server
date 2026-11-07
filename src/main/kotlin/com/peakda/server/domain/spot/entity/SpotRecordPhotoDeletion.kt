package com.peakda.server.domain.spot.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** 소유 증거의 스냅샷. 사용자/기록 삭제와 무관하게 남으며 업로드 orphan은 대상으로 삼지 않는다. */
@Entity
@Table(name = "spot_record_photo_deletions")
class SpotRecordPhotoDeletion(
    @Column(columnDefinition = "TEXT", nullable = false)
    val objectKey: String,
    @Column(nullable = false)
    val ownerId: Long,
    @Column(nullable = false)
    val evidence: String,
    @Column(columnDefinition = "TEXT", nullable = false)
    val variantNames: String,
    @Column(nullable = false)
    var nextAttemptAt: Instant = Instant.now(),
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PhotoDeletionStatus = PhotoDeletionStatus.PENDING
    @Column(columnDefinition = "TEXT", nullable = false)
    var completedVariants: String = ""
    var attempts: Int = 0
    var totalAttempts: Int = 0
    var reprocessCount: Int = 0
    var leaseUntil: Instant? = null
    var claimToken: UUID? = null
    @Column(columnDefinition = "TEXT")
    var lastError: String? = null
    var finishedAt: Instant? = null
    var outcome: String? = null

    fun ownsClaim(token: UUID, now: Instant): Boolean =
        status == PhotoDeletionStatus.PROCESSING && claimToken == token && leaseUntil?.isAfter(now) == true

    fun finish(now: Instant, reason: String) {
        status = PhotoDeletionStatus.COMPLETED
        finishedAt = now
        outcome = reason
        leaseUntil = null
        claimToken = null
    }
}
