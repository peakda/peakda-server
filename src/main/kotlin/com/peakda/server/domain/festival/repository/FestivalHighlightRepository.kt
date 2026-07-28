package com.peakda.server.domain.festival.repository

import com.peakda.server.domain.festival.entity.FestivalHighlight
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FestivalHighlightRepository : JpaRepository<FestivalHighlight, Long> {
    fun findByFestivalEditorialIdOrderBySortOrderAsc(festivalEditorialId: Long): List<FestivalHighlight>

    /**
     * 축제 에디토리얼의 주요 볼거리를 즉시 삭제한다.
     *
     * 파생 delete 는 Hibernate flush 시 INSERT보다 늦게 실행될 수 있다.
     * 따라서 같은 정렬 키의 전량 교체가 유니크 제약을 위반할 수 있다.
     * 벌크 JPQL로 호출 시점에 삭제해 재삽입보다 먼저 DB에 반영한다.
     */
    @Modifying
    @Query("DELETE FROM FestivalHighlight h WHERE h.festivalEditorialId = :festivalEditorialId")
    fun deleteByFestivalEditorialId(
        @Param("festivalEditorialId") festivalEditorialId: Long,
    )
}
