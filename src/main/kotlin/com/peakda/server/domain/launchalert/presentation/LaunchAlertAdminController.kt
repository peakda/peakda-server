package com.peakda.server.domain.launchalert.presentation

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.page.toPageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.launchalert.application.LaunchAlertAdminService
import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import com.peakda.server.domain.launchalert.presentation.request.RequestLaunchAlertDispatchRequest
import com.peakda.server.domain.launchalert.presentation.request.SendLaunchAlertTestMailRequest
import com.peakda.server.domain.launchalert.presentation.response.LaunchAlertDispatchResponse
import com.peakda.server.domain.launchalert.presentation.response.LaunchAlertSubscriptionResponse
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/launch-alerts")
class LaunchAlertAdminController(
    private val launchAlertAdminService: LaunchAlertAdminService,
) : LaunchAlertAdminControllerDocs {

    override fun list(
        platform: LaunchAlertPlatform,
        email: String?,
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<LaunchAlertSubscriptionResponse>>> {
        val response = launchAlertAdminService
            .list(platform, email, pageRequest.toPageable(Sort.by(Sort.Direction.DESC, "id")))
            .toPageResponse()
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun delete(principal: PrincipalDetails, id: Long): ResponseEntity<ApiResponse<Unit>> {
        launchAlertAdminService.delete(adminId(principal), id)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }

    override fun dispatches(
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<LaunchAlertDispatchResponse>>> {
        val response = launchAlertAdminService
            .dispatches(pageRequest.toPageable(Sort.by(Sort.Direction.DESC, "id")))
            .toPageResponse()
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun requestDispatch(
        principal: PrincipalDetails,
        request: RequestLaunchAlertDispatchRequest,
    ): ResponseEntity<ApiResponse<LaunchAlertDispatchResponse>> {
        val response = launchAlertAdminService.requestDispatch(adminId(principal), request.toCommand())
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.CREATED, response))
    }

    override fun cancelDispatch(
        principal: PrincipalDetails,
        id: Long,
    ): ResponseEntity<ApiResponse<LaunchAlertDispatchResponse>> {
        val response = launchAlertAdminService.cancelDispatch(adminId(principal), id)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun sendTestMail(
        principal: PrincipalDetails,
        request: SendLaunchAlertTestMailRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        launchAlertAdminService.sendTestMail(adminId(principal), request.toCommand())
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }

    private fun adminId(principal: PrincipalDetails): Long = requireNotNull(principal.getUser().id)
}
