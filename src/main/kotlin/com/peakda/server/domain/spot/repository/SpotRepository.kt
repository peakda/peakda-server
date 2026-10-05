package com.peakda.server.domain.spot.repository

import com.peakda.server.common.persistence.LastModifiedRow
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SpotRepository : JpaRepository<Spot, Long> {
    fun findByTypeAndAttractionId(type: SpotType, attractionId: Long): Spot?
    fun findByTypeAndKakaoPlaceId(type: SpotType, kakaoPlaceId: String): Spot?

    /** 명소 id 묶음에 대응하는 (이미 materialize 된) 명소형 Spot 들. 지도 핀에 spotId 를 채울 때 쓴다. */
    fun findByTypeAndAttractionIdIn(type: SpotType, attractionIds: Collection<Long>): List<Spot>

    /** [id] 이후 [type] Spot 의 공개 여부 동기화용 최소 컬럼을 id 오름차순으로 읽는다. 순회 중 갱신해도 누락이 없도록 keyset 으로 페이징한다. */
    fun findByTypeAndIdGreaterThanOrderByIdAsc(type: SpotType, id: Long, pageable: Pageable): List<SpotVisibilityRow>

    @Modifying
    @Query(value = "UPDATE spots SET visible = :visible, updated_at = now() WHERE id IN (:ids)", nativeQuery = true)
    fun updateVisibleByIdIn(@Param("ids") ids: Collection<Long>, @Param("visible") visible: Boolean): Int

    /** [type] 공개 Spot 의 id·수정 시각을 id 오름차순으로 (sitemap). */
    fun findByTypeAndVisibleTrueOrderByIdAsc(type: SpotType): List<LastModifiedRow>

    /** 이름 부분일치(대소문자 무시) 스팟 검색. 비공개(visible=false) 는 제외. */
    fun findByVisibleTrueAndNameContainingIgnoreCase(name: String, pageable: Pageable): Page<Spot>

    @Query(
        """
            SELECT s FROM Spot s
            WHERE s.visible = true
              AND s.type = :type
              AND s.latitude BETWEEN :minLat AND :maxLat
              AND s.longitude BETWEEN :minLng AND :maxLng
        """,
    )
    fun findVisibleInBoundingBox(
        @Param("type") type: SpotType,
        @Param("minLat") minLat: Double,
        @Param("maxLat") maxLat: Double,
        @Param("minLng") minLng: Double,
        @Param("maxLng") maxLng: Double,
    ): List<Spot>
}

/** Spot 공개 여부 동기화에 필요한 컬럼만 읽는 projection. */
interface SpotVisibilityRow {
    val id: Long
    val attractionId: Long?
    val visible: Boolean
}
