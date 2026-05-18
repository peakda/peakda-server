package com.peakda.server.common.security

import com.peakda.server.common.security.SecurityExceptionConfig
import com.peakda.server.common.security.filter.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
import org.springframework.web.cors.CorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val securityExceptionConfig: SecurityExceptionConfig,
    private val corsConfigurationSource: CorsConfigurationSource,
    private val oAuth2SecurityConfig: OAuth2SecurityConfig,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {

    companion object {
        private val PUBLIC_URLS = arrayOf(
            "/",
            "/favicon.ico",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/oauth2/**",
            "/login/oauth2/**",
            "/api/auth/refresh"
        )
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .headers { header ->
                header
                    .frameOptions { it.sameOrigin() }
                    .contentTypeOptions(Customizer.withDefaults())
                    .cacheControl { it.disable() }
                    .contentSecurityPolicy {
                        it.policyDirectives("default-src 'self'; frame-ancestors 'self'")
                    }
                    .referrerPolicy {
                        it.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)
                    }
                    .permissionsPolicyHeader {
                        it.policy("geolocation=(), microphone=(), camera=()")
                    }
            }
            .authorizeHttpRequests {
                it.requestMatchers(*PUBLIC_URLS).permitAll()
                    .requestMatchers("/api/auth/signup/nickname/check", "/api/auth/signup/complete").hasRole("SIGNUP")
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers("/actuator/**").hasRole("ADMIN")
                    .anyRequest().hasRole("USER")
            }
            .exceptionHandling { securityExceptionConfig.configure(it) }
            .oauth2Login { oAuth2SecurityConfig.configure(it) }
            .addFilterBefore(jwtAuthenticationFilter, OAuth2LoginAuthenticationFilter::class.java)

        return http.build()
    }
}
