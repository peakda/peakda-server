package com.peakda.server.domain.notification.exception

import com.peakda.server.common.exception.BusinessException
import com.peakda.server.common.exception.ErrorCode

class NotificationNotFoundException : BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND)
