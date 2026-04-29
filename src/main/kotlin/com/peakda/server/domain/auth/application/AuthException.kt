package com.peakda.server.domain.auth.application

import com.peakda.server.global.exception.BusinessException
import com.peakda.server.global.model.ErrorCode

class AuthException(
    errorCode: ErrorCode,
) : BusinessException(errorCode)
