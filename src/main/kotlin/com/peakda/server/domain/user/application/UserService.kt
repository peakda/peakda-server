package com.peakda.server.domain.user.application

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.image.ImageException
import com.peakda.server.common.image.ImageResizer
import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.domain.user.exception.UserNotFoundException
import com.peakda.server.domain.user.presentation.response.ProfileImageResponse
import com.peakda.server.domain.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class UserService(
    private val userRepository: UserRepository,
    private val imageResizer: ImageResizer,
    private val objectStorage: ObjectStorage,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun uploadProfileImage(userId: Long, file: MultipartFile): ProfileImageResponse {
        validate(file)

        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        val previousUrl = user.profileImageUrl

        val resized = imageResizer.resize(file.bytes, ProfileImagePolicy.VARIANTS)
        val variantUrls = resized.associate { result ->
            val key = ProfileImagePolicy.keyOf(userId, result.variant)
            val url = objectStorage.upload(key, result.bytes, result.variant.format.mimeType)
            result.variant.name to url
        }

        val mainUrl = variantUrls[ProfileImagePolicy.MAIN_VARIANT]
            ?: throw ImageException(ErrorCode.IMAGE_PROCESSING_FAILED)
        user.profileImageUrl = mainUrl

        if (!previousUrl.isNullOrBlank() && previousUrl != mainUrl) {
            deleteManaged(userId, previousUrl)
        }

        return ProfileImageResponse(profileImageUrl = mainUrl, variants = variantUrls)
    }

    @Transactional
    fun deleteProfileImage(userId: Long) {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        val currentUrl = user.profileImageUrl ?: return

        user.profileImageUrl = null
        deleteManaged(userId, currentUrl)
    }

    private fun validate(file: MultipartFile) {
        if (file.isEmpty) {
            throw ImageException(ErrorCode.IMAGE_REQUIRED)
        }
        if (file.size > ProfileImagePolicy.MAX_FILE_SIZE_BYTES) {
            throw ImageException(ErrorCode.IMAGE_SIZE_EXCEEDED)
        }
        val contentType = file.contentType?.lowercase()
        if (contentType !in ProfileImagePolicy.ALLOWED_MIME_TYPES) {
            throw ImageException(ErrorCode.INVALID_IMAGE_FORMAT)
        }
    }

    private fun deleteManaged(userId: Long, currentUrl: String) {
        val managed = ProfileImagePolicy.VARIANTS.any { currentUrl.endsWith(ProfileImagePolicy.keyOf(userId, it)) }
        if (!managed) return
        ProfileImagePolicy.VARIANTS.forEach { variant ->
            val key = ProfileImagePolicy.keyOf(userId, variant)
            runCatching { objectStorage.delete(key) }
                .onFailure { log.warn("프로필 이미지 삭제 실패 key={}", key, it) }
        }
    }
}
