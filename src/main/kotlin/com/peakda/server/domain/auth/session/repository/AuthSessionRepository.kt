package com.peakda.server.domain.auth.session.repository

import com.peakda.server.domain.auth.session.entity.AuthSession
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface AuthSessionRepository : JpaRepository<AuthSession, UUID> {

    /** 같은 세션의 재발급이 동시에 오면 한 요청씩 처리해야 회전 결과가 갈리지 않는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AuthSession s WHERE s.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): AuthSession?

    fun findByIdAndUserId(id: UUID, userId: Long): AuthSession?

    fun findByUserIdAndDeviceId(userId: Long, deviceId: String): AuthSession?

    fun findByUserIdOrderByLastUsedAtAsc(userId: Long): List<AuthSession>

    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<AuthSession>

    fun findByUserId(userId: Long): List<AuthSession>

    /** 이미 다른 요청이 지운 세션이면 0 을 돌려준다. 엔티티 삭제는 이 경우 낙관적 락 예외로 실패한다. */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM AuthSession s WHERE s.id = :id")
    fun deleteByIdReturningCount(@Param("id") id: UUID): Int

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM AuthSession s WHERE s.userId = :userId AND s.expiresAt <= :now")
    fun deleteExpiredByUserId(@Param("userId") userId: Long, @Param("now") now: Instant): Int

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM AuthSession s WHERE s.userId = :userId")
    fun deleteAllByUserIdInBulk(@Param("userId") userId: Long): Int
}
