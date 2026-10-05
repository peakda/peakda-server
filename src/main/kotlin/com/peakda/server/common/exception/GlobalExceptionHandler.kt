package com.peakda.server.common.exception

import com.peakda.server.common.exception.BusinessException
import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.response.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.MultipartException
import org.springframework.web.multipart.support.MissingServletRequestPartException
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

    /**
     * 요청이 컨트롤러에 닿기 전에 Spring MVC 가 거절한 경우다. 클라이언트 오류이므로 500 으로 돌려주면
     * 5xx 알림과 에러 로그에 섞인다. 로그에는 사용자가 보낸 값을 남기지 않고 무엇이 틀렸는지만 남긴다.
     */
    @ExceptionHandler(
        MissingServletRequestParameterException::class,
        MissingRequestHeaderException::class,
        MissingServletRequestPartException::class,
        MethodArgumentTypeMismatchException::class,
        HttpMessageNotReadableException::class,
        HandlerMethodValidationException::class,
        MultipartException::class,
    )
    fun handleBadRequest(e: Exception): ResponseEntity<ApiResponse<Unit>> {
        log.warn("잘못된 요청 - {}", describe(e))
        return buildResponse(ErrorCode.INVALID_REQUEST)
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(e: HttpRequestMethodNotSupportedException): ResponseEntity<ApiResponse<Unit>> {
        log.warn("지원하지 않는 메서드 - {}", e.method)
        // 405 는 Allow 헤더로 쓸 수 있는 메서드를 알려야 한다(RFC 9110).
        return buildResponse(ErrorCode.METHOD_NOT_ALLOWED, e.headers)
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleMediaTypeNotSupported(e: HttpMediaTypeNotSupportedException): ResponseEntity<ApiResponse<Unit>> {
        log.warn("지원하지 않는 Content-Type - {}", e.contentType)
        // Accept 헤더로 받을 수 있는 형식을 알린다.
        return buildResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE, e.headers)
    }

    // 클라이언트가 받을 수 있는 형식(Accept)으로 응답을 만들 수 없다. 본문도 그 형식으로 못 쓰므로 상태만 돌려준다.
    @ExceptionHandler(HttpMediaTypeNotAcceptableException::class)
    fun handleMediaTypeNotAcceptable(e: HttpMediaTypeNotAcceptableException): ResponseEntity<Unit> {
        log.warn("응답할 수 없는 Accept - {}", e.supportedMediaTypes)
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build()
    }

    // 업로드 크기 한도(spring.servlet.multipart)를 넘으면 컨트롤러에 닿기 전에 막힌다. 업로드는 이미지뿐이다.
    // 한도를 크게 넘는 요청은 Tomcat 이 본문을 다 읽지 않고 연결을 끊어 이 응답이 닿지 않을 수 있다(max-swallow-size).
    // MultipartException 보다 구체적인 타입이라 위의 400 처리보다 먼저 걸린다.
    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSize(e: MaxUploadSizeExceededException): ResponseEntity<ApiResponse<Unit>> {
        log.warn("업로드 크기 초과 - {}", e.cause?.javaClass?.simpleName ?: "-")
        return buildResponse(ErrorCode.IMAGE_SIZE_EXCEEDED)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(e: Exception): ResponseEntity<ApiResponse<Unit>> {
        log.error("예상하지 못한 오류가 발생했습니다.", e)
        return buildResponse(ErrorCode.INTERNAL_SERVER_ERROR)
    }

    private fun describe(e: Exception): String = when (e) {
        is MissingServletRequestParameterException -> "필수 파라미터 누락: ${e.parameterName}"
        is MissingRequestHeaderException -> "필수 헤더 누락: ${e.headerName}"
        is MissingServletRequestPartException -> "필수 파트 누락: ${e.requestPartName}"
        is MethodArgumentTypeMismatchException -> "파라미터 형식 오류: ${e.name}"
        is HttpMessageNotReadableException -> "요청 본문을 읽을 수 없음"
        is MultipartException -> "multipart 요청을 해석할 수 없음"
        is HandlerMethodValidationException -> "파라미터 검증 실패: " +
            e.parameterValidationResults.joinToString(", ") { it.methodParameter.parameterName ?: "?" }
        else -> e.javaClass.simpleName
    }

    private fun buildResponse(errorCode: ErrorCode, headers: HttpHeaders = HttpHeaders.EMPTY): ResponseEntity<ApiResponse<Unit>> {
        val body = ApiResponse.error<Unit>(errorCode)
        return ResponseEntity.status(errorCode.httpStatus).headers(headers).body(body)
    }
}
