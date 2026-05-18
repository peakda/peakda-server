package com.peakda.server.common.security.cookie

import jakarta.servlet.http.Cookie
import org.springframework.http.ResponseCookie

object CookieUtils {

    fun createAccessTokenCookie(token: String, maxAge: Long, properties: CookieProperties): ResponseCookie =
        baseCookie(properties.accessTokenName, token, maxAge, properties)

    fun createRefreshTokenCookie(token: String, maxAge: Long, properties: CookieProperties): ResponseCookie =
        baseCookie(properties.refreshTokenName, token, maxAge, properties)

    fun createSignupTokenCookie(token: String, properties: CookieProperties): ResponseCookie =
        baseCookie(properties.signupTokenName, token, properties.signupTokenValidityInSeconds, properties)

    fun deleteAccessTokenCookie(properties: CookieProperties): ResponseCookie =
        baseCookie(properties.accessTokenName, "", 0, properties)

    fun deleteRefreshTokenCookie(properties: CookieProperties): ResponseCookie =
        baseCookie(properties.refreshTokenName, "", 0, properties)

    fun deleteSignupTokenCookie(properties: CookieProperties): ResponseCookie =
        baseCookie(properties.signupTokenName, "", 0, properties)

    fun getAccessTokenFromCookies(cookies: Array<Cookie>?, properties: CookieProperties): String? =
        cookies?.firstOrNull { it.name == properties.accessTokenName }?.value

    fun getRefreshTokenFromCookies(cookies: Array<Cookie>?, properties: CookieProperties): String? =
        cookies?.firstOrNull { it.name == properties.refreshTokenName }?.value

    fun getSignupTokenFromCookies(cookies: Array<Cookie>?, properties: CookieProperties): String? =
        cookies?.firstOrNull { it.name == properties.signupTokenName }?.value

    private fun baseCookie(
        name: String,
        value: String,
        maxAge: Long,
        properties: CookieProperties,
    ): ResponseCookie {
        val builder = ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(properties.secure)
            .path(properties.path)
            .maxAge(maxAge)
            .sameSite(properties.sameSite)
        if (!properties.domain.isNullOrBlank()) {
            builder.domain(properties.domain)
        }
        return builder.build()
    }
}
