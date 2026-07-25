package com.peakda.server.domain.curation.exception

import com.peakda.server.common.exception.BusinessException
import com.peakda.server.common.exception.ErrorCode

class CurationNotFoundException : BusinessException(ErrorCode.CURATION_NOT_FOUND)
