package com.peakda.server.domain.notification.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.notification.application.DeviceTokenService
import com.peakda.server.domain.notification.presentation.request.RegisterDeviceRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/devices")
class DeviceController(
    private val deviceTokenService: DeviceTokenService,
) : DeviceControllerDocs {

    override fun register(
        principal: PrincipalDetails,
        request: RegisterDeviceRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        val userId = requireNotNull(principal.getUser().id)
        deviceTokenService.register(userId, request.token, request.platform)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }

    override fun unregister(
        principal: PrincipalDetails,
        token: String,
    ): ResponseEntity<ApiResponse<Unit>> {
        val userId = requireNotNull(principal.getUser().id)
        deviceTokenService.unregister(userId, token)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }
}
