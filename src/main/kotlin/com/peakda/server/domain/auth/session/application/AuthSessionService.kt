package com.peakda.server.domain.auth.session.application

import com.peakda.server.domain.auth.session.entity.AuthSession
import com.peakda.server.domain.auth.session.presentation.response.DeviceSessionListResponse
import com.peakda.server.domain.auth.session.presentation.response.DeviceSessionResponse
import com.peakda.server.domain.auth.session.repository.AuthSessionRepository
import com.peakda.server.domain.user.exception.UserNotFoundException
import com.peakda.server.domain.user.repository.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 로그인 세션(기기)의 생성·해제와 기기 목록. 토큰 발급·회전은 TokenIssueService 가 맡고 여기를 호출한다.
 */
@Service
@Transactional
class AuthSessionService(
    private val authSessionRepository: AuthSessionRepository,
    private val userRepository: UserRepository,
    private val authSessionCache: AuthSessionCache,
    private val eventPublisher: ApplicationEventPublisher,
    private val properties: AuthSessionProperties,
) {

    /**
     * 새 세션을 만든다. 같은 기기의 기존 세션은 교체하고, 기기 수가 한도를 넘으면 가장 오래 안 쓴 세션부터 해제한다.
     * 동시에 두 기기에서 로그인해도 한도를 넘지 않도록 사용자 행을 잠근 뒤 센다.
     */
    fun open(
        userId: Long,
        sessionId: UUID,
        device: SessionDevice,
        refreshTokenHash: String,
        now: Instant,
        expiresAt: Instant,
    ): AuthSession {
        userRepository.findByIdForUpdate(userId) ?: throw UserNotFoundException()
        authSessionRepository.deleteExpiredByUserId(userId, now)

        authSessionRepository.findByUserIdAndDeviceId(userId, device.deviceId)?.let { replaced ->
            authSessionRepository.delete(replaced)
            authSessionCache.markRevoked(listOf(replaced.id))
        }
        // 같은 (user_id, device_id) 를 지우고 다시 넣으므로, insert 보다 delete 가 먼저 나가게 한다.
        authSessionRepository.flush()

        val active = authSessionRepository.findByUserIdOrderByLastUsedAtAsc(userId)
        val overflow = active.size - (properties.maxDevicesPerUser - 1)
        if (overflow > 0) {
            active.take(overflow).forEach(::revoke)
            authSessionRepository.flush()
        }

        return authSessionRepository.save(
            AuthSession(
                id = sessionId,
                userId = userId,
                deviceId = device.deviceId,
                deviceName = device.deviceName,
                clientType = device.clientType,
                platform = device.platform,
                refreshTokenHash = refreshTokenHash,
                lastUsedAt = now,
                expiresAt = expiresAt,
            ),
        )
    }

    /** 재발급 중인 세션을 잠근다. 같은 세션의 재발급은 한 번에 하나만 처리된다. */
    fun findForRotation(sessionId: UUID): AuthSession? = authSessionRepository.findByIdForUpdate(sessionId)

    fun revoke(session: AuthSession) {
        authSessionRepository.delete(session)
        authSessionCache.markRevoked(listOf(session.id))
        eventPublisher.publishEvent(AuthSessionRevokedEvent(session.userId, session.deviceId))
    }

    @Transactional(readOnly = true)
    fun list(userId: Long, currentSessionId: UUID?, now: Instant = Instant.now()): DeviceSessionListResponse {
        val sessions = authSessionRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .filterNot { it.isExpired(now) }
            .map { DeviceSessionResponse.from(it, current = it.id == currentSessionId) }
        return DeviceSessionListResponse(
            count = sessions.size,
            maxCount = properties.maxDevicesPerUser,
            devices = sessions,
        )
    }

    /** 기기 목록에서 기기 하나를 해제한다. 본인 세션이 아니면 없는 것으로 본다. */
    fun revoke(userId: Long, sessionId: UUID) {
        val session = authSessionRepository.findByIdAndUserId(sessionId, userId) ?: throw DeviceSessionNotFoundException()
        revoke(session)
    }

    /** 로그아웃. 지금 요청한 세션만 끊고 다른 기기는 그대로 둔다. 세션 도입 전 토큰이면 그 토큰만 지운다. */
    fun revokeCurrent(userId: Long, sessionId: UUID?) {
        if (sessionId == null) {
            authSessionCache.deleteLegacyRefreshToken(userId)
            return
        }
        authSessionRepository.findByIdAndUserId(sessionId, userId)?.let(::revoke)
    }

    /** 탈퇴처럼 모든 기기를 끊는다. 푸시 토큰은 호출하는 쪽에서 함께 지우므로 이벤트는 내지 않는다. */
    fun revokeAll(userId: Long) {
        val sessionIds = authSessionRepository.findByUserId(userId).map { it.id }
        authSessionRepository.deleteAllByUserIdInBulk(userId)
        authSessionCache.markRevoked(sessionIds)
        authSessionCache.deleteLegacyRefreshToken(userId)
    }
}
