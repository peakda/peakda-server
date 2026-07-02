package com.peakda.server.domain.user.repository

import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.entity.UserStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByProviderAndProviderId(provider: OAuth2LoginType, providerId: String): User?
    fun existsByNickname(nickname: String): Boolean

    /** 닉네임 부분일치(대소문자 무시) 사용자 검색. 탈퇴(익명화) 계정은 [status] 로 걸러 제외한다. */
    fun findByStatusAndNicknameContainingIgnoreCase(status: UserStatus, nickname: String, pageable: Pageable): Page<User>
}
