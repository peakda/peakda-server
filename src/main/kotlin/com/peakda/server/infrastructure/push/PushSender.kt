package com.peakda.server.infrastructure.push

import com.peakda.server.domain.notification.entity.DeviceToken

interface PushSender {
    fun send(tokens: List<DeviceToken>, payload: PushPayload)
}
