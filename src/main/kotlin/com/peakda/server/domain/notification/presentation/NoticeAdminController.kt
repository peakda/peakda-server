package com.peakda.server.domain.notification.presentation

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.page.toPageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.notification.application.NoticeAdminService
import com.peakda.server.domain.notification.entity.NoticeStatus
import com.peakda.server.domain.notification.presentation.request.UpsertNoticeRequest
import com.peakda.server.domain.notification.presentation.response.NoticeResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/notices")
class NoticeAdminController(
    private val noticeAdminService: NoticeAdminService,
) : NoticeAdminControllerDocs {

    override fun create(
        principal: PrincipalDetails,
        request: UpsertNoticeRequest,
    ): ResponseEntity<ApiResponse<NoticeResponse>> {
        val adminId = requireNotNull(principal.getUser().id)
        val response = noticeAdminService.create(adminId, request.toCommand())
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(HttpStatus.CREATED, response))
    }

    override fun list(
        status: NoticeStatus?,
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<NoticeResponse>>> {
        val response = noticeAdminService.list(status, pageRequest.toPageable()).toPageResponse()
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun get(id: Long): ResponseEntity<ApiResponse<NoticeResponse>> {
        val response = noticeAdminService.get(id)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun update(
        principal: PrincipalDetails,
        id: Long,
        request: UpsertNoticeRequest,
    ): ResponseEntity<ApiResponse<NoticeResponse>> {
        val adminId = requireNotNull(principal.getUser().id)
        val response = noticeAdminService.update(adminId, id, request.toCommand())
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun dispatch(
        principal: PrincipalDetails,
        id: Long,
    ): ResponseEntity<ApiResponse<NoticeResponse>> {
        val adminId = requireNotNull(principal.getUser().id)
        val response = noticeAdminService.dispatch(adminId, id)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun cancel(
        principal: PrincipalDetails,
        id: Long,
    ): ResponseEntity<ApiResponse<NoticeResponse>> {
        val adminId = requireNotNull(principal.getUser().id)
        val response = noticeAdminService.cancel(adminId, id)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }
}
