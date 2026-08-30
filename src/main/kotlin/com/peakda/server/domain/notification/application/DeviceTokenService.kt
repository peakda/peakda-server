package com.peakda.server.domain.notification.application

import com.peakda.server.domain.notification.entity.DevicePlatform
import com.peakda.server.domain.notification.repository.DeviceTokenRepository
import com.peakda.server.domain.user.exception.UserNotFoundException
import com.peakda.server.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
@Transactional
class DeviceTokenService(
    private val deviceTokenRepository: DeviceTokenRepository,
    private val userRepository: UserRepository,
    private val properties: DeviceTokenProperties,
) {

    fun register(userId: Long, token: String, platform: DevicePlatform) {
        userRepository.findByIdForUpdate(userId) ?: throw UserNotFoundException()
        deviceTokenRepository.upsert(userId, token, platform.name)
        deviceTokenRepository.deleteExceeding(userId, properties.maxPerUser)
    }

    fun unregister(userId: Long, token: String) {
        deviceTokenRepository.deleteByUserIdAndToken(userId, token)
    }

    fun deleteAllByUser(userId: Long) {
        deviceTokenRepository.deleteByUserId(userId)
    }

    /**
     * FCM 이 무효(UNREGISTERED·INVALID_ARGUMENT)로 응답한 토큰을 삭제한다.
     * 발송 어댑터는 비동기 리스너와 배치 스레드에서 트랜잭션 밖으로 실행되므로,
     * 삭제 트랜잭션 경계를 이 서비스가 연다.
     */
    fun deleteInvalid(tokens: Collection<String>): Int {
        if (tokens.isEmpty()) return 0
        return deviceTokenRepository.deleteByTokenIn(tokens)
    }

    /**
     * 마지막 등록·갱신이 보관 기간을 지난 토큰을 정리한다.
     * FCM 이 미사용 토큰을 스스로 무효화하므로 서버도 같은 주기로 지운다.
     */
    fun deleteStale(now: Instant = Instant.now()): Int =
        deviceTokenRepository.deleteUpdatedBefore(now.minus(properties.retentionDays, ChronoUnit.DAYS))
}
