package com.peakda.server.common.openapi

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig(
    private val openApiProperties: OpenApiProperties,
) {

    companion object {
        private const val KAKAO_LOGIN_PATH = "/oauth2/authorization/kakao"
    }

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .servers(
                listOf(
                    Server().url(openApiProperties.servers.dev).description("Develop"),
                    Server().url(openApiProperties.servers.local).description("Local"),
                )
            )
            .info(
                Info()
                    .title("PEAKDA API")
                    .description("계절 여행 타이밍 안내 서비스 API 문서")
                    .version("v1")
            )
            .components(
                Components()
                    .addSecuritySchemes("accessTokenCookie", cookieSecurityScheme("access-token"))
                    .addSecuritySchemes("refreshTokenCookie", cookieSecurityScheme("refresh-token"))
                    .addSecuritySchemes("signupTokenCookie", cookieSecurityScheme("signup-token"))
            )
    }

    @Bean
    fun authUserGroupedOpenApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("01-auth-user")
            .displayName("인증·사용자")
            .pathsToMatch(
                "/api/auth/**",
                "/api/users/**",
                "/oauth2/**",
                "/login/oauth2/**",
            )
            .addOpenApiCustomizer(oauth2LoginCustomizer())
            .build()

    @Bean
    fun spotGroupedOpenApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("02-spot")
            .displayName("스팟")
            .pathsToMatch("/api/spots/**")
            .build()

    @Bean
    fun plantGroupedOpenApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("03-plant")
            .displayName("식물")
            .pathsToMatch("/api/plants/**")
            .build()

    @Bean
    fun seasonalGroupedOpenApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("04-seasonal")
            .displayName("계절 개화")
            .pathsToMatch("/api/seasonal/**")
            .build()

    @Bean
    fun feedGroupedOpenApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("05-feed")
            .displayName("피드")
            .pathsToMatch("/api/feed/**")
            .build()

    @Bean
    fun homeGroupedOpenApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("06-home")
            .displayName("홈")
            .pathsToMatch("/api/home/**")
            .build()

    @Bean
    fun searchGroupedOpenApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("07-search")
            .displayName("검색")
            .pathsToMatch("/api/search/**")
            .build()

    @Bean
    fun reportGroupedOpenApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("08-report")
            .displayName("신고")
            .pathsToMatch("/api/reports/**")
            .build()

    @Bean
    fun notificationGroupedOpenApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("09-notification")
            .displayName("알림")
            .pathsToMatch("/api/notifications/**", "/api/devices/**")
            .build()

    @Bean
    fun exploreGroupedOpenApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("10-explore")
            .displayName("탐색")
            .pathsToMatch("/api/explore", "/api/explore/**")
            .build()

    @Bean
    fun curationGroupedOpenApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("11-curation")
            .displayName("큐레이션")
            .pathsToMatch("/api/curations", "/api/curations/**", "/api/admin/curations", "/api/admin/curations/**")
            .build()

    @Bean
    fun festivalGroupedOpenApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("12-festival")
            .displayName("축제")
            .pathsToMatch("/api/festivals/**", "/api/admin/festivals/**")
            .build()

    @Bean
    fun allGroupedOpenApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("99-all")
            .displayName("전체")
            .pathsToMatch("/**")
            .addOpenApiCustomizer(oauth2LoginCustomizer())
            .build()

    private fun oauth2LoginCustomizer(): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            openApi.path(
                KAKAO_LOGIN_PATH,
                PathItem().get(
                    Operation()
                        .tags(listOf("Auth"))
                        .summary("카카오 로그인 시작 - 링크 클릭용")
                        .description(
                            "이 엔드포인트는 API 호출용이 아니라 브라우저 이동용입니다.\n\n" +
                                "카카오 로그인은 Swagger UI의 Execute 버튼이 아니라 " +
                                "아래 링크를 클릭해서 시작하세요.\n\n" +
                                "- [Local 카카오 로그인 시작](${openApiProperties.servers.local}$KAKAO_LOGIN_PATH)\n" +
                                "- [Develop 카카오 로그인 시작](${openApiProperties.servers.dev}$KAKAO_LOGIN_PATH)"
                        )
                        .externalDocs(
                            ExternalDocumentation()
                                .description("Develop 카카오 로그인 시작")
                                .url("${openApiProperties.servers.dev}$KAKAO_LOGIN_PATH")
                        )
                        .responses(
                            ApiResponses()
                                .addApiResponse("302", ApiResponse().description("OAuth2 제공자 인증 페이지로 리다이렉트"))
                        )
                )
            )
        }

    private fun cookieSecurityScheme(name: String): SecurityScheme {
        return SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .`in`(SecurityScheme.In.COOKIE)
            .name(name)
    }
}
