package com.peakda.server.domain.user.presentation

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.user.application.FollowService
import com.peakda.server.domain.user.presentation.response.FollowSummaryResponse
import com.peakda.server.domain.user.presentation.response.FollowUserResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class FollowController(
    private val followService: FollowService,
) : FollowControllerDocs {

    override fun follow(
        principal: PrincipalDetails,
        userId: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        val followerId = requireNotNull(principal.getUser().id)
        followService.follow(followerId, userId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }

    override fun unfollow(
        principal: PrincipalDetails,
        userId: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        val followerId = requireNotNull(principal.getUser().id)
        followService.unfollow(followerId, userId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }

    override fun followers(
        principal: PrincipalDetails,
        userId: Long,
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<FollowUserResponse>>> {
        val currentUserId = requireNotNull(principal.getUser().id)
        val response = followService.getFollowers(userId, currentUserId, pageRequest)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun followings(
        principal: PrincipalDetails,
        userId: Long,
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<FollowUserResponse>>> {
        val currentUserId = requireNotNull(principal.getUser().id)
        val response = followService.getFollowings(userId, currentUserId, pageRequest)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun summary(
        principal: PrincipalDetails,
        userId: Long,
    ): ResponseEntity<ApiResponse<FollowSummaryResponse>> {
        val currentUserId = requireNotNull(principal.getUser().id)
        val response = followService.getSummary(userId, currentUserId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }
}
