package com.peakda.server.domain.user.presentation

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.page.toPageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.user.application.UserAdminService
import com.peakda.server.domain.user.entity.UserRole
import com.peakda.server.domain.user.entity.UserStatus
import com.peakda.server.domain.user.presentation.request.ChangeUserStatusRequest
import com.peakda.server.domain.user.presentation.response.UserAdminResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/users")
class UserAdminController(
    private val userAdminService: UserAdminService,
) : UserAdminControllerDocs {

    override fun list(
        q: String?,
        status: UserStatus?,
        role: UserRole?,
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<UserAdminResponse>>> {
        val response = userAdminService
            .list(q, status, role, pageRequest.toPageable())
            .toPageResponse()
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun changeStatus(
        principal: PrincipalDetails,
        id: Long,
        request: ChangeUserStatusRequest,
    ): ResponseEntity<ApiResponse<UserAdminResponse>> {
        val adminId = requireNotNull(principal.getUser().id)
        val response = userAdminService.changeStatus(adminId, id, request.toCommand())
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }
}
