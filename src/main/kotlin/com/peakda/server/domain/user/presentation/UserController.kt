package com.peakda.server.domain.user.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.user.application.UserFavoriteCategoryService
import com.peakda.server.domain.user.application.UserService
import com.peakda.server.domain.user.presentation.request.FavoriteCategoryUpdateRequest
import com.peakda.server.domain.user.presentation.response.FavoriteCategoryResponse
import com.peakda.server.domain.user.presentation.response.ProfileImageResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
    private val userFavoriteCategoryService: UserFavoriteCategoryService,
) : UserControllerDocs {

    override fun uploadProfileImage(
        principal: PrincipalDetails,
        image: MultipartFile,
    ): ResponseEntity<ApiResponse<ProfileImageResponse>> {
        val userId = requireNotNull(principal.getUser().id)
        val response = userService.uploadProfileImage(userId, image)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun deleteProfileImage(
        principal: PrincipalDetails,
    ): ResponseEntity<ApiResponse<Unit>> {
        val userId = requireNotNull(principal.getUser().id)
        userService.deleteProfileImage(userId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }

    override fun updateFavoriteCategories(
        principal: PrincipalDetails,
        request: FavoriteCategoryUpdateRequest,
    ): ResponseEntity<ApiResponse<FavoriteCategoryResponse>> {
        val userId = requireNotNull(principal.getUser().id)
        val categories = userFavoriteCategoryService.replace(userId, request.categories)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, FavoriteCategoryResponse.of(categories)))
    }
}
