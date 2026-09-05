package com.peakda.server.common.openapi

import com.peakda.server.common.exception.ErrorCode

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ApiErrorResponses(vararg val value: ErrorCode)
