package com.peakda.server.domain.user.exception

import com.peakda.server.global.exception.BusinessException
import com.peakda.server.global.model.ErrorCode

class NicknameDuplicatedException : BusinessException(ErrorCode.NICKNAME_DUPLICATED)

class UserNotFoundException : BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
