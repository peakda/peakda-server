package com.peakda.server.common.storage

import com.peakda.server.common.exception.BusinessException
import com.peakda.server.common.exception.ErrorCode

class StorageException(
    errorCode: ErrorCode,
) : BusinessException(errorCode)
