package com.peakda.server.common.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * 요청마다 ID 를 정해 MDC(`requestId`)와 응답 헤더 `X-Request-Id` 에 싣는다.
 *
 * prod 에서는 Caddy 가 만든 ID 를 그대로 써서 접근 로그와 앱 로그가 같은 값으로 이어진다.
 * 헤더가 없거나 형식이 맞지 않으면(로컬·컨테이너 내부 호출) 새로 만든다.
 * 인증 실패 로그에도 ID 가 남도록 보안 필터보다 앞에 둔다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestIdFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestId = request.getAttribute(ATTRIBUTE) as? String
            ?: resolve(request.getHeader(HEADER)).also { request.setAttribute(ATTRIBUTE, it) }

        MDC.put(MDC_KEY, requestId)
        response.setHeader(HEADER, requestId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_KEY)
        }
    }

    // 처리되지 않은 예외로 /error 에 다시 들어올 때도 같은 ID 로 로그를 남긴다.
    override fun shouldNotFilterErrorDispatch(): Boolean = false

    private fun resolve(header: String?): String =
        header?.takeIf { VALID_ID.matches(it) } ?: UUID.randomUUID().toString()

    companion object {
        const val HEADER = "X-Request-Id"
        const val MDC_KEY = "requestId"

        private val ATTRIBUTE = "${RequestIdFilter::class.java.name}.requestId"

        // 헤더 값이 그대로 로그에 들어가므로 영숫자·하이픈 64자까지만 받는다(Caddy 의 UUID 포함).
        private val VALID_ID = Regex("[A-Za-z0-9-]{1,64}")
    }
}
