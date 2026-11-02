package com.peakda.server.domain.auth.apple.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 애플 로그인 사용자의 애플 리프레시 토큰. 회원 탈퇴 때 애플 토큰 폐기 API 를 부르는 데만 쓴다.
 * 쓰기는 [com.peakda.server.domain.auth.apple.repository.AppleTokenRepository.upsert] 로만 한다.
 */
@Entity
@Table(name = "apple_tokens")
class AppleToken(
    /** identity token 의 sub. 애플 회원의 users.provider_id 와 같다. */
    @Id
    @Column(name = "apple_user_id", columnDefinition = "TEXT")
    val appleUserId: String,

    @Column(name = "refresh_token", nullable = false, columnDefinition = "TEXT")
    val refreshToken: String,
) : BaseTimeEntity()
