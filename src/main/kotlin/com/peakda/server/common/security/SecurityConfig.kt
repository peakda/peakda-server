package com.peakda.server.common.security

import com.peakda.server.common.security.SecurityExceptionConfig
import com.peakda.server.common.security.filter.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
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
            "/admin",
            "/admin/**",
            "/oauth2/**",
            "/login/oauth2/**",
            "/api/auth/refresh",
            "/api/auth/app/token",
            "/api/auth/app/token/refresh",
        )
    }

    @Bean
    fun roleHierarchy(): RoleHierarchy =
        RoleHierarchyImpl.fromHierarchy("ROLE_ADMIN > ROLE_USER")

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
                        it.policyDirectives("default-src 'self'; img-src 'self' data: https:; frame-ancestors 'self'")
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
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/curations",
                        "/api/curations/**",
                        "/api/festivals/**",
                    ).permitAll()
                    .requestMatchers(
                        "/api/auth/signup/nickname/check",
                        "/api/auth/signup/complete",
                        "/api/auth/signup/profile-image",
                    ).hasRole("SIGNUP")
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    // health/readiness·health/liveness 는 컨테이너 헬스체크와 외부 감시가
                    // 인증 없이 호출한다. show-details 가 never 라 상태값만 노출된다.
                    .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                    // Alloy 가 컨테이너 네트워크(app:8080)로 30초마다 긁는다. 토큰을 쥐여줄 수 없다.
                    // 외부 차단은 Caddy 가 맡는다 — /actuator/* 를 health 만 남기고 404 로 막는다.
                    .requestMatchers("/actuator/prometheus").permitAll()
                    .requestMatchers("/actuator/**").hasRole("ADMIN")
                    .anyRequest().hasRole("USER")
            }
            .exceptionHandling { securityExceptionConfig.configure(it) }
            .oauth2Login { oAuth2SecurityConfig.configure(it) }
            .addFilterBefore(jwtAuthenticationFilter, OAuth2LoginAuthenticationFilter::class.java)

        return http.build()
    }
}
