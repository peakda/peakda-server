package com.peakda.server.global.security.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerExceptionResolver

@Component
class SecurityExceptionConfig(
    @param:Qualifier("handlerExceptionResolver")
    private val resolver: HandlerExceptionResolver,
) {

    fun configure(exceptions: ExceptionHandlingConfigurer<HttpSecurity>) {
        exceptions
            .authenticationEntryPoint { request, response, exception ->
                resolver.resolveException(request, response, null, exception)
            }
            .accessDeniedHandler { request, response, exception ->
                resolver.resolveException(request, response, null, exception)
            }
    }
}
