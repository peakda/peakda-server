package com.peakda.server.global.exception

import com.peakda.server.global.model.ErrorCode

abstract class BusinessException(
    val errorCode: ErrorCode,
) : RuntimeException(errorCode.message)
