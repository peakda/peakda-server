package com.peakda.server.domain.curation.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.curation.application.CurationImageUploader
import com.peakda.server.domain.curation.presentation.response.UploadedImageResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/admin/curations/images")
class CurationImageAdminController(
    private val curationImageUploader: CurationImageUploader,
) : CurationImageAdminControllerDocs {

    override fun upload(file: MultipartFile): ResponseEntity<ApiResponse<UploadedImageResponse>> {
        val uploaded = curationImageUploader.upload(file)
        val response = UploadedImageResponse(
            objectKey = uploaded.objectKey,
            previewUrl = uploaded.previewUrl,
        )
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }
}
