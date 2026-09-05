package com.peakda.server.domain.user.exception

import com.peakda.server.common.exception.BusinessException
import com.peakda.server.common.exception.ErrorCode

class NicknameDuplicatedException : BusinessException(ErrorCode.NICKNAME_DUPLICATED)

class UserNotFoundException : BusinessException(ErrorCode.RESOURCE_NOT_FOUND)

class AdminUserNotFoundException : BusinessException(ErrorCode.USER_NOT_FOUND)

class UserStatusNotChangeableException : BusinessException(ErrorCode.USER_STATUS_NOT_CHANGEABLE)

class AdminSelfActionNotAllowedException : BusinessException(ErrorCode.ADMIN_SELF_ACTION_NOT_ALLOWED)

class SelfFollowNotAllowedException : BusinessException(ErrorCode.SELF_FOLLOW_NOT_ALLOWED)

class SelfBlockNotAllowedException : BusinessException(ErrorCode.SELF_BLOCK_NOT_ALLOWED)
