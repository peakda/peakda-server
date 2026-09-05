package com.peakda.server.domain.admin.exception

import com.peakda.server.common.exception.BusinessException
import com.peakda.server.common.exception.ErrorCode

class SchedulerJobNotFoundException : BusinessException(ErrorCode.SCHEDULER_JOB_NOT_FOUND)
class SchedulerJobAlreadyRunningException : BusinessException(ErrorCode.SCHEDULER_JOB_ALREADY_RUNNING)
