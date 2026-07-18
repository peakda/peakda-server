package com.peakda.server.domain.notification.application

import com.peakda.server.domain.notification.entity.DevicePlatform
import com.peakda.server.domain.notification.repository.DeviceTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class DeviceTokenService(
    private val deviceTokenRepository: DeviceTokenRepository,
) {

    fun register(userId: Long, token: String, platform: DevicePlatform) {
        deviceTokenRepository.upsert(userId, token, platform.name)
    }

    fun unregister(userId: Long, token: String) {
        deviceTokenRepository.deleteByUserIdAndToken(userId, token)
    }

    fun deleteAllByUser(userId: Long) {
        deviceTokenRepository.deleteByUserId(userId)
    }
}
