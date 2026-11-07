package com.peakda.server.domain.feed.application

import com.peakda.server.common.exception.BusinessException
import com.peakda.server.common.exception.ErrorCode

class InvalidEmojiException : BusinessException(ErrorCode.INVALID_REQUEST)
