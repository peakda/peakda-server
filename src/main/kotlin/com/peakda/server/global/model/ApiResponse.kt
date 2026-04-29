package com.peakda.server.global.model

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.HttpStatus

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val status: Int,
    val code: String,
    val message: String,
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
