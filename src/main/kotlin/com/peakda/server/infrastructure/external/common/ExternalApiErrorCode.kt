package com.peakda.server.infrastructure.external.common

import com.peakda.server.common.exception.ErrorCode

enum class ExternalApiErrorCode(
    val errorCode: ErrorCode,
) {
    EXTERNAL_API_UNAVAILABLE(ErrorCode.EXTERNAL_API_UNAVAILABLE),
    EXTERNAL_API_TIMEOUT(ErrorCode.EXTERNAL_API_TIMEOUT),
    EXTERNAL_API_QUOTA_EXCEEDED(ErrorCode.EXTERNAL_API_QUOTA_EXCEEDED),
    EXTERNAL_API_AUTH_FAILED(ErrorCode.EXTERNAL_API_AUTH_FAILED),
    EXTERNAL_API_INVALID_RESPONSE(ErrorCode.EXTERNAL_API_INVALID_RESPONSE),
    EXTERNAL_API_BAD_REQUEST(ErrorCode.EXTERNAL_API_BAD_REQUEST),
}
