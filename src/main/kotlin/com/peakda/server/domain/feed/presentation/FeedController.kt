package com.peakda.server.domain.feed.presentation

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.feed.application.FeedReactionService
import com.peakda.server.domain.feed.application.FeedService
import com.peakda.server.domain.feed.entity.FeedFilter
import com.peakda.server.domain.feed.presentation.response.FeedReactionSummaryResponse
import com.peakda.server.domain.spot.entity.ReactionType
import com.peakda.server.domain.spot.presentation.response.SpotRecordResponse
import com.peakda.server.domain.spot.presentation.response.SpotRecordSummaryResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/feed")
class FeedController(
    private val feedService: FeedService,
    private val feedReactionService: FeedReactionService,
) : FeedControllerDocs {

    override fun list(
        principal: PrincipalDetails,
        filter: FeedFilter,
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<SpotRecordSummaryResponse>>> {
        val userId = requireNotNull(principal.getUser().id)
        val response = feedService.list(userId, filter, pageRequest)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun get(
        principal: PrincipalDetails,
        id: Long,
    ): ResponseEntity<ApiResponse<SpotRecordResponse>> {
        val response = feedService.detail(id)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun addReaction(
        principal: PrincipalDetails,
        id: Long,
        reactionType: ReactionType,
    ): ResponseEntity<ApiResponse<FeedReactionSummaryResponse>> {
        val userId = requireNotNull(principal.getUser().id)
        val response = feedReactionService.add(userId, id, reactionType)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun removeReaction(
        principal: PrincipalDetails,
        id: Long,
        reactionType: ReactionType,
    ): ResponseEntity<ApiResponse<FeedReactionSummaryResponse>> {
        val userId = requireNotNull(principal.getUser().id)
        val response = feedReactionService.remove(userId, id, reactionType)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }
}
