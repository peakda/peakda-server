package com.peakda.server.domain.user.exception

import com.peakda.server.common.exception.BusinessException
import com.peakda.server.common.exception.ErrorCode

class NicknameDuplicatedException : BusinessException(ErrorCode.NICKNAME_DUPLICATED)

class UserNotFoundException : BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
