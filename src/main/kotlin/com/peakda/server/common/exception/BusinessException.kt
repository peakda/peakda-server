package com.peakda.server.common.exception

import com.peakda.server.common.exception.ErrorCode

abstract class BusinessException(
    val errorCode: ErrorCode,
) : RuntimeException(errorCode.message)
