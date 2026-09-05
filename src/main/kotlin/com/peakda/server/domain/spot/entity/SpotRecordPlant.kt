package com.peakda.server.domain.spot.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.io.Serializable

@Entity
@Table(
    name = "spot_record_plants",
    indexes = [
        Index(name = "ix_spot_record_plants_plant_id", columnList = "plant_id"),
    ],
)
class SpotRecordPlant(
    @EmbeddedId
    val id: SpotRecordPlantId,
) {
    val spotRecordId: Long get() = id.spotRecordId
    val plantId: Long get() = id.plantId
}

@Embeddable
data class SpotRecordPlantId(
    @Column(name = "spot_record_id", nullable = false)
    val spotRecordId: Long,

    @Column(name = "plant_id", nullable = false)
    val plantId: Long,
) : Serializable
