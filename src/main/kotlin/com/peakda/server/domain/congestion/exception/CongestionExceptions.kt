package com.peakda.server.domain.congestion.exception

import com.peakda.server.common.exception.BusinessException
import com.peakda.server.common.exception.ErrorCode

class CongestionLinkNotFoundException : BusinessException(ErrorCode.CONGESTION_LINK_NOT_FOUND)

class CongestionLinkAttractionRequiredException : BusinessException(ErrorCode.CONGESTION_LINK_ATTRACTION_REQUIRED)
