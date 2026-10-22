package com.peakda.server.domain.launchalert.application

import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform

data class UpdateLaunchAlertMailTemplateCommand(
    val platform: LaunchAlertPlatform,
    val content: LaunchAlertMailContent,
)
