package com.peakda.server.domain.curation.repository

import com.peakda.server.domain.curation.entity.CurationRecommendation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CurationRecommendationRepository : JpaRepository<CurationRecommendation, Long> {
    fun findByCurationIdOrderBySortOrderAsc(curationId: Long): List<CurationRecommendation>

    /**
     * 큐레이션의 추천 카드를 즉시 삭제한다. 전량 교체에서 INSERT 가 DELETE 보다 먼저 flush 되어
     * 유니크 제약을 위반하는 것을 막는다 (`CurationChapterRepository.deleteByCurationId` 와 같은 이유).
     */
    @Modifying
    @Query("DELETE FROM CurationRecommendation r WHERE r.curationId = :curationId")
    fun deleteByCurationId(@Param("curationId") curationId: Long)
}
