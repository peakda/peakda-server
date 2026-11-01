package com.peakda.server.domain.festival.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.festival.application.FestivalAdminService
import com.peakda.server.domain.festival.application.toCommand
import com.peakda.server.domain.festival.presentation.request.SaveFestivalRequest
import com.peakda.server.domain.festival.presentation.response.FestivalAdminDetailResponse
import com.peakda.server.domain.festival.presentation.response.FestivalIdResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.ZoneId

@RestController
@RequestMapping("/api/admin/festivals")
class FestivalAdminController(
    private val festivalAdminService: FestivalAdminService,
) : FestivalAdminControllerDocs {

    override fun detail(festivalId: Long): ResponseEntity<ApiResponse<FestivalAdminDetailResponse>> {
        val response = festivalAdminService.detail(festivalId, LocalDate.now(KST))
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun create(
        principal: PrincipalDetails,
        request: SaveFestivalRequest,
    ): ResponseEntity<ApiResponse<FestivalIdResponse>> {
        val adminId = requireNotNull(principal.getUser().id)
        val festivalId = festivalAdminService.create(adminId, request.toCommand())
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(HttpStatus.CREATED, FestivalIdResponse(festivalId)))
    }

    override fun update(
        principal: PrincipalDetails,
        festivalId: Long,
        request: SaveFestivalRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        festivalAdminService.update(requireNotNull(principal.getUser().id), festivalId, request.toCommand())
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }

    override fun delete(principal: PrincipalDetails, festivalId: Long): ResponseEntity<ApiResponse<Unit>> {
        festivalAdminService.delete(requireNotNull(principal.getUser().id), festivalId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }

    companion object {
        private val KST = ZoneId.of("Asia/Seoul")
    }
}
