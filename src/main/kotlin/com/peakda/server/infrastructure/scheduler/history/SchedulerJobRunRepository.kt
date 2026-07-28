package com.peakda.server.infrastructure.scheduler.history

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface SchedulerJobRunRepository : JpaRepository<SchedulerJobRun, Long> {
    fun findTop20ByJobNameOrderByStartedAtDesc(jobName: String): List<SchedulerJobRun>

    /**
     * 잡 이름·상태·시작 시각을 선택 조건으로 실행 이력을 조회한다.
     *
     * 선택 조건을 `(:param IS NULL OR ...)` 로 쓰면 PostgreSQL 이 실패한다.
     * Hibernate 가 같은 이름의 파라미터를 두 개의 placeholder 로 펴는데,
     * `IS NULL` 쪽 placeholder 는 타입을 유추할 문맥이 없어
     * `could not determine data type of parameter $N` (SQLState 42P18) 이 난다.
     * 그래서 파라미터를 NOT NULL 컬럼과 함께 `COALESCE` 에 넣어 컬럼에서 타입을 가져오게 한다.
     * `job_name`·`status`·`started_at` 은 모두 NOT NULL 이므로 값이 없으면 조건이 항상 참이 된다.
     */
    @Query(
        """
            SELECT r
            FROM SchedulerJobRun r
            WHERE r.jobName = COALESCE(:jobName, r.jobName)
              AND r.status = COALESCE(:status, r.status)
              AND r.startedAt >= COALESCE(:since, r.startedAt)
            ORDER BY r.id DESC
        """,
    )
    fun findRuns(
        @Param("jobName") jobName: String?,
        @Param("status") status: SchedulerJobStatus?,
        @Param("since") since: Instant?,
        pageable: Pageable,
    ): Page<SchedulerJobRun>

    fun existsByJobNameAndStatus(jobName: String, status: SchedulerJobStatus): Boolean

    @Query(
        value = """
            SELECT DISTINCT ON (job_name) *
            FROM scheduler_job_runs
            ORDER BY job_name, id DESC
        """,
        nativeQuery = true,
    )
    fun findLatestRunPerJob(): List<SchedulerJobRun>
}
