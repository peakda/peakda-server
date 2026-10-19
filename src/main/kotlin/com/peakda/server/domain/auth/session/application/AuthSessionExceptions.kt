package com.peakda.server.domain.auth.session.application

import com.peakda.server.common.exception.BusinessException
import com.peakda.server.common.exception.ErrorCode

class DeviceSessionNotFoundException : BusinessException(ErrorCode.DEVICE_SESSION_NOT_FOUND)
