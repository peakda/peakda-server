package com.peakda.server.domain.notification.application

import com.peakda.server.domain.notification.entity.DevicePlatform
import com.peakda.server.domain.notification.repository.DeviceTokenRepository
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.never
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant
import java.time.temporal.ChronoUnit

class DeviceTokenServiceTest {

    private val deviceTokenRepository = mock(DeviceTokenRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val properties = DeviceTokenProperties()
    private val service = DeviceTokenService(deviceTokenRepository, userRepository, properties)

    init {
        `when`(userRepository.findByIdForUpdate(USER_ID)).thenReturn(mock(User::class.java))
    }

    @Test
    fun `디바이스 토큰 등록은 사용자 행 잠금 후 처리한다`() {
        service.register(USER_ID, TOKEN, DevicePlatform.ANDROID)

        inOrder(userRepository, deviceTokenRepository).apply {
            verify(userRepository).findByIdForUpdate(USER_ID)
            verify(deviceTokenRepository).upsert(USER_ID, TOKEN, DevicePlatform.ANDROID.name)
            verify(deviceTokenRepository).deleteExceeding(USER_ID, 10)
        }
    }

    @Test
    fun `디바이스 토큰 등록은 네이티브 upsert에 위임한다`() {
        service.register(USER_ID, TOKEN, DevicePlatform.ANDROID)

        verify(deviceTokenRepository).upsert(USER_ID, TOKEN, DevicePlatform.ANDROID.name)
    }

    @Test
    fun `디바이스 토큰 등록 후 사용자별 상한 초과분 정리를 위임한다`() {
        service.register(USER_ID, TOKEN, DevicePlatform.ANDROID)

        verify(deviceTokenRepository).deleteExceeding(USER_ID, 10)
    }

    @Test
    fun `디바이스 토큰 해제는 사용자와 토큰을 함께 확인해 삭제한다`() {
        service.unregister(USER_ID, TOKEN)

        verify(deviceTokenRepository).deleteByUserIdAndToken(USER_ID, TOKEN)
    }

    @Test
    fun `회원 탈퇴 시 사용자의 모든 디바이스 토큰 삭제를 위임한다`() {
        service.deleteAllByUser(USER_ID)

        verify(deviceTokenRepository).deleteByUserId(USER_ID)
    }

    @Test
    fun `무효 토큰 삭제는 한 번의 일괄 삭제로 위임한다`() {
        service.deleteInvalid(listOf(TOKEN, "other-token"))

        verify(deviceTokenRepository).deleteByTokenIn(listOf(TOKEN, "other-token"))
    }

    @Test
    fun `삭제할 무효 토큰이 없으면 레포지토리를 호출하지 않는다`() {
        service.deleteInvalid(emptyList())

        verify(deviceTokenRepository, never()).deleteByTokenIn(anyCollection())
    }

    @Test
    fun `보관 기간이 지난 토큰은 마지막 갱신 시각 기준으로 정리한다`() {
        val now = Instant.parse("2026-08-30T00:00:00Z")

        service.deleteStale(now)

        verify(deviceTokenRepository).deleteUpdatedBefore(now.minus(270, ChronoUnit.DAYS))
    }

    companion object {
        private const val USER_ID = 42L
        private const val TOKEN = "device-token"
    }
}
