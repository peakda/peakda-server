package com.peakda.server.infrastructure.external.common

import com.peakda.server.common.exception.BusinessException
import java.time.Duration

class ExternalApiException(
    externalApiErrorCode: ExternalApiErrorCode,
    message: String? = null,
    cause: Throwable? = null,
    val retryAfter: Duration? = null,
) : BusinessException(externalApiErrorCode.errorCode) {
    override val message: String = message ?: externalApiErrorCode.errorCode.message

    init {
        if (cause != null) {
            initCause(cause)
        }
    }
}
