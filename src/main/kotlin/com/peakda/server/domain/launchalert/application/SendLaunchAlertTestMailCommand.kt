package com.peakda.server.domain.launchalert.application

import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform

data class SendLaunchAlertTestMailCommand(
    val platform: LaunchAlertPlatform,
    val storeUrl: String,
    val email: String,
)
