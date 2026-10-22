package com.peakda.server.domain.launchalert.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.launchalert.application.LaunchAlertService
import com.peakda.server.domain.launchalert.application.SubscribeLaunchAlertCommand
import com.peakda.server.domain.launchalert.presentation.request.SubscribeLaunchAlertRequest
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/launch-alerts")
class LaunchAlertController(
    private val launchAlertService: LaunchAlertService,
) : LaunchAlertControllerDocs {

    override fun subscribe(
        request: SubscribeLaunchAlertRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        launchAlertService.subscribe(
            SubscribeLaunchAlertCommand(
                platform = request.platform,
                email = request.email,
                privacyConsent = request.privacyConsent,
                // forward-headers-strategy=framework 라 프록시 뒤에서도 X-Forwarded-For 의 클라이언트 IP 다.
                clientIp = httpRequest.remoteAddr,
            ),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.CREATED))
    }
}
