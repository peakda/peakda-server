package com.peakda.server.common.exception

import com.peakda.server.common.exception.ErrorCode

class AuthorizationException(
    errorCode: ErrorCode,
) : BusinessException(errorCode)
