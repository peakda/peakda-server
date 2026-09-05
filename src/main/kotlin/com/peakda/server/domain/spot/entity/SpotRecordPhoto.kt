package com.peakda.server.domain.spot.entity

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
    name = "spot_record_photos",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_spot_record_photos_record_sort",
            columnNames = ["spot_record_id", "sort_order"],
        ),
    ],
)
class SpotRecordPhoto(
    @Column(name = "spot_record_id", nullable = false)
    val spotRecordId: Long,

    @Column(name = "object_key", nullable = false, columnDefinition = "TEXT")
    var objectKey: String,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
