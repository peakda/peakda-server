package com.peakda.server.domain.notification.presentation.request

import com.peakda.server.domain.notification.entity.DevicePlatform
import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RegisterDeviceRequestTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `ASCII 토큰은 1024자까지 허용한다`() {
        val request = RegisterDeviceRequest("a".repeat(1024), DevicePlatform.ANDROID)

        assertThat(validator.validate(request)).isEmpty()
    }

    @Test
    fun `1024자를 초과한 토큰은 거부한다`() {
        val request = RegisterDeviceRequest("a".repeat(1025), DevicePlatform.ANDROID)

        assertThat(validator.validate(request).map { it.propertyPath.toString() }).contains("token")
    }

    @Test
    fun `멀티바이트 문자가 포함된 토큰은 거부한다`() {
        val request = RegisterDeviceRequest("token-한글", DevicePlatform.ANDROID)

        assertThat(validator.validate(request).map { it.propertyPath.toString() }).contains("token")
    }
}
