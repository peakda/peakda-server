package com.peakda.server.domain.report.exception

import com.peakda.server.common.exception.BusinessException
import com.peakda.server.common.exception.ErrorCode

class SelfReportNotAllowedException : BusinessException(ErrorCode.SELF_REPORT_NOT_ALLOWED)
