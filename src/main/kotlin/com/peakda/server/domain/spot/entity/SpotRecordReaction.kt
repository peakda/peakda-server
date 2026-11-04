package com.peakda.server.domain.spot.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "spot_record_reactions",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_spot_record_reactions_user_record_type",
            columnNames = ["user_id", "spot_record_id", "reaction_type"],
        ),
    ],
    indexes = [
        Index(name = "ix_spot_record_reactions_spot_record_id", columnList = "spot_record_id"),
        Index(name = "ix_spot_record_reactions_user_id", columnList = "user_id"),
    ],
)
class SpotRecordReaction(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "spot_record_id", nullable = false)
    val spotRecordId: Long,

    @Column(name = "reaction_type", nullable = false, columnDefinition = "TEXT")
    val reactionType: String,
) : BaseTimeEntity() {

    constructor(userId: Long, spotRecordId: Long, reactionType: ReactionType) :
        this(userId, spotRecordId, reactionType.name)

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
