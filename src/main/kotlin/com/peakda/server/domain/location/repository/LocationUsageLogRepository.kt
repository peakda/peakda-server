package com.peakda.server.domain.location.repository

import com.peakda.server.domain.location.entity.LocationServiceType
import com.peakda.server.domain.location.entity.LocationUsageLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface LocationUsageLogRepository : JpaRepository<LocationUsageLog, Long> {

    /**
     * 제공서비스·이용일시 구간을 선택 조건으로 확인자료를 조회한다. 조건은 서로 독립적으로 AND 결합된다.
     *
     * 선택 조건을 `(:param IS NULL OR ...)` 로 쓰면 PostgreSQL 이 실패한다.
     * Hibernate 가 같은 이름의 파라미터를 두 개의 placeholder 로 펴는데,
     * `IS NULL` 쪽 placeholder 는 타입을 유추할 문맥이 없어
     * `could not determine data type of parameter $N` (SQLState 42P18) 이 난다.
     * 그래서 파라미터를 NOT NULL 컬럼과 함께 `COALESCE` 에 넣어 컬럼에서 타입을 가져오게 한다.
     * `service`·`used_at` 은 모두 NOT NULL 이므로 값이 없으면 조건이 항상 참이 된다.
     */
    @Query(
        """
            SELECT l FROM LocationUsageLog l
            WHERE l.service = COALESCE(:service, l.service)
              AND l.usedAt >= COALESCE(:from, l.usedAt)
              AND l.usedAt <= COALESCE(:to, l.usedAt)
            ORDER BY l.id DESC
        """,
    )
    fun search(
        @Param("service") service: LocationServiceType?,
        @Param("from") from: Instant?,
        @Param("to") to: Instant?,
        pageable: Pageable,
    ): Page<LocationUsageLog>

    /**
     * [search] 에 대상 사용자 조건을 더한 조회. 이메일 검색으로 좁힌 사용자 집합에만 적용한다.
     *
     * 컬렉션 파라미터는 [search] 의 `COALESCE` 방식으로 선택 조건화할 수 없어 메서드를 나눴다.
     * 빈 컬렉션은 넘기지 않는다 — 호출부에서 빈 결과로 단락시킨다.
     */
    @Query(
        """
            SELECT l FROM LocationUsageLog l
            WHERE l.userId IN :userIds
              AND l.service = COALESCE(:service, l.service)
              AND l.usedAt >= COALESCE(:from, l.usedAt)
              AND l.usedAt <= COALESCE(:to, l.usedAt)
            ORDER BY l.id DESC
        """,
    )
    fun searchByUserIds(
        @Param("userIds") userIds: Collection<Long>,
        @Param("service") service: LocationServiceType?,
        @Param("from") from: Instant?,
        @Param("to") to: Instant?,
        pageable: Pageable,
    ): Page<LocationUsageLog>
}
