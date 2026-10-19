package com.peakda.server.domain.auth.session.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.auth.session.application.AuthSessionService
import com.peakda.server.domain.auth.session.presentation.response.DeviceSessionListResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/users/me/devices")
class DeviceSessionController(
    private val authSessionService: AuthSessionService,
) : DeviceSessionControllerDocs {

    override fun list(
        principal: PrincipalDetails,
    ): ResponseEntity<ApiResponse<DeviceSessionListResponse>> {
        val userId = requireNotNull(principal.getUser().id)
        val response = authSessionService.list(userId, principal.sessionId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun revoke(
        principal: PrincipalDetails,
        sessionId: UUID,
    ): ResponseEntity<ApiResponse<Unit>> {
        val userId = requireNotNull(principal.getUser().id)
        authSessionService.revoke(userId, sessionId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }
}
