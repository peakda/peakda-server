package com.peakda.server.domain.spot.repository

import com.peakda.server.domain.spot.entity.SpotFavorite
import com.peakda.server.domain.spot.entity.SpotType
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

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
     * 만개 임박 알림 대상 찜을 조회한다. 알림이 켜졌고(notify_enabled) 노출 중인 명소형 스팟에 걸린 찜만
     * (수신자·스팟명·명소 id) 로 뽑는다. 실제 "만개 임박" 판정은 호출측(알림 서비스)이 개화 추정과 결합해 수행한다.
     */
    @Query(
        """
            SELECT f.userId AS userId,
                   s.id AS spotId,
                   s.name AS spotName,
                   s.attractionId AS attractionId
            FROM SpotFavorite f, Spot s
            WHERE s.id = f.spotId
              AND f.notifyEnabled = true
              AND s.type = :attractionType
              AND s.visible = true
              AND s.attractionId IS NOT NULL
            ORDER BY f.id
        """,
    )
    fun findAlertTargets(
        @Param("attractionType") attractionType: SpotType,
        pageable: Pageable,
    ): Slice<AlertTargetFavorite>

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

/** [SpotFavoriteRepository.findAlertTargets] 프로젝션. 명소형 찜의 수신자·스팟명·명소 id. */
interface AlertTargetFavorite {
    val userId: Long
    val spotId: Long
    val spotName: String
    val attractionId: Long
}
