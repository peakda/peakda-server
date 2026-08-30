package com.peakda.server.domain.auth.app.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AuthorizationCodePayloadTest {

    @Test
    fun `가입 완료 사용자 페이로드는 왕복한다`() {
        val payload = AuthorizationCodePayload.Authenticated(42L)

        assertThat(AuthorizationCodePayload.parse(payload.serialize())).isEqualTo(payload)
    }

    @Test
    fun `가입 대기 페이로드는 왕복한다`() {
        val payload = AuthorizationCodePayload.SignupRequired("signup-token")

        assertThat(AuthorizationCodePayload.parse(payload.serialize())).isEqualTo(payload)
    }

    @Test
    fun `알 수 없는 형식은 읽지 않는다`() {
        assertThat(AuthorizationCodePayload.parse("garbage")).isNull()
        assertThat(AuthorizationCodePayload.parse("user:not-a-number")).isNull()
    }
}
