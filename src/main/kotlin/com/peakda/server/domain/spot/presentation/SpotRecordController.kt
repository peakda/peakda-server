package com.peakda.server.domain.spot.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.spot.application.SpotRecordPhotoUploader
import com.peakda.server.domain.spot.presentation.response.SpotRecordPhotoUploadResponse
import com.peakda.server.domain.spot.presentation.response.SpotRecordPhotoUploadResponse.UploadedSpotRecordPhoto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/spot-records")
class SpotRecordController(
    private val spotRecordPhotoUploader: SpotRecordPhotoUploader,
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
}
