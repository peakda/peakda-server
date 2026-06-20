package com.peakda.server.domain.user.application

import com.peakda.server.common.security.cookie.CookieProperties
import com.peakda.server.common.security.cookie.CookieUtils
import com.peakda.server.domain.auth.application.RefreshTokenService
import com.peakda.server.domain.spot.application.SpotFavoriteService
import com.peakda.server.domain.spot.application.SpotRecordService
import com.peakda.server.domain.user.exception.UserNotFoundException
import com.peakda.server.domain.user.repository.UserRepository
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserWithdrawService(
    private val userRepository: UserRepository,
    private val followService: FollowService,
    private val spotRecordService: SpotRecordService,
    private val spotFavoriteService: SpotFavoriteService,
    private val refreshTokenService: RefreshTokenService,
    private val cookieProperties: CookieProperties,
) {

    /**
     * 계정 탈퇴. 본인 콘텐츠(기록·찜·팔로우)를 삭제하고 사용자를 익명화·비활성화한 뒤
     * 토큰·쿠키를 정리한다. 복구는 지원하지 않는다 (결정 G).
     */
    @Transactional
    fun withdraw(userId: Long, response: HttpServletResponse) {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException() }

        spotRecordService.deleteAllByUser(userId)
        spotFavoriteService.deleteAllByUser(userId)
        followService.deleteAllByUser(userId)

        user.withdraw()

        refreshTokenService.deleteRefreshToken(userId)
        response.addHeader("Set-Cookie", CookieUtils.deleteAccessTokenCookie(cookieProperties).toString())
        response.addHeader("Set-Cookie", CookieUtils.deleteRefreshTokenCookie(cookieProperties).toString())
        SecurityContextHolder.clearContext()
    }
}
