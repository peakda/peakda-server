package com.peakda.server.domain.attraction.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "attractions",
    uniqueConstraints = [UniqueConstraint(name = "uk_attractions_tour_api_content_id", columnNames = ["tour_api_content_id"])],
    indexes = [Index(name = "idx_attractions_legal_dong_sigungu_code", columnList = "legal_dong_sigungu_code")],
)
class Attraction(
    @Column(name = "tour_api_content_id", nullable = false, columnDefinition = "TEXT")
    val tourApiContentId: String,

    @Column(name = "content_type_code", columnDefinition = "TEXT")
    var contentTypeCode: String? = null,

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    var title: String,

    @Column(name = "address_main", columnDefinition = "TEXT")
    var addressMain: String? = null,

    @Column(name = "address_detail", columnDefinition = "TEXT")
    var addressDetail: String? = null,

    @Column(name = "area_code", columnDefinition = "TEXT")
    var areaCode: String? = null,

    @Column(name = "sigungu_code", columnDefinition = "TEXT")
    var sigunguCode: String? = null,

    /** 법정동 시도 코드 2자리 (TourAPI `lDongRegnCd` 앞 2자리). 행정구역 개편 후 코드다. */
    @Column(name = "legal_dong_area_code", columnDefinition = "TEXT")
    var legalDongAreaCode: String? = null,

    /** 법정동 시군구 코드 5자리 (TourAPI `lDongRegnCd` + `lDongSignguCd`). 관광지 집중률 연결 키. */
    @Column(name = "legal_dong_sigungu_code", columnDefinition = "TEXT")
    var legalDongSigunguCode: String? = null,

    @Column(name = "longitude")
    var longitude: Double? = null,

    @Column(name = "latitude")
    var latitude: Double? = null,

    @Column(name = "primary_image_url", columnDefinition = "TEXT")
    var primaryImageUrl: String? = null,

    @Column(name = "thumbnail_image_url", columnDefinition = "TEXT")
    var thumbnailImageUrl: String? = null,

    @Column(name = "category_major", columnDefinition = "TEXT")
    var categoryMajor: String? = null,

    @Column(name = "category_medium", columnDefinition = "TEXT")
    var categoryMedium: String? = null,

    @Column(name = "category_minor", columnDefinition = "TEXT")
    var categoryMinor: String? = null,

    @Column(name = "lcls_systm_major", columnDefinition = "TEXT")
    var lclsSystmMajor: String? = null,

    @Column(name = "lcls_systm_medium", columnDefinition = "TEXT")
    var lclsSystmMedium: String? = null,

    @Column(name = "lcls_systm_minor", columnDefinition = "TEXT")
    var lclsSystmMinor: String? = null,

    @Column(name = "external_created_at", columnDefinition = "TEXT")
    var externalCreatedAt: String? = null,

    @Column(name = "external_modified_at", columnDefinition = "TEXT")
    var externalModifiedAt: String? = null,

    @Column(name = "visible", nullable = false)
    var visible: Boolean = true,

    /** 마지막으로 열리는지 확인한 썸네일 URL. 동기화로 [thumbnailImageUrl] 이 바뀌면 이전 확인 결과는 저절로 무효가 된다. */
    @Column(name = "thumbnail_checked_url", columnDefinition = "TEXT")
    var thumbnailCheckedUrl: String? = null,

    @Column(name = "thumbnail_checked_at")
    var thumbnailCheckedAt: Instant? = null,

    /** [thumbnailCheckedUrl] 이 관광공사 이미지 서버에서 404/410 이었는지. */
    @Column(name = "thumbnail_missing", nullable = false)
    var thumbnailMissing: Boolean = false,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set

    /**
     * 목록 카드에 쓸 이미지. 관광공사 썸네일(firstimage2)을 먼저 쓰고, 썸네일이 없거나 실제 파일이 없다고 확인됐으면 원본(firstimage)을 쓴다.
     * 관광공사는 썸네일을 만들지 못한 콘텐츠에도 썸네일 URL 을 내려 준다. 깨진 썸네일은 내려 주지 않고 null 로 둬서 호출부가 기록 사진 등으로 대체하게 한다.
     */
    fun cardImageUrl(): String? {
        val thumbnail = thumbnailImageUrl
        val thumbnailMissingConfirmed = thumbnailMissing && thumbnail == thumbnailCheckedUrl
        if (thumbnail != null && !thumbnailMissingConfirmed) return thumbnail
        return primaryImageUrl
    }
}
