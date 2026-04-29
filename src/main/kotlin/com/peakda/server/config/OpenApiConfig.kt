package com.peakda.server.config

import com.peakda.server.properties.OpenApiProperties
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.OpenApiCustomizer
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
                    Server().url(openApiProperties.servers.local).description("Local"),
                    Server().url(openApiProperties.servers.dev).description("Develop"),
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
    fun oauth2LoginOpenApiCustomizer(): OpenApiCustomizer {
        return OpenApiCustomizer { openApi ->
            openApi.path(
                "/oauth2/authorization/kakao",
                PathItem().get(
                    Operation()
                        .tags(listOf("Auth"))
                        .summary("카카오 로그인 시작 - 링크 클릭용")
                        .description(
                            "이 엔드포인트는 API 호출용이 아니라 브라우저 이동용입니다.\n\n" +
                                "카카오 로그인은 Swagger UI의 Execute 버튼이 아니라 아래 링크를 클릭해서 시작하세요.\n\n" +
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
    }

    private fun cookieSecurityScheme(name: String): SecurityScheme {
        return SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .`in`(SecurityScheme.In.COOKIE)
            .name(name)
    }
}
