package com.peakda.server.common.image

import com.peakda.server.common.exception.BusinessException
import com.peakda.server.common.exception.ErrorCode

class ImageException(
    errorCode: ErrorCode,
) : BusinessException(errorCode)
