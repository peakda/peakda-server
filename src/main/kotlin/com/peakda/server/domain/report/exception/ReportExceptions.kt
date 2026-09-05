package com.peakda.server.domain.report.exception

import com.peakda.server.common.exception.BusinessException
import com.peakda.server.common.exception.ErrorCode

class SelfReportNotAllowedException : BusinessException(ErrorCode.SELF_REPORT_NOT_ALLOWED)
class ReportNotFoundException : BusinessException(ErrorCode.REPORT_NOT_FOUND)
class ReportAlreadyReviewedException : BusinessException(ErrorCode.REPORT_ALREADY_REVIEWED)
class ReportActionNotSupportedException : BusinessException(ErrorCode.REPORT_ACTION_NOT_SUPPORTED)
