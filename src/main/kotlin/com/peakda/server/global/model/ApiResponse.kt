package com.peakda.server.global.model

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.HttpStatus

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "공통 응답 envelope")
data class ApiResponse<T>(
    @field:Schema(description = "HTTP status code", example = "200")
    val status: Int,
    @field:Schema(description = "성공 시 'SUCCESS', 실패 시 ErrorCode enum name", example = "SUCCESS")
    val code: String,
    @field:Schema(description = "사람이 읽는 메시지", example = "OK")
    val message: String,
    @field:Schema(description = "응답 본문 (성공 시 채워지고, 에러 시 생략됨)", nullable = true)
    val data: T?,
) {
    companion object {
        private const val SUCCESS_CODE = "SUCCESS"

        fun <T> success(httpStatus: HttpStatus, data: T): ApiResponse<T> = ApiResponse(
            status = httpStatus.value(),
            code = SUCCESS_CODE,
            message = httpStatus.reasonPhrase,
            data = data,
        )

        fun success(httpStatus: HttpStatus): ApiResponse<Unit> = ApiResponse(
            status = httpStatus.value(),
            code = SUCCESS_CODE,
            message = httpStatus.reasonPhrase,
            data = null,
        )

        fun <T> error(errorCode: ErrorCode): ApiResponse<T> = ApiResponse(
            status = errorCode.httpStatus.value(),
            code = errorCode.name,
            message = errorCode.message,
            data = null,
        )
    }
}
