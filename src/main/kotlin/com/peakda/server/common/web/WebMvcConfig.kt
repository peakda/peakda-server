package com.peakda.server.common.web

import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * JSON 단일 응답 강제. Accept 헤더가 XML 을 요청해도 JSON 으로 응답한다.
 * 외부 API XML 파싱은 [com.peakda.server.infrastructure.external.datagokr.DataGoKrErrorDecoder]
 * 가 별도 XmlMapper 인스턴스로 처리하므로 영향 없음.
 */
@Configuration
class WebMvcConfig : WebMvcConfigurer {

    /**
     * 백오피스 셸 진입점. `/admin` 과 `/admin/` 을 모두 등록한다.
     *
     * Spring Framework 6 부터 뒤 슬래시 매칭이 기본 해제되어 `/admin` 규칙만으로는 `/admin/` 이 404 가 되고,
     * welcome-page 의 index.html 처리는 루트 `/` 에만 적용되어 하위 경로에서는 동작하지 않는다.
     */
    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addRedirectViewController("/admin", "/admin/index.html")
        registry.addRedirectViewController("/admin/", "/admin/index.html")
    }

    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
        configurer
            .defaultContentType(MediaType.APPLICATION_JSON)
            .favorParameter(false)
            .ignoreAcceptHeader(true)
    }
}
