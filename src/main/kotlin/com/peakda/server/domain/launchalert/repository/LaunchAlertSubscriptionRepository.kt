package com.peakda.server.domain.launchalert.repository

import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import com.peakda.server.domain.launchalert.entity.LaunchAlertSubscription
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface LaunchAlertSubscriptionRepository : JpaRepository<LaunchAlertSubscription, Long> {

    /**
     * 신청을 저장하고, 같은 플랫폼·이메일이 이미 있으면 동의 정보만 갱신한다.
     *
     * 존재 확인 후 저장하면 동시에 들어온 같은 신청이 유니크 제약에 걸려 500 이 된다. 한 문장으로 처리한다.
     * 호출하는 서비스는 트랜잭션 없이 횟수 제한(Redis)을 먼저 확인하므로, 커넥션은 이 문장 동안만 잡는다.
     */
    @Transactional
    @Modifying
    @Query(
        value = """
            INSERT INTO launch_alert_subscriptions
                (platform, email, consent_version, consented_at, created_at, updated_at)
            VALUES (:platform, :email, :consentVersion, :consentedAt, :consentedAt, :consentedAt)
            ON CONFLICT ON CONSTRAINT uk_launch_alert_subscriptions_platform_email DO UPDATE SET
                consent_version = EXCLUDED.consent_version,
                consented_at = EXCLUDED.consented_at,
                updated_at = EXCLUDED.updated_at
        """,
        nativeQuery = true,
    )
    fun upsert(
        @Param("platform") platform: String,
        @Param("email") email: String,
        @Param("consentVersion") consentVersion: String,
        @Param("consentedAt") consentedAt: Instant,
    )

    fun findAllByPlatform(platform: LaunchAlertPlatform, pageable: Pageable): Page<LaunchAlertSubscription>

    fun findAllByPlatformAndEmailContaining(
        platform: LaunchAlertPlatform,
        email: String,
        pageable: Pageable,
    ): Page<LaunchAlertSubscription>

    fun countByPlatform(platform: LaunchAlertPlatform): Long

    fun findTop100ByPlatformAndIdGreaterThanOrderByIdAsc(
        platform: LaunchAlertPlatform,
        id: Long,
    ): List<LaunchAlertSubscription>

    /**
     * 메일을 보낸 신청을 한 건씩 바로 지운다. 묶음이 끝난 뒤 한꺼번에 지우면, 그 사이 잡이 끊겼을 때
     * 이미 메일을 받은 사람이 다음 실행에서 또 받는다.
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM LaunchAlertSubscription s WHERE s.id = :id")
    fun deleteSent(@Param("id") id: Long): Int
}
