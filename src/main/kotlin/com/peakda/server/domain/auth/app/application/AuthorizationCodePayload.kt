package com.peakda.server.domain.auth.app.application

/**
 * 일회성 코드에 담기는 인증 결과.
 * 가입까지 끝난 사용자면 [Authenticated], 추가 정보 입력이 남았으면 [SignupRequired] 다.
 */
sealed interface AuthorizationCodePayload {

    data class Authenticated(val userId: Long) : AuthorizationCodePayload

    data class SignupRequired(val signupToken: String) : AuthorizationCodePayload

    fun serialize(): String = when (this) {
        is Authenticated -> "$USER_PREFIX$userId"
        is SignupRequired -> "$SIGNUP_PREFIX$signupToken"
    }

    companion object {
        private const val USER_PREFIX = "user:"
        private const val SIGNUP_PREFIX = "signup:"

        fun parse(value: String): AuthorizationCodePayload? = when {
            value.startsWith(USER_PREFIX) -> value.removePrefix(USER_PREFIX).toLongOrNull()?.let(::Authenticated)
            value.startsWith(SIGNUP_PREFIX) -> SignupRequired(value.removePrefix(SIGNUP_PREFIX))
            else -> null
        }
    }
}
