package com.peakda.server.domain.user.application

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.image.ImageException
import com.peakda.server.common.image.ImageResizer
import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.common.storage.ObjectKeyUrlResolver
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
    private val objectKeyUrlResolver: ObjectKeyUrlResolver,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun uploadProfileImage(userId: Long, file: MultipartFile): ProfileImageResponse {
        ProfileImagePolicy.validate(file)

        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        val previousKey = user.profileImageUrl

        val resized = imageResizer.resize(file.bytes, ProfileImagePolicy.VARIANTS)
        val variantKeys = resized.associate { result ->
            val key = ProfileImagePolicy.keyOf(userId, result.variant)
            objectStorage.upload(key, result.bytes, result.variant.format.mimeType)
            result.variant.name to key
        }

        val mainKey = variantKeys[ProfileImagePolicy.MAIN_VARIANT]
            ?: throw ImageException(ErrorCode.IMAGE_PROCESSING_FAILED)
        user.profileImageUrl = mainKey

        if (!previousKey.isNullOrBlank() && previousKey != mainKey) {
            deleteManaged(userId, previousKey)
        }

        val variantUrls = variantKeys.mapValues { (_, key) -> objectStorage.presignedGetUrl(key) }
        val mainUrl = requireNotNull(variantUrls[ProfileImagePolicy.MAIN_VARIANT])
        return ProfileImageResponse(
            profileImageUrl = mainUrl,
            profileImageKey = mainKey,
            variants = variantUrls,
        )
    }

    @Transactional
    fun deleteProfileImage(userId: Long) {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        val currentKey = user.profileImageUrl ?: return

        user.profileImageUrl = null
        deleteManaged(userId, currentKey)
    }

    private fun deleteManaged(userId: Long, currentKey: String) {
        val managed = ProfileImagePolicy.VARIANTS.any { currentKey == ProfileImagePolicy.keyOf(userId, it) }
        if (!managed) return
        ProfileImagePolicy.VARIANTS.forEach { variant ->
            val key = ProfileImagePolicy.keyOf(userId, variant)
            runCatching { objectStorage.delete(key) }
                .onFailure { log.warn("프로필 이미지 삭제 실패 key={}", key, it) }
        }
    }
}
