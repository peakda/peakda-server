package com.peakda.server.global.openapi

import com.peakda.server.global.model.ErrorCode

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ApiErrorResponses(vararg val value: ErrorCode)
