package com.peakda.server.domain.spot.repository

import com.peakda.server.domain.spot.entity.SpotFavorite
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface SpotFavoriteRepository : JpaRepository<SpotFavorite, Long> {
    fun findByUserIdAndSpotId(userId: Long, spotId: Long): SpotFavorite?
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<SpotFavorite>
    fun countByUserId(userId: Long): Long
    fun deleteByUserIdAndSpotId(userId: Long, spotId: Long)
    fun deleteByUserId(userId: Long)

    /** 찜이 많은 순서로 스팟 id·찜 수를 뽑는다 ("인기 스팟"/트렌딩 소스). [pageable] 로 상위 N개만 자른다. */
    @Query(
        """
            SELECT f.spotId AS spotId, COUNT(f) AS favoriteCount
            FROM SpotFavorite f
            GROUP BY f.spotId
            ORDER BY COUNT(f) DESC
        """,
    )
    fun findTrendingSpotIds(pageable: Pageable): List<SpotFavoriteCount>

    /**
     * 찜을 멱등하게 추가한다. 이미 같은 (user, spot) 이 있으면 아무 것도 하지 않으므로
     * 동시 요청에서도 유니크 제약 위반 예외 없이 단일 행을 보장한다.
     */
    @Modifying
    @Query(
        value = """
            INSERT INTO spot_favorites (user_id, spot_id, notify_enabled, created_at, updated_at)
            VALUES (:userId, :spotId, TRUE, now(), now())
            ON CONFLICT (user_id, spot_id) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(userId: Long, spotId: Long)
}

/** [SpotFavoriteRepository.findTrendingSpotIds] 프로젝션. */
interface SpotFavoriteCount {
    val spotId: Long
    val favoriteCount: Long
}
