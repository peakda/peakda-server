package com.peakda.server.domain.spot.exception

import com.peakda.server.common.exception.BusinessException
import com.peakda.server.common.exception.ErrorCode

class SpotNotFoundException : BusinessException(ErrorCode.SPOT_NOT_FOUND)
class AttractionNotFoundException : BusinessException(ErrorCode.ATTRACTION_NOT_FOUND)
