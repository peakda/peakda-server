package com.peakda.server.domain.notification.application

import com.peakda.server.domain.notification.entity.DevicePlatform
import com.peakda.server.domain.notification.repository.DeviceTokenRepository
import com.peakda.server.domain.user.exception.UserNotFoundException
import com.peakda.server.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class DeviceTokenService(
    private val deviceTokenRepository: DeviceTokenRepository,
    private val userRepository: UserRepository,
) {

    fun register(userId: Long, token: String, platform: DevicePlatform) {
        userRepository.findByIdForUpdate(userId) ?: throw UserNotFoundException()
        deviceTokenRepository.upsert(userId, token, platform.name)
        deviceTokenRepository.deleteExceeding(userId, MAX_DEVICES_PER_USER)
    }

    fun unregister(userId: Long, token: String) {
        deviceTokenRepository.deleteByUserIdAndToken(userId, token)
    }

    fun deleteAllByUser(userId: Long) {
        deviceTokenRepository.deleteByUserId(userId)
    }

    companion object {
        private const val MAX_DEVICES_PER_USER = 10
    }
}
