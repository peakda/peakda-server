package com.peakda.server.domain.notification.presentation

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.notification.application.NotificationService
import com.peakda.server.domain.notification.entity.NotificationSegment
import com.peakda.server.domain.notification.presentation.response.NotificationResponse
import com.peakda.server.domain.notification.presentation.response.UnreadCountResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService,
) : NotificationControllerDocs {

    override fun list(
        principal: PrincipalDetails,
        segment: NotificationSegment,
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> {
        val userId = requireNotNull(principal.getUser().id)
        val response = notificationService.list(userId, segment, pageRequest)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun unreadCount(
        principal: PrincipalDetails,
    ): ResponseEntity<ApiResponse<UnreadCountResponse>> {
        val userId = requireNotNull(principal.getUser().id)
        val response = UnreadCountResponse(notificationService.unreadCount(userId))
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun markRead(
        principal: PrincipalDetails,
        id: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        val userId = requireNotNull(principal.getUser().id)
        notificationService.markRead(userId, id)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }

    override fun markAllRead(
        principal: PrincipalDetails,
    ): ResponseEntity<ApiResponse<Unit>> {
        val userId = requireNotNull(principal.getUser().id)
        notificationService.markAllRead(userId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }
}
