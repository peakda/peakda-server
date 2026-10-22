package com.peakda.server.domain.launchalert.repository

import com.peakda.server.domain.launchalert.entity.LaunchAlertMailTemplate
import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LaunchAlertMailTemplateRepository : JpaRepository<LaunchAlertMailTemplate, LaunchAlertPlatform> {

    /** 문구 수정과 발송 요청이 서로를 기다리게 잡는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM LaunchAlertMailTemplate t WHERE t.platform = :platform")
    fun findByIdForUpdate(@Param("platform") platform: LaunchAlertPlatform): LaunchAlertMailTemplate?
}
