package com.peakda.server.domain.launchalert.repository

import com.peakda.server.domain.launchalert.entity.LaunchAlertDispatch
import com.peakda.server.domain.launchalert.entity.LaunchAlertDispatchStatus
import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import org.springframework.data.jpa.repository.JpaRepository

interface LaunchAlertDispatchRepository : JpaRepository<LaunchAlertDispatch, Long> {

    fun existsByStatus(status: LaunchAlertDispatchStatus): Boolean

    fun existsByPlatformAndStatus(platform: LaunchAlertPlatform, status: LaunchAlertDispatchStatus): Boolean

    fun findFirstByStatusOrderByIdAsc(status: LaunchAlertDispatchStatus): LaunchAlertDispatch?
}
