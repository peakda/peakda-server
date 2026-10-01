package com.peakda.server.domain.attraction.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LegalDongCodeTest {
    @Test
    fun `시도 2자리와 시군구 3자리를 5자리 시군구 코드로 합친다`() {
        assertThat(LegalDongCode.sigunguCode("11", "110")).isEqualTo("11110")
        assertThat(LegalDongCode.areaCode("11")).isEqualTo("11")
    }

    @Test
    fun `세종처럼 두 필드에 5자리 코드가 겹쳐 오면 그대로 쓴다`() {
        assertThat(LegalDongCode.sigunguCode("36110", "36110")).isEqualTo("36110")
        assertThat(LegalDongCode.sigunguCode("36110", "")).isEqualTo("36110")
        assertThat(LegalDongCode.areaCode("36110")).isEqualTo("36")
    }

    @Test
    fun `비어 있거나 형식이 맞지 않으면 null 이다`() {
        assertThat(LegalDongCode.sigunguCode("", "")).isNull()
        assertThat(LegalDongCode.sigunguCode("11", "")).isNull()
        assertThat(LegalDongCode.sigunguCode("1a", "110")).isNull()
        assertThat(LegalDongCode.areaCode("")).isNull()
    }
}
