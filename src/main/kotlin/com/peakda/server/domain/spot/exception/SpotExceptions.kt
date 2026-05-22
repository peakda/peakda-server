package com.peakda.server.domain.spot.exception

import com.peakda.server.common.exception.BusinessException
import com.peakda.server.common.exception.ErrorCode

class SpotNotFoundException : BusinessException(ErrorCode.SPOT_NOT_FOUND)
class AttractionNotFoundException : BusinessException(ErrorCode.ATTRACTION_NOT_FOUND)
class SpotRecordNotFoundException : BusinessException(ErrorCode.SPOT_RECORD_NOT_FOUND)
class SpotRecordForbiddenException : BusinessException(ErrorCode.SPOT_RECORD_FORBIDDEN)
class SpotRecordInvalidStatusException : BusinessException(ErrorCode.SPOT_RECORD_INVALID_STATUS)
class PlantNotFoundException : BusinessException(ErrorCode.PLANT_NOT_FOUND)
class PlantInactiveException : BusinessException(ErrorCode.PLANT_INACTIVE)
class PlantSuggestionDuplicateException : BusinessException(ErrorCode.PLANT_SUGGESTION_DUPLICATE)
class PlantSuggestionRateLimitException : BusinessException(ErrorCode.PLANT_SUGGESTION_RATE_LIMIT)
