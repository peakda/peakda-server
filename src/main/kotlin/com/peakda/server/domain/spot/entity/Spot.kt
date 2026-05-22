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

@Entity
@Table(
    name = "spots",
    indexes = [
        Index(name = "ix_spots_attraction_id", columnList = "attraction_id"),
        Index(name = "ix_spots_lat_lng", columnList = "latitude,longitude"),
        Index(name = "ix_spots_kakao_place_id", columnList = "kakao_place_id"),
    ],
)
class Spot(
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "TEXT")
    var type: SpotType,

    @Column(name = "attraction_id")
    var attractionId: Long? = null,

    @Column(name = "name", nullable = false, columnDefinition = "TEXT")
    var name: String,

    @Column(name = "address", columnDefinition = "TEXT")
    var address: String? = null,

    @Column(name = "latitude", nullable = false)
    var latitude: Double,

    @Column(name = "longitude", nullable = false)
    var longitude: Double,

    @Column(name = "kakao_place_id", columnDefinition = "TEXT")
    var kakaoPlaceId: String? = null,

    @Column(name = "created_by_user_id")
    var createdByUserId: Long? = null,

    @Column(name = "visible", nullable = false)
    var visible: Boolean = true,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
