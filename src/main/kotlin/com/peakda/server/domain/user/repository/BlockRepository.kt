package com.peakda.server.domain.user.repository

import com.peakda.server.domain.user.entity.Block
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface BlockRepository : JpaRepository<Block, Long> {
    fun deleteByBlockerIdAndBlockedId(blockerId: Long, blockedId: Long)
    fun findByBlockerIdOrderByCreatedAtDesc(blockerId: Long, pageable: Pageable): Page<Block>

    /** 피드 등에서 차단 대상을 걸러낼 때 쓰는, 페이징 없는 전체 차단 대상 id 목록. */
    @Query("select b.blockedId from Block b where b.blockerId = :blockerId")
    fun findBlockedIdsByBlockerId(blockerId: Long): List<Long>

    /** 사용자가 맺은 모든 차단 관계(차단함·차단당함 양방향)를 삭제한다. 계정 탈퇴 시 사용. */
    @Modifying
    @Query("delete from Block b where b.blockerId = :userId or b.blockedId = :userId")
    fun deleteAllByUserId(userId: Long)

    /**
     * 차단을 멱등하게 추가한다. 이미 같은 (blocker, blocked) 이 있으면 무시되므로
     * 동시 요청에서도 유니크 제약 위반 예외 없이 단일 행을 보장한다.
     */
    @Modifying
    @Query(
        value = """
            INSERT INTO user_blocks (blocker_id, blocked_id, created_at, updated_at)
            VALUES (:blockerId, :blockedId, now(), now())
            ON CONFLICT (blocker_id, blocked_id) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(blockerId: Long, blockedId: Long)
}
