package com.peakda.server.domain.spot.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/** 기록에 첨부하기 전에 서버가 확인한 업로드. 삭제된 key도 남겨 재첨부를 막는다. */
@Entity
@Table(name = "spot_record_photo_uploads")
class SpotRecordPhotoUpload(
    @Id
    @Column(name = "object_key", columnDefinition = "TEXT")
    val objectKey: String,
    @Column(name = "owner_id", nullable = false)
    val ownerId: Long,
    @Column(name = "variant_names", nullable = false, columnDefinition = "TEXT")
    var variantNames: String,
    @Column(name = "evidence", nullable = false)
    val evidence: String,
    @Column(name = "available", nullable = false)
    var available: Boolean = true,
)
