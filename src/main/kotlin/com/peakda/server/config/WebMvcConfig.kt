package com.peakda.server.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * JSON 단일 응답 강제. Accept 헤더가 XML 을 요청해도 JSON 으로 응답한다.
 * 외부 API XML 파싱은 [com.peakda.server.infrastructure.external.common.DataGoKrErrorDecoder]
 * 가 별도 XmlMapper 인스턴스로 처리하므로 영향 없음.
 */
@Configuration
class WebMvcConfig : WebMvcConfigurer {

    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
        configurer
            .defaultContentType(MediaType.APPLICATION_JSON)
            .favorParameter(false)
            .ignoreAcceptHeader(true)
    }
}
