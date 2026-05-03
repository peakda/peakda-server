package com.peakda.server.infrastructure.external.common

import com.peakda.server.global.exception.BusinessException

class ExternalApiException(
    externalApiErrorCode: ExternalApiErrorCode,
    message: String? = null,
    cause: Throwable? = null,
) : BusinessException(externalApiErrorCode.errorCode) {
    override val message: String = message ?: externalApiErrorCode.errorCode.message

    init {
        if (cause != null) {
            initCause(cause)
        }
    }
}
