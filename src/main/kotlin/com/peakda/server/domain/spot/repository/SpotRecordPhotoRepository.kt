package com.peakda.server.domain.spot.repository

import com.peakda.server.domain.spot.entity.SpotRecordPhoto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SpotRecordPhotoRepository : JpaRepository<SpotRecordPhoto, Long> {
    fun findBySpotRecordIdOrderBySortOrderAsc(spotRecordId: Long): List<SpotRecordPhoto>
    fun findBySpotRecordIdIn(spotRecordIds: Collection<Long>): List<SpotRecordPhoto>
    fun deleteBySpotRecordId(spotRecordId: Long)

    /**
     * 여러 스팟의 최근 게시 기록 사진을 스팟당 [limit] 장까지 한 번에 조회한다.
     * 기록의 최신 기준은 방문일(없으면 작성일)이며, 같은 기록 안에서는 정렬 순서를 따른다.
     * 몇 장을 쓸지는 호출측(application)이 정한다.
     *
     * 프로젝션 컬럼에 카멜 별칭을 붙이지 않는다. PostgreSQL 은 따옴표 없는 별칭을 소문자로 접어
     * `spotId` 가 `spotid` 라벨이 되고, 그러면 인터페이스 프로젝션이 값을 찾지 못한다.
     */
    @Query(
        value = """
            SELECT spot_id, object_key
            FROM (
                SELECT r.spot_id AS spot_id,
                       p.object_key AS object_key,
                       ROW_NUMBER() OVER (
                           PARTITION BY r.spot_id
                           ORDER BY COALESCE(r.visited_date, r.created_at::date) DESC,
                                    r.created_at DESC,
                                    p.sort_order ASC
                       ) AS rn
                FROM spot_records r
                JOIN spot_record_photos p ON p.spot_record_id = r.id
                WHERE r.spot_id IN (:spotIds)
                  AND r.status = :status
            ) ranked
            WHERE rn <= :limit
            ORDER BY spot_id ASC, rn ASC
        """,
        nativeQuery = true,
    )
    fun findRecentPhotosBySpotIds(
        @Param("spotIds") spotIds: Collection<Long>,
        @Param("status") status: String,
        @Param("limit") limit: Int,
    ): List<SpotPhoto>
}

/** [SpotRecordPhotoRepository.findRecentPhotosBySpotIds] 프로젝션. */
interface SpotPhoto {
    val spotId: Long
    val objectKey: String
}
