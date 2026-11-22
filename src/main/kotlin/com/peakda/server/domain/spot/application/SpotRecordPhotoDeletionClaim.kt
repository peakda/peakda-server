package com.peakda.server.domain.spot.application

import java.util.UUID

data class SpotRecordPhotoDeletionClaim(val id: Long, val objectKey: String, val token: UUID)
