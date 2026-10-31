package com.peakda.server.domain.festival.exception

import com.peakda.server.common.exception.BusinessException
import com.peakda.server.common.exception.ErrorCode

class FestivalNotFoundException : BusinessException(ErrorCode.FESTIVAL_NOT_FOUND)

class FestivalNotEditableException : BusinessException(ErrorCode.FESTIVAL_NOT_EDITABLE)

class FestivalDuplicatedException : BusinessException(ErrorCode.FESTIVAL_DUPLICATED)

class FestivalInvalidPeriodException : BusinessException(ErrorCode.FESTIVAL_INVALID_PERIOD)

class FestivalInvalidCoordinatesException : BusinessException(ErrorCode.FESTIVAL_INVALID_COORDINATES)

class FestivalEditorialNotFoundException : BusinessException(ErrorCode.FESTIVAL_EDITORIAL_NOT_FOUND)

class FestivalEditorialNotPublishedException : BusinessException(ErrorCode.FESTIVAL_EDITORIAL_NOT_PUBLISHED)
