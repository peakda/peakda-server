package com.peakda.server.domain.notification.repository

import com.peakda.server.domain.notification.entity.DeviceToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface DeviceTokenRepository : JpaRepository<DeviceToken, Long> {

    fun findByUserId(userId: Long): List<DeviceToken>

    fun deleteByUserIdAndToken(userId: Long, token: String)

    fun deleteByUserId(userId: Long)

    /**
     * 디바이스 토큰을 멱등하게 등록한다. 이미 등록된 토큰이면 소유자와 플랫폼을 갱신하므로
     * 기기 소유자가 변경된 경우에도 토큰은 한 사용자에게만 귀속된다.
     */
    @Modifying
    @Query(
        value = """
            INSERT INTO device_tokens (user_id, token, platform, created_at, updated_at)
            VALUES (:userId, :token, :platform, now(), now())
            ON CONFLICT (token) DO UPDATE SET
                user_id = :userId,
                platform = :platform,
                updated_at = now()
        """,
        nativeQuery = true,
    )
    fun upsert(userId: Long, token: String, platform: String)
}
