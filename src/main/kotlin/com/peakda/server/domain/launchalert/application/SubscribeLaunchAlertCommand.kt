package com.peakda.server.domain.launchalert.application

import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform

data class SubscribeLaunchAlertCommand(
    val platform: LaunchAlertPlatform,
    val email: String,
    val privacyConsent: Boolean,
    val clientIp: String,
)
