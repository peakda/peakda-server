package com.peakda.server.common.exception

class AuthorizationException(
    errorCode: ErrorCode,
) : BusinessException(errorCode)
