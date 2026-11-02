package com.peakda.server.domain.user.application

import com.peakda.server.common.security.cookie.CookieProperties
import com.peakda.server.common.security.cookie.CookieUtils
import com.peakda.server.domain.auth.apple.application.AppleUserWithdrawnEvent
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.auth.session.application.AuthSessionService
import com.peakda.server.domain.notification.application.BloomTimingAlertService
import com.peakda.server.domain.notification.application.DeviceTokenService
import com.peakda.server.domain.notification.application.NotificationService
import com.peakda.server.domain.spot.application.SpotFavoriteService
import com.peakda.server.domain.spot.application.SpotRecordService
import com.peakda.server.domain.user.exception.UserNotFoundException
import com.peakda.server.domain.user.repository.UserRepository
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserWithdrawService(
    private val userRepository: UserRepository,
    private val followService: FollowService,
    private val blockService: BlockService,
    private val userFavoriteCategoryService: UserFavoriteCategoryService,
    private val spotRecordService: SpotRecordService,
    private val spotFavoriteService: SpotFavoriteService,
    private val notificationService: NotificationService,
    private val bloomTimingAlertService: BloomTimingAlertService,
    private val deviceTokenService: DeviceTokenService,
    private val authSessionService: AuthSessionService,
    private val cookieProperties: CookieProperties,
    private val eventPublisher: ApplicationEventPublisher,
) {

    /**
     * 계정 탈퇴. 본인 콘텐츠(기록·찜·팔로우·차단)를 삭제하고 사용자를 익명화·비활성화한 뒤
     * 토큰·쿠키를 정리한다. 복구는 지원하지 않는다 (결정 G).
     */
    @Transactional
    fun withdraw(userId: Long, response: HttpServletResponse) {
        val user = userRepository.findByIdForUpdate(userId) ?: throw UserNotFoundException()

        spotRecordService.deleteAllByUser(userId)
        spotFavoriteService.deleteAllByUser(userId)
        followService.deleteAllByUser(userId)
        blockService.deleteAllByUser(userId)
        userFavoriteCategoryService.deleteAllByUser(userId)
        notificationService.deleteAllByUser(userId)
        bloomTimingAlertService.deleteAllByUser(userId)
        deviceTokenService.deleteAllByUser(userId)

        // 애플 토큰 폐기는 커밋 뒤에 한다(AppleTokenRevokeListener). withdraw() 가 providerId 를 익명화하므로 먼저 싣는다.
        if (user.provider == OAuth2LoginType.APPLE) {
            eventPublisher.publishEvent(AppleUserWithdrawnEvent(userId, user.providerId))
        }

        user.withdraw()

        authSessionService.revokeAll(userId)
        response.addHeader("Set-Cookie", CookieUtils.deleteAccessTokenCookie(cookieProperties).toString())
        response.addHeader("Set-Cookie", CookieUtils.deleteRefreshTokenCookie(cookieProperties).toString())
        SecurityContextHolder.clearContext()
    }
}
