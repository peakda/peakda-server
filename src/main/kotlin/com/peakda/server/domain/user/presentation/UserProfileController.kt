package com.peakda.server.domain.user.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.user.application.UserProfileService
import com.peakda.server.domain.user.presentation.response.UserProfileResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserProfileController(
    private val userProfileService: UserProfileService,
) : UserProfileControllerDocs {

    override fun getProfile(
        principal: PrincipalDetails,
        id: Long,
    ): ResponseEntity<ApiResponse<UserProfileResponse>> {
        val currentUserId = requireNotNull(principal.getUser().id)
        val response = userProfileService.getProfile(id, currentUserId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }
}
