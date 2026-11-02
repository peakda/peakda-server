package com.peakda.server.domain.user.application

import com.peakda.server.common.security.cookie.CookieProperties
import com.peakda.server.domain.auth.apple.application.AppleUserWithdrawnEvent
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.auth.session.application.AuthSessionService
import com.peakda.server.domain.notification.application.BloomTimingAlertService
import com.peakda.server.domain.notification.application.DeviceTokenService
import com.peakda.server.domain.notification.application.NotificationService
import com.peakda.server.domain.spot.application.SpotFavoriteService
import com.peakda.server.domain.spot.application.SpotRecordService
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Test
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher

class UserWithdrawServiceTest {

    private val userRepository = mock(UserRepository::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val service = UserWithdrawService(
        userRepository = userRepository,
        followService = mock(FollowService::class.java),
        blockService = mock(BlockService::class.java),
        userFavoriteCategoryService = mock(UserFavoriteCategoryService::class.java),
        spotRecordService = mock(SpotRecordService::class.java),
        spotFavoriteService = mock(SpotFavoriteService::class.java),
        notificationService = mock(NotificationService::class.java),
        bloomTimingAlertService = mock(BloomTimingAlertService::class.java),
        deviceTokenService = mock(DeviceTokenService::class.java),
        authSessionService = mock(AuthSessionService::class.java),
        cookieProperties = CookieProperties(domain = null, secure = false, sameSite = "Lax"),
        eventPublisher = eventPublisher,
    )

    @Test
    fun `애플 회원이 탈퇴하면 익명화하기 전의 애플 사용자 식별자로 토큰 폐기를 알린다`() {
        val user = userOf(OAuth2LoginType.APPLE)

        service.withdraw(USER_ID, mock(HttpServletResponse::class.java))

        val order = inOrder(eventPublisher, user)
        order.verify(eventPublisher).publishEvent(AppleUserWithdrawnEvent(USER_ID, APPLE_USER_ID))
        order.verify(user).withdraw()
    }

    @Test
    fun `애플이 아닌 회원의 탈퇴는 애플 토큰 폐기를 알리지 않는다`() {
        userOf(OAuth2LoginType.KAKAO)

        service.withdraw(USER_ID, mock(HttpServletResponse::class.java))

        verifyNoInteractions(eventPublisher)
    }

    private fun userOf(provider: OAuth2LoginType): User = mock(User::class.java).also {
        `when`(it.provider).thenReturn(provider)
        `when`(it.providerId).thenReturn(APPLE_USER_ID)
        `when`(userRepository.findByIdForUpdate(USER_ID)).thenReturn(it)
    }

    companion object {
        private const val USER_ID = 42L
        private const val APPLE_USER_ID = "001234.0123456789abcdef0123456789abcdef.0123"
    }
}
