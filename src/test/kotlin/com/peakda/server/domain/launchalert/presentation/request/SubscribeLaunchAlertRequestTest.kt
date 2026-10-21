package com.peakda.server.domain.launchalert.presentation.request

import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class SubscribeLaunchAlertRequestTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator

    @ParameterizedTest
    @ValueSource(strings = ["ex1@xxx.com", "first.last+tag@mail.co.kr", "a_b-c@sub-domain.example.io"])
    fun `일반 이메일은 받는다`(email: String) {
        assertThat(validator.validate(request(email))).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "=cmd|'/c calc'!A1@x.io",
            "+1@x.io",
            "-1@x.io",
            "\"a,b\"@x.io",
            "a@localhost",
            "a@x.c",
            "a@-x.io",
            "no-at-sign.io",
        ],
    )
    fun `수식으로 읽히거나 형식이 어긋난 이메일은 거절한다`(email: String) {
        assertThat(validator.validate(request(email))).isNotEmpty()
    }

    private fun request(email: String) =
        SubscribeLaunchAlertRequest(platform = LaunchAlertPlatform.IOS, email = email, privacyConsent = true)
}
