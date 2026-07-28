package com.peakda.server.domain.admin.application

import com.peakda.server.domain.admin.presentation.response.AdminSessionResponse
import com.peakda.server.domain.user.entity.User
import org.springframework.stereotype.Service

@Service
class AdminSessionService {
    fun getSession(user: User): AdminSessionResponse = AdminSessionResponse.from(user)
}
