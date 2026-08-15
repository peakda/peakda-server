package com.peakda.server.domain.spot.presentation

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.spot.application.CreateSpotRecordCommand
import com.peakda.server.domain.spot.application.SpotRecordPhotoUploader
import com.peakda.server.domain.spot.application.SpotRecordService
import com.peakda.server.domain.spot.application.SpotResolveInput
import com.peakda.server.domain.spot.application.UpdateSpotRecordCommand
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.presentation.request.CreateSpotRecordRequest
import com.peakda.server.domain.spot.presentation.request.SpotInputRequest
import com.peakda.server.domain.spot.presentation.request.UpdateSpotRecordRequest
import com.peakda.server.domain.spot.presentation.response.SpotRecordPhotoUploadResponse
import com.peakda.server.domain.spot.presentation.response.SpotRecordPhotoUploadResponse.UploadedSpotRecordPhoto
import com.peakda.server.domain.spot.presentation.response.SpotRecordResponse
import com.peakda.server.domain.spot.presentation.response.SpotRecordSummaryResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/spots/records")
class SpotRecordController(
    private val spotRecordPhotoUploader: SpotRecordPhotoUploader,
    private val spotRecordService: SpotRecordService,
) : SpotRecordControllerDocs {

    override fun uploadPhotos(
        principal: PrincipalDetails,
        images: List<MultipartFile>,
    ): ResponseEntity<ApiResponse<SpotRecordPhotoUploadResponse>> {
        val userId = requireNotNull(principal.getUser().id)
        val uploaded = spotRecordPhotoUploader.upload(userId, images)
        val response = SpotRecordPhotoUploadResponse(
            photos = uploaded.map { UploadedSpotRecordPhoto(objectKey = it.objectKey, previewUrl = it.previewUrl) },
        )
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun create(
        principal: PrincipalDetails,
        request: CreateSpotRecordRequest,
    ): ResponseEntity<ApiResponse<SpotRecordResponse>> {
        val userId = requireNotNull(principal.getUser().id)
        val command = CreateSpotRecordCommand(
            userId = userId,
            spot = request.spotInput.toResolveInput(userId),
            visitedDate = request.visitedDate,
            bloomStage = request.bloomStage,
            memo = request.memo,
            plantIds = request.plantIds,
            photoKeys = request.photoKeys,
            status = request.status,
        )
        val response = spotRecordService.create(command)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun update(
        principal: PrincipalDetails,
        id: Long,
        request: UpdateSpotRecordRequest,
    ): ResponseEntity<ApiResponse<SpotRecordResponse>> {
        val userId = requireNotNull(principal.getUser().id)
        val command = UpdateSpotRecordCommand(
            recordId = id,
            userId = userId,
            visitedDate = request.visitedDate,
            bloomStage = request.bloomStage,
            memo = request.memo,
            plantIds = request.plantIds,
            photoKeys = request.photoKeys,
        )
        val response = spotRecordService.update(command)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun publish(
        principal: PrincipalDetails,
        id: Long,
    ): ResponseEntity<ApiResponse<SpotRecordResponse>> {
        val userId = requireNotNull(principal.getUser().id)
        val response = spotRecordService.publish(id, userId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun delete(
        principal: PrincipalDetails,
        id: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        val userId = requireNotNull(principal.getUser().id)
        spotRecordService.delete(id, userId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }

    override fun get(
        principal: PrincipalDetails,
        id: Long,
    ): ResponseEntity<ApiResponse<SpotRecordResponse>> {
        val userId = requireNotNull(principal.getUser().id)
        val response = spotRecordService.get(id, userId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun listBySpot(
        principal: PrincipalDetails,
        spotId: Long,
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<SpotRecordSummaryResponse>>> {
        val userId = requireNotNull(principal.getUser().id)
        val response = spotRecordService.listBySpot(spotId, userId, pageRequest)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun listMine(
        principal: PrincipalDetails,
        status: SpotRecordStatus,
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<SpotRecordSummaryResponse>>> {
        val userId = requireNotNull(principal.getUser().id)
        val response = spotRecordService.listMine(userId, status, pageRequest)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    private fun SpotInputRequest.toResolveInput(userId: Long) = SpotResolveInput(
        existingSpotId = existingSpotId,
        type = type,
        attractionId = attractionId,
        name = name,
        address = address,
        latitude = latitude,
        longitude = longitude,
        kakaoPlaceId = kakaoPlaceId,
        userId = userId,
    )
}
