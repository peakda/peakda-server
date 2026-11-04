package com.peakda.server.domain.feed.presentation.v2

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.feed.application.FeedReactionService
import com.peakda.server.domain.feed.application.FeedService
import com.peakda.server.domain.feed.entity.FeedFilter
import com.peakda.server.domain.feed.presentation.v2.response.FeedRecordResponse
import com.peakda.server.domain.feed.presentation.v2.response.FeedRecordSummaryResponse
import com.peakda.server.domain.feed.presentation.v2.response.ReactionPageResponse
import com.peakda.server.domain.feed.presentation.v2.response.ReactionStateResponse
import com.peakda.server.domain.feed.presentation.v2.response.toResponse
import com.peakda.server.domain.feed.presentation.v2.response.toFeedRecordResponse
import com.peakda.server.domain.feed.presentation.v2.response.toFeedRecordSummaryResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v2/feed")
class FeedV2Controller(
    private val feedService: FeedService,
    private val feedReactionService: FeedReactionService,
) : FeedV2ControllerDocs {
    override fun list(principal: PrincipalDetails?, filter: FeedFilter, pageRequest: PageRequest): ResponseEntity<ApiResponse<PageResponse<FeedRecordSummaryResponse>>> {
        val userId = principal?.getUser()?.id
        val page = feedService.list(userId, filter, pageRequest)
        val reactions = feedReactionService.previews(page.content.map { it.id }, userId)
        val response = PageResponse(
            content = page.content.map { it.toFeedRecordSummaryResponse(reactions.getValue(it.id)) },
            page = page.page, size = page.size, totalElements = page.totalElements,
            totalPages = page.totalPages, hasNext = page.hasNext,
        )
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun get(principal: PrincipalDetails?, id: Long): ResponseEntity<ApiResponse<FeedRecordResponse>> {
        val userId = principal?.getUser()?.id
        val record = feedService.detail(id, userId)
        val reactions = feedReactionService.previews(listOf(id), userId).getValue(id)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, record.toFeedRecordResponse(reactions)))
    }

    override fun addReaction(principal: PrincipalDetails, id: Long, emojiKey: String): ResponseEntity<ApiResponse<ReactionStateResponse>> {
        val state = feedReactionService.add(requireNotNull(principal.getUser().id), id, emojiKey)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, state.toResponse()))
    }

    override fun removeReaction(principal: PrincipalDetails, id: Long, emojiKey: String): ResponseEntity<ApiResponse<ReactionStateResponse>> {
        val state = feedReactionService.remove(requireNotNull(principal.getUser().id), id, emojiKey)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, state.toResponse()))
    }

    override fun reactions(principal: PrincipalDetails?, id: Long, afterKey: String?, size: Int, mineOnly: Boolean): ResponseEntity<ApiResponse<ReactionPageResponse>> {
        val page = feedReactionService.list(id, principal?.getUser()?.id, afterKey, size, mineOnly)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, page.toResponse()))
    }
}
