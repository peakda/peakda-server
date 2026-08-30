package com.peakda.server.domain.auth.application

import com.peakda.server.common.exception.AuthorizationException
import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.image.ImageException
import com.peakda.server.common.image.ImageResizer
import com.peakda.server.common.security.cookie.CookieProperties
import com.peakda.server.common.security.cookie.CookieUtils
import com.peakda.server.common.security.jwt.JwtProperties
import com.peakda.server.common.security.principal.SignupSessionPrincipal
import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.common.storage.ObjectKeyUrlResolver
import com.peakda.server.domain.auth.presentation.response.UserInfoResponse
import com.peakda.server.domain.auth.signup.application.SignupProfileImagePolicy
import com.peakda.server.domain.auth.signup.presentation.request.SignupCompleteRequest
import com.peakda.server.domain.auth.signup.presentation.response.NicknameCheckResponse
import com.peakda.server.domain.auth.signup.repository.SignupSessionRepository
import com.peakda.server.domain.user.application.ProfileImagePolicy
import com.peakda.server.domain.user.application.UserFavoriteCategoryService
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.presentation.response.ProfileImageResponse
import com.peakda.server.domain.user.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val cookieProperties: CookieProperties,
    private val jwtProperties: JwtProperties,
    private val refreshTokenService: RefreshTokenService,
    private val tokenIssueService: TokenIssueService,
    private val signupSessionRepository: SignupSessionRepository,
    private val imageResizer: ImageResizer,
    private val objectStorage: ObjectStorage,
    private val objectKeyUrlResolver: ObjectKeyUrlResolver,
    private val userFavoriteCategoryService: UserFavoriteCategoryService,
) {

    companion object {
        private val NICKNAME_REGEX = Regex("^[가-힣a-zA-Z0-9]{2,10}$")
    }

    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional(readOnly = true)
    fun getUserInfo(userId: Long): UserInfoResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { AuthorizationException(ErrorCode.RESOURCE_NOT_FOUND) }

        return UserInfoResponse.from(
            user = user,
            profileImageUrl = objectKeyUrlResolver.resolve(user.profileImageUrl),
            favoriteCategories = userFavoriteCategoryService.findCategories(userId),
        )
    }

    @Transactional(readOnly = true)
    fun checkNickname(value: String): NicknameCheckResponse {
        validateNickname(value)
        return NicknameCheckResponse(available = !userRepository.existsByNickname(value))
    }

    @Transactional(readOnly = true)
    fun uploadSignupProfileImage(
        principal: SignupSessionPrincipal,
        file: MultipartFile,
    ): ProfileImageResponse {
        ProfileImagePolicy.validate(file)
        val sessionId = requireNotNull(principal.getSignupSession().id) { "signup session id must exist" }

        val resized = imageResizer.resize(file.bytes, ProfileImagePolicy.VARIANTS)
        val variantKeys = resized.associate { result ->
            val key = SignupProfileImagePolicy.keyOf(sessionId, result.variant)
            objectStorage.upload(key, result.bytes, result.variant.format.mimeType)
            result.variant.name to key
        }
        val mainKey = variantKeys[ProfileImagePolicy.MAIN_VARIANT]
            ?: throw ImageException(ErrorCode.IMAGE_PROCESSING_FAILED)
        val variantUrls = variantKeys.mapValues { (_, key) -> objectStorage.presignedGetUrl(key) }
        val mainUrl = requireNotNull(variantUrls[ProfileImagePolicy.MAIN_VARIANT])
        return ProfileImageResponse(
            profileImageUrl = mainUrl,
            profileImageKey = mainKey,
            variants = variantUrls,
        )
    }

    @Transactional
    fun completeSignup(
        principal: SignupSessionPrincipal,
        request: SignupCompleteRequest,
        response: HttpServletResponse,
    ) {
        validateNickname(request.nickname)
        if (userRepository.existsByNickname(request.nickname)) {
            throw AuthException(ErrorCode.NICKNAME_DUPLICATED)
        }

        val signupSession = principal.getSignupSession()
        val sessionId = requireNotNull(signupSession.id) { "signup session id must exist" }
        val initialImageValue = request.profileImageUrl ?: signupSession.profileImageUrl

        val user = userRepository.save(
            User.create(
                provider = signupSession.provider,
                providerId = signupSession.providerId,
                nickname = request.nickname,
                email = signupSession.email,
                profileImageUrl = initialImageValue,
            )
        )
        val userId = requireNotNull(user.id)

        userFavoriteCategoryService.replace(userId, request.favoriteCategories)

        promoteSignupProfileImageIfManaged(sessionId, userId, user, initialImageValue)

        signupSessionRepository.delete(signupSession)

        val tokenResponse = tokenIssueService.issue(user)

        val signupTokenCookie = CookieUtils.deleteSignupTokenCookie(cookieProperties)
        val accessTokenCookie = CookieUtils.createAccessTokenCookie(
            token = tokenResponse.accessToken,
            maxAge = jwtProperties.accessTokenValidityInSeconds,
            properties = cookieProperties,
        )
        val refreshTokenCookie = CookieUtils.createRefreshTokenCookie(
            token = tokenResponse.refreshToken,
            maxAge = jwtProperties.refreshTokenValidityInSeconds,
            properties = cookieProperties,
        )

        response.addHeader("Set-Cookie", signupTokenCookie.toString())
        response.addHeader("Set-Cookie", accessTokenCookie.toString())
        response.addHeader("Set-Cookie", refreshTokenCookie.toString())
    }

    /** 쿠키의 Refresh Token 으로 새 토큰 쌍을 발급해 다시 쿠키로 내린다. */
    fun refresh(request: HttpServletRequest, response: HttpServletResponse) {
        val refreshToken = CookieUtils.getRefreshTokenFromCookies(request.cookies, cookieProperties)
            ?: throw AuthorizationException(ErrorCode.REFRESH_TOKEN_EXPIRED)

        val tokenResponse = tokenIssueService.rotate(refreshToken)

        val accessTokenCookie = CookieUtils.createAccessTokenCookie(
            token = tokenResponse.accessToken,
            maxAge = jwtProperties.accessTokenValidityInSeconds,
            properties = cookieProperties
        )
        val refreshTokenCookie = CookieUtils.createRefreshTokenCookie(
            token = tokenResponse.refreshToken,
            maxAge = jwtProperties.refreshTokenValidityInSeconds,
            properties = cookieProperties
        )

        response.addHeader("Set-Cookie", accessTokenCookie.toString())
        response.addHeader("Set-Cookie", refreshTokenCookie.toString())
    }

    fun logout(userId: Long, response: HttpServletResponse) {
        // Redis에서 Refresh Token 삭제
        refreshTokenService.deleteRefreshToken(userId)

        val accessTokenCookie = CookieUtils.deleteAccessTokenCookie(cookieProperties)
        val refreshTokenCookie = CookieUtils.deleteRefreshTokenCookie(cookieProperties)

        response.addHeader("Set-Cookie", accessTokenCookie.toString())
        response.addHeader("Set-Cookie", refreshTokenCookie.toString())

        SecurityContextHolder.clearContext()
    }

    private fun validateNickname(nickname: String) {
        if (!NICKNAME_REGEX.matches(nickname)) {
            throw AuthException(ErrorCode.NICKNAME_INVALID)
        }
    }

    private fun promoteSignupProfileImageIfManaged(
        sessionId: Long,
        userId: Long,
        user: User,
        initialImageValue: String?,
    ) {
        if (initialImageValue.isNullOrBlank()) return
        if (!SignupProfileImagePolicy.isManaged(sessionId, initialImageValue)) return

        var mainKey: String? = null
        ProfileImagePolicy.VARIANTS.forEach { variant ->
            val sourceKey = SignupProfileImagePolicy.keyOf(sessionId, variant)
            val destinationKey = ProfileImagePolicy.keyOf(userId, variant)
            objectStorage.copy(sourceKey, destinationKey)
            if (variant.name == ProfileImagePolicy.MAIN_VARIANT) {
                mainKey = destinationKey
            }
        }
        user.profileImageUrl = mainKey

        ProfileImagePolicy.VARIANTS.forEach { variant ->
            val tempKey = SignupProfileImagePolicy.keyOf(sessionId, variant)
            runCatching { objectStorage.delete(tempKey) }
                .onFailure { log.warn("가입 임시 프로필 이미지 삭제 실패 key={}", tempKey, it) }
        }
    }
}
