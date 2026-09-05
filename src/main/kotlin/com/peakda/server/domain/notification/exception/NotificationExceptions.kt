package com.peakda.server.domain.notification.exception

import com.peakda.server.common.exception.BusinessException
import com.peakda.server.common.exception.ErrorCode

class NotificationNotFoundException : BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND)
class NoticeNotFoundException : BusinessException(ErrorCode.NOTICE_NOT_FOUND)
class NoticeNotEditableException : BusinessException(ErrorCode.NOTICE_NOT_EDITABLE)
class NoticeAlreadyDispatchedException : BusinessException(ErrorCode.NOTICE_ALREADY_DISPATCHED)
