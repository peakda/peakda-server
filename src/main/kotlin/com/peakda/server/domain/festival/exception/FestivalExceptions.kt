package com.peakda.server.domain.festival.exception

import com.peakda.server.common.exception.BusinessException
import com.peakda.server.common.exception.ErrorCode

class FestivalNotFoundException : BusinessException(ErrorCode.FESTIVAL_NOT_FOUND)

class FestivalEditorialNotFoundException : BusinessException(ErrorCode.FESTIVAL_EDITORIAL_NOT_FOUND)
