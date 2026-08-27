package com.peakda.server.domain.auth.oauth.apple

import com.peakda.server.common.exception.BusinessException
import com.peakda.server.common.exception.ErrorCode

/** Apple id_token 검증 실패 (서명·issuer·audience·만료 등). */
class AppleTokenInvalidException : BusinessException(ErrorCode.APPLE_TOKEN_INVALID)
