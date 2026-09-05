package com.peakda.server.domain.curation.repository

import com.peakda.server.domain.curation.entity.CurationChapter
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CurationChapterRepository : JpaRepository<CurationChapter, Long> {
    fun findByCurationIdOrderBySortOrderAsc(curationId: Long): List<CurationChapter>

    /**
     * 큐레이션의 챕터를 즉시 삭제한다.
     *
     * 파생 delete 는 삭제를 영속성 컨텍스트에 쌓아두고 flush 시점에 실행하는데, Hibernate 의 flush 순서가
     * INSERT → DELETE 라서 주차 단위 전량 교체에서 같은 `(curation_id, sort_order)` 로 다시 넣는 INSERT 가
     * 삭제보다 먼저 나가 유니크 제약을 위반한다. 벌크 JPQL 은 호출 시점에 DB 로 나가므로 역전이 생기지 않는다.
     */
    @Modifying
    @Query("DELETE FROM CurationChapter c WHERE c.curationId = :curationId")
    fun deleteByCurationId(@Param("curationId") curationId: Long)
}
