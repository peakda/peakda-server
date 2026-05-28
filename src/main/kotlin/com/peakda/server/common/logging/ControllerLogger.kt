package com.peakda.server.common.logging

import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

/**
 * `com.peakda.server` 패키지 내 모든 `@RestController` 진입점 호출을 로깅한다.
 *
 * 형식: `{호출클래스}.{호출메서드} - {요청파라미터}`
 *
 * 인증 주체·파일·서블릿 객체 등 노이즈 인자는 파라미터 로그에서 제외한다.
 */
@Aspect
@Component
class ControllerLogger {

    @Pointcut("within(com.peakda.server..*) && @within(org.springframework.web.bind.annotation.RestController)")
    fun restController() {
    }

    @Before("restController()")
    fun logRequest(joinPoint: JoinPoint) {
        if (!log.isInfoEnabled) return

        val signature = joinPoint.signature as MethodSignature
        val target = "${signature.declaringType.simpleName}.${signature.name}"
        log.info("{} - {}", target, formatParameters(signature.parameterNames, joinPoint.args))
    }

    private fun formatParameters(names: Array<String>?, args: Array<Any?>): String {
        val params = args.indices
            .filterNot { isNoise(args[it]) }
            .joinToString(", ") { index ->
                val name = names?.getOrNull(index) ?: "arg$index"
                "$name=${args[index]}"
            }
        return params.ifEmpty { "(no params)" }
    }

    private fun isNoise(arg: Any?): Boolean = when (arg) {
        null -> false
        is ServletRequest, is ServletResponse -> true
        is MultipartFile -> true
        is Authentication, is SecurityContext, is UserDetails -> true
        else -> false
    }

    companion object {
        private val log = LoggerFactory.getLogger(ControllerLogger::class.java)
    }
}
