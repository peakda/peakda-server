package com.peakda.server.domain.auth.application

import com.peakda.server.common.exception.BusinessException
import com.peakda.server.common.exception.ErrorCode

class AuthException(
    errorCode: ErrorCode,
) : BusinessException(errorCode)
