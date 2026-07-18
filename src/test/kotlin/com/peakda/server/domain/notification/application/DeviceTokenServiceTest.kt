package com.peakda.server.domain.notification.application

import com.peakda.server.domain.notification.entity.DevicePlatform
import com.peakda.server.domain.notification.repository.DeviceTokenRepository
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class DeviceTokenServiceTest {

    private val deviceTokenRepository = mock(DeviceTokenRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val service = DeviceTokenService(deviceTokenRepository, userRepository)

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

    companion object {
        private const val USER_ID = 42L
        private const val TOKEN = "device-token"
    }
}
