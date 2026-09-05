package com.peakda.server.domain.location.application

import com.peakda.server.domain.location.entity.LocationAccessChannel
import com.peakda.server.domain.location.entity.LocationServiceType
import java.time.Instant

data class RecordLocationUsageCommand(
    val userId: Long,
    val channel: LocationAccessChannel,
    val service: LocationServiceType,
    val usedAt: Instant,
)
