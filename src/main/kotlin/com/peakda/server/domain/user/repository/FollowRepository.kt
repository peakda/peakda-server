package com.peakda.server.domain.user.repository

import com.peakda.server.domain.user.entity.Follow
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FollowRepository : JpaRepository<Follow, Long> {
    fun existsByFollowerIdAndFollowingId(followerId: Long, followingId: Long): Boolean
    fun deleteByFollowerIdAndFollowingId(followerId: Long, followingId: Long)

    /** 사용자가 맺은 모든 팔로우 관계(팔로워·팔로잉 양방향)를 삭제한다. 계정 탈퇴 시 사용. */
    @Modifying
    @Query("delete from Follow f where f.followerId = :userId or f.followingId = :userId")
    fun deleteAllByUserId(userId: Long)

    /*
     * 팔로워·팔로잉의 수와 목록은 모두 "상대 사용자가 존재하고 탈퇴(DEACTIVATED)하지 않은" 행만 센다.
     * follows 에는 users FK 가 없어 상대 행이 사라지거나 탈퇴한 팔로우가 남을 수 있는데,
     * 수와 목록이 서로 다른 기준을 쓰면 "팔로잉 5 / 목록 3" 처럼 어긋난다.
     */

    /** 팔로잉 수: 이 사용자가 팔로우하는 사람 수 */
    @Query(
        """
            SELECT COUNT(f) FROM Follow f
            WHERE f.followerId = :followerId
              AND EXISTS (
                SELECT 1 FROM User u
                WHERE u.id = f.followingId AND u.status <> com.peakda.server.domain.user.entity.UserStatus.DEACTIVATED
              )
        """,
    )
    fun countFollowings(@Param("followerId") followerId: Long): Long

    /** 팔로워 수: 이 사용자를 팔로우하는 사람 수 */
    @Query(
        """
            SELECT COUNT(f) FROM Follow f
            WHERE f.followingId = :followingId
              AND EXISTS (
                SELECT 1 FROM User u
                WHERE u.id = f.followerId AND u.status <> com.peakda.server.domain.user.entity.UserStatus.DEACTIVATED
              )
        """,
    )
    fun countFollowers(@Param("followingId") followingId: Long): Long

    @Query(
        """
            SELECT f.followingId AS userId, COUNT(f) AS followerCount
            FROM Follow f
            WHERE f.followingId IN :userIds
              AND EXISTS (
                SELECT 1 FROM User u
                WHERE u.id = f.followerId AND u.status <> com.peakda.server.domain.user.entity.UserStatus.DEACTIVATED
              )
            GROUP BY f.followingId
        """,
    )
    fun countByFollowingIdIn(@Param("userIds") userIds: Collection<Long>): List<FollowerCount>

    /** 팔로워 목록: 대상(followingId)을 팔로우하는 행들을 최근 팔로우 순으로 */
    @Query(
        """
            SELECT f FROM Follow f
            WHERE f.followingId = :followingId
              AND EXISTS (
                SELECT 1 FROM User u
                WHERE u.id = f.followerId AND u.status <> com.peakda.server.domain.user.entity.UserStatus.DEACTIVATED
              )
            ORDER BY f.createdAt DESC
        """,
    )
    fun findFollowers(@Param("followingId") followingId: Long, pageable: Pageable): Page<Follow>

    /** 팔로잉 목록: 대상(followerId)이 팔로우하는 행들을 최근 팔로우 순으로 */
    @Query(
        """
            SELECT f FROM Follow f
            WHERE f.followerId = :followerId
              AND EXISTS (
                SELECT 1 FROM User u
                WHERE u.id = f.followingId AND u.status <> com.peakda.server.domain.user.entity.UserStatus.DEACTIVATED
              )
            ORDER BY f.createdAt DESC
        """,
    )
    fun findFollowings(@Param("followerId") followerId: Long, pageable: Pageable): Page<Follow>

    /**
     * 목록의 각 사용자에 대해 현재 로그인 사용자(followerId)가 팔로우 중인 대상 id 만 추려서 반환한다.
     * 목록 항목별 "팔로우/팔로잉" 버튼 상태를 N+1 없이 한 번에 계산하기 위함.
     */
    @Query("select f.followingId from Follow f where f.followerId = :followerId and f.followingId in :targetIds")
    fun findFollowingTargetIds(followerId: Long, targetIds: Collection<Long>): List<Long>

    /** 팔로잉 피드 — 이 사용자가 팔로우하는 모든 대상 id. */
    @Query("select f.followingId from Follow f where f.followerId = :followerId")
    fun findFollowingIds(followerId: Long): List<Long>

    /**
     * 팔로우를 멱등하게 추가한다. 이미 같은 (follower, following) 이 있으면 무시되므로
     * 동시 요청에서도 유니크 제약 위반 예외 없이 단일 행을 보장한다.
     */
    @Modifying
    @Query(
        value = """
            INSERT INTO follows (follower_id, following_id, created_at, updated_at)
            VALUES (:followerId, :followingId, now(), now())
            ON CONFLICT (follower_id, following_id) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(followerId: Long, followingId: Long)
}

/** [FollowRepository.countByFollowingIdIn] 프로젝션. */
interface FollowerCount {
    val userId: Long
    val followerCount: Long
}
