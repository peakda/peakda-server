package com.peakda.server.domain.admin.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.admin.application.AdminSessionService
import com.peakda.server.domain.admin.presentation.response.AdminSessionResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/session")
class AdminSessionController(
    private val adminSessionService: AdminSessionService,
) : AdminSessionControllerDocs {

    override fun getSession(
        principal: PrincipalDetails,
    ): ResponseEntity<ApiResponse<AdminSessionResponse>> {
        val response = adminSessionService.getSession(principal.getUser())
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }
}
