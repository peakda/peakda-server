package com.peakda.server.domain.launchalert.application

import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform

data class RequestLaunchAlertDispatchCommand(
    val platform: LaunchAlertPlatform,
    val storeUrl: String,
)
