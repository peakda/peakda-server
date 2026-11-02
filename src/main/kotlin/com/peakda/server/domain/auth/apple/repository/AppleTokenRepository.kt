package com.peakda.server.domain.auth.apple.repository

import com.peakda.server.domain.auth.apple.entity.AppleToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface AppleTokenRepository : JpaRepository<AppleToken, String> {

    /** 같은 사용자가 다시 로그인하면 새 토큰으로 바꾼다. 동시에 로그인해도 행이 하나만 남는다. */
    @Transactional
    @Modifying
    @Query(
        nativeQuery = true,
        value = """
            INSERT INTO apple_tokens (apple_user_id, refresh_token, created_at, updated_at)
            VALUES (:appleUserId, :refreshToken, now(), now())
            ON CONFLICT (apple_user_id)
            DO UPDATE SET refresh_token = EXCLUDED.refresh_token, updated_at = now()
        """,
    )
    fun upsert(
        @Param("appleUserId") appleUserId: String,
        @Param("refreshToken") refreshToken: String,
    )
}
