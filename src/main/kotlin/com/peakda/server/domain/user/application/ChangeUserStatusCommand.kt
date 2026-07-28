package com.peakda.server.domain.user.application

import com.peakda.server.domain.user.entity.UserStatus

data class ChangeUserStatusCommand(
    val status: UserStatus,
    val memo: String? = null,
)
