package com.peakda.server.domain.spot.repository

import com.peakda.server.domain.spot.entity.PhotoDeletionStatus

interface PhotoDeletionStatusCount {
    val status: PhotoDeletionStatus
    val total: Long
}
