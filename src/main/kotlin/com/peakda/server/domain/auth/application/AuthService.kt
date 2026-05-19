package com.peakda.server.domain.auth.application

import com.peakda.server.common.exception.AuthorizationException
import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.image.ImageException
import com.peakda.server.common.image.ImageResizer
import com.peakda.server.common.security.cookie.CookieProperties
import com.peakda.server.common.security.cookie.CookieUtils
import com.peakda.server.common.security.jwt.JwtProperties
import com.peakda.server.common.security.jwt.JwtTokenGenerator
import com.peakda.server.common.security.jwt.JwtTokenProvider
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.common.security.principal.SignupSessionPrincipal
import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.domain.auth.presentation.response.UserInfoResponse
import com.peakda.server.domain.auth.signup.application.SignupProfileImagePolicy
import com.peakda.server.domain.auth.signup.presentation.request.SignupCompleteRequest
import com.peakda.server.domain.auth.signup.presentation.response.NicknameCheckResponse
import com.peakda.server.domain.auth.signup.repository.SignupSessionRepository
import com.peakda.server.domain.user.application.ProfileImagePolicy
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
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtTokenGenerator: JwtTokenGenerator,
    private val refreshTokenService: RefreshTokenService,
    private val signupSessionRepository: SignupSessionRepository,
    private val imageResizer: ImageResizer,
    private val objectStorage: ObjectStorage,
) {

    companion object {
        private val NICKNAME_REGEX = Regex("^[가-힣a-zA-Z0-9]{2,10}$")
    }

    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional(readOnly = true)
    fun getUserInfo(userId: Long): UserInfoResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { AuthorizationException(ErrorCode.RESOURCE_NOT_FOUND) }

        return UserInfoResponse.from(user)
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
        val variantUrls = resized.associate { result ->
            val key = SignupProfileImagePolicy.keyOf(sessionId, result.variant)
            val url = objectStorage.upload(key, result.bytes, result.variant.format.mimeType)
            result.variant.name to url
        }
        val mainUrl = variantUrls[ProfileImagePolicy.MAIN_VARIANT]
            ?: throw ImageException(ErrorCode.IMAGE_PROCESSING_FAILED)
        return ProfileImageResponse(profileImageUrl = mainUrl, variants = variantUrls)
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
        val initialImageUrl = request.profileImageUrl ?: signupSession.profileImageUrl

        val user = userRepository.save(
            User.create(
                provider = signupSession.provider,
                providerId = signupSession.providerId,
                nickname = request.nickname,
                email = signupSession.email,
                profileImageUrl = initialImageUrl,
            )
        )
        val userId = requireNotNull(user.id)

        promoteSignupProfileImageIfManaged(sessionId, userId, user, initialImageUrl)

        signupSessionRepository.delete(signupSession)

        val tokenResponse = jwtTokenGenerator.generateToken(
            userId = requireNotNull(user.id),
            email = user.email,
            authorities = listOf("${PrincipalDetails.ROLE_PREFIX}${user.role.name}"),
        )
        refreshTokenService.saveRefreshToken(user.id!!, tokenResponse.refreshToken)

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

    /**
     * Refresh Token으로 새 토큰 쌍 발급 (Refresh Token Rotation)
     * Redis에 저장된 토큰과 비교하여 탈취 감지
     */
    fun refresh(request: HttpServletRequest, response: HttpServletResponse) {
        // 1. 쿠키에서 refreshToken 추출
        val refreshToken = CookieUtils.getRefreshTokenFromCookies(request.cookies, cookieProperties)
            ?: throw AuthorizationException(ErrorCode.REFRESH_TOKEN_EXPIRED)

        // 2. JWT 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw AuthorizationException(ErrorCode.REFRESH_TOKEN_EXPIRED)
        }

        // 3. userId 추출
        val userId = jwtTokenProvider.getUserIdFromToken(refreshToken)
            ?: throw AuthorizationException(ErrorCode.REFRESH_TOKEN_INVALID)

        // 4. Redis에 저장된 토큰과 비교 (탈취 감지)
        val storedToken = refreshTokenService.getRefreshToken(userId)
        if (storedToken == null || storedToken != refreshToken) {
            // 불일치 시 탈취 의심 → 해당 userId 토큰 삭제
            refreshTokenService.deleteRefreshToken(userId)
            log.warn("Refresh token 불일치 감지 - 탈취 의심. userId: {}", userId)
            throw AuthorizationException(ErrorCode.REFRESH_TOKEN_INVALID)
        }

        // 5. 사용자 정보 조회
        val user = userRepository.findById(userId)
            .orElseThrow { AuthorizationException(ErrorCode.RESOURCE_NOT_FOUND) }

        // 6. 새 토큰 쌍 발급
        val tokenResponse = jwtTokenGenerator.generateToken(
            userId = user.id!!,
            email = user.email,
            authorities = listOf("${PrincipalDetails.ROLE_PREFIX}${user.role.name}"),
        )

        // 7. Redis에 새 Refresh Token 저장 (Rotation)
        refreshTokenService.rotateRefreshToken(userId, tokenResponse.refreshToken)

        // 8. 새 쿠키로 응답
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
        initialImageUrl: String?,
    ) {
        if (initialImageUrl.isNullOrBlank()) return
        if (!SignupProfileImagePolicy.isManaged(sessionId, initialImageUrl)) return

        var mainUrl: String? = null
        ProfileImagePolicy.VARIANTS.forEach { variant ->
            val sourceKey = SignupProfileImagePolicy.keyOf(sessionId, variant)
            val destinationKey = ProfileImagePolicy.keyOf(userId, variant)
            val url = objectStorage.copy(sourceKey, destinationKey)
            if (variant.name == ProfileImagePolicy.MAIN_VARIANT) {
                mainUrl = url
            }
        }
        user.profileImageUrl = mainUrl

        ProfileImagePolicy.VARIANTS.forEach { variant ->
            val tempKey = SignupProfileImagePolicy.keyOf(sessionId, variant)
            runCatching { objectStorage.delete(tempKey) }
                .onFailure { log.warn("가입 임시 프로필 이미지 삭제 실패 key={}", tempKey, it) }
        }
    }
}
