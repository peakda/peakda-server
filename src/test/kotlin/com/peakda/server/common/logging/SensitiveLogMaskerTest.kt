package com.peakda.server.common.logging

import com.peakda.server.domain.auth.app.presentation.request.AppTokenExchangeRequest
import com.peakda.server.domain.auth.app.presentation.request.AppTokenRefreshRequest
import com.peakda.server.domain.notification.entity.DevicePlatform
import com.peakda.server.domain.notification.presentation.request.RegisterDeviceRequest
import com.peakda.server.domain.spot.presentation.request.SpotMatchRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SensitiveLogMaskerTest {

    @Test
    fun `요청 객체의 리프레시 토큰과 교환 코드를 가린다`() {
        val refresh = SensitiveLogMasker.mask("request=${AppTokenRefreshRequest("eyJhbGciOi.payload.sig")}")
        val exchange = SensitiveLogMasker.mask("request=${AppTokenExchangeRequest("one-time-code_123")}")

        assertThat(refresh).isEqualTo("request=AppTokenRefreshRequest(refreshToken=***)")
        assertThat(exchange).isEqualTo("request=AppTokenExchangeRequest(code=***)")
    }

    @Test
    fun `FCM 기기 토큰을 가리고 나머지 필드는 남긴다`() {
        val platform = DevicePlatform.entries.first()
        val masked = SensitiveLogMasker.mask("request=${RegisterDeviceRequest("fcm:APA91b-x_y", platform)}")

        assertThat(masked).isEqualTo("request=RegisterDeviceRequest(token=***, platform=$platform)")
    }

    @Test
    fun `좌표는 소수 둘째 자리까지만 남긴다`() {
        val body = SensitiveLogMasker.mask("request=${SpotMatchRequest(37.566535, 126.977969, "서울광장")}")
        val params = SensitiveLogMasker.mask("lat=37.5665, lng=-126.9779, minLat=33.1, maxLng=131.87249")

        assertThat(body).contains("latitude=37.57, longitude=126.98, name=서울광장")
        assertThat(params).isEqualTo("lat=37.57, lng=-126.98, minLat=33.10, maxLng=131.87")
    }

    @Test
    fun `이메일은 첫 글자와 도메인만 남긴다`() {
        assertThat(SensitiveLogMasker.mask("email=devuser@example.com, page=0"))
            .isEqualTo("email=d***@example.com, page=0")
    }

    @Test
    fun `이름이 겹치기만 하는 필드와 null 값은 건드리지 않는다`() {
        val text = "regionCode=11, category=null, plateau=3, lat=null, email=null"

        assertThat(SensitiveLogMasker.mask(text)).isEqualTo(text)
    }
}
