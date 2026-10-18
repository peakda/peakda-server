package com.peakda.server.domain.auth.session.application

import com.peakda.server.common.security.cookie.CookieProperties
import com.peakda.server.common.security.cookie.CookieUtils
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component

/**
 * 웹 세션의 기기 정보. 브라우저마다 device-id 쿠키를 하나 발급해 같은 브라우저의 재로그인을 같은 기기로 본다.
 */
@Component
class WebSessionDevices(
    private val cookieProperties: CookieProperties,
) {

    fun resolve(request: HttpServletRequest): SessionDevice = SessionDevice.web(
        deviceId = CookieUtils.getDeviceIdFromCookies(request.cookies, cookieProperties),
        userAgent = request.getHeader(HttpHeaders.USER_AGENT),
    )

    /** 처음 받은 브라우저에는 발급하고, 이미 있으면 만료일을 다시 늘린다. */
    fun writeCookie(response: HttpServletResponse, device: SessionDevice) {
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            CookieUtils.createDeviceIdCookie(device.deviceId, cookieProperties).toString(),
        )
    }
}
