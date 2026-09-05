package com.peakda.server.domain.user.presentation

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.user.application.BlockService
import com.peakda.server.domain.user.presentation.response.BlockedUserResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class BlockController(
    private val blockService: BlockService,
) : BlockControllerDocs {

    override fun block(
        principal: PrincipalDetails,
        userId: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        val blockerId = requireNotNull(principal.getUser().id)
        blockService.block(blockerId, userId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }

    override fun unblock(
        principal: PrincipalDetails,
        userId: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        val blockerId = requireNotNull(principal.getUser().id)
        blockService.unblock(blockerId, userId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }

    override fun list(
        principal: PrincipalDetails,
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<BlockedUserResponse>>> {
        val blockerId = requireNotNull(principal.getUser().id)
        val response = blockService.list(blockerId, pageRequest)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }
}
