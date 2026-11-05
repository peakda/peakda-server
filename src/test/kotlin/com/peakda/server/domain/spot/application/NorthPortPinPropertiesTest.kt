package com.peakda.server.domain.spot.application

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.context.properties.bind.BindException
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.io.ClassPathResource

class NorthPortPinPropertiesTest {
    @Test
    fun `운영 설정을 생략하면 기본 비활성이며 좌표는 null이다`() {
        val properties = bind()
        assertThat(properties.enabled).isFalse()
        assertThat(properties.latitude).isNull()
        assertThat(properties.longitude).isNull()
        assertThat(properties.linkUrl).isEmpty()
    }

    @Test
    fun `환경 변수로 활성화 좌표 링크를 지정할 수 있다`() {
        val properties = bind(
            "NORTH_PORT_PIN_ENABLED" to "true",
            "NORTH_PORT_PIN_LATITUDE" to "0.0",
            "NORTH_PORT_PIN_LONGITUDE" to "0.0",
            "NORTH_PORT_PIN_LINK_URL" to "https://example.com/north-port",
        )
        assertThat(properties.enabled).isTrue()
        assertThat(properties.latitude).isEqualTo(0.0)
        assertThat(properties.longitude).isEqualTo(0.0)
        assertThat(properties.linkUrl).isEqualTo("https://example.com/north-port")
    }

    @Test
    fun `좌표가 없는 상태로 활성화하면 바인딩에 실패한다`() {
        assertThatThrownBy { bind("NORTH_PORT_PIN_ENABLED" to "true") }
            .isInstanceOf(BindException::class.java)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "/relative", "javascript:alert(1)", "https://user:password@example.com"])
    fun `링크가 잘못된 상태로 활성화하면 바인딩에 실패한다`(url: String) {
        assertThatThrownBy {
            bind(
                "NORTH_PORT_PIN_ENABLED" to "true",
                "NORTH_PORT_PIN_LATITUDE" to "0.0",
                "NORTH_PORT_PIN_LONGITUDE" to "0.0",
                "NORTH_PORT_PIN_LINK_URL" to url,
            )
        }.isInstanceOf(BindException::class.java)
    }

    @ParameterizedTest
    @ValueSource(doubles = [-91.0, 91.0, Double.NaN, Double.POSITIVE_INFINITY])
    fun `유효하지 않은 위도는 거부한다`(latitude: Double) {
        assertThatThrownBy {
            NorthPortPinProperties(true, latitude, 0.0, "https://example.com/north-port")
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @ParameterizedTest
    @ValueSource(doubles = [-181.0, 181.0, Double.NaN, Double.POSITIVE_INFINITY])
    fun `유효하지 않은 경도는 거부한다`(longitude: Double) {
        assertThatThrownBy {
            NorthPortPinProperties(true, 0.0, longitude, "https://example.com/north-port")
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun bind(vararg env: Pair<String, String>): NorthPortPinProperties {
        val environment = StandardEnvironment()
        val sources = environment.propertySources
        sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)
        sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)
        sources.addFirst(MapPropertySource("env", mapOf(*env)))
        YamlPropertySourceLoader().load("application", ClassPathResource("application.yml")).forEach(sources::addLast)
        return Binder.get(environment).bind("peakda.spot.north-port-pin", NorthPortPinProperties::class.java).get()
    }
}
