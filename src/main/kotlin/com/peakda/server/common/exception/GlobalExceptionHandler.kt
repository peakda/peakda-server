package com.peakda.server.common.exception

import com.peakda.server.common.exception.BusinessException
import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.response.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ApiResponse<Unit>> {
        log.warn("비즈니스 예외 - code={}, message={}", e.errorCode.name, e.message)
        return buildResponse(e.errorCode)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Unit>> {
        val detail = e.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        log.warn("요청 검증 실패 - {}", detail)
        return buildResponse(ErrorCode.INVALID_REQUEST)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(e: AccessDeniedException): ResponseEntity<ApiResponse<Unit>> {
        log.warn("접근 권한 없음 - {}", e.message)
        return buildResponse(ErrorCode.FORBIDDEN)
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(e: AuthenticationException): ResponseEntity<ApiResponse<Unit>> {
        log.warn("인증 실패 - {}", e.message)
        return buildResponse(ErrorCode.UNAUTHORIZED)
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(e: NoResourceFoundException): ResponseEntity<ApiResponse<Unit>> {
        return buildResponse(ErrorCode.RESOURCE_NOT_FOUND)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(e: Exception): ResponseEntity<ApiResponse<Unit>> {
        log.error("예상하지 못한 오류가 발생했습니다.", e)
        return buildResponse(ErrorCode.INTERNAL_SERVER_ERROR)
    }

    private fun buildResponse(errorCode: ErrorCode): ResponseEntity<ApiResponse<Unit>> {
        val body = ApiResponse.error<Unit>(errorCode)
        return ResponseEntity.status(errorCode.httpStatus).body(body)
    }
}
