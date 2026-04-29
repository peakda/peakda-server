package com.peakda.server.global.exception

import com.peakda.server.global.model.ErrorCode

class AuthorizationException(
    errorCode: ErrorCode,
) : BusinessException(errorCode)
