package com.peakda.server.domain.auth.apple.application

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AppleLoginConfig {

    @Bean
    fun appleIdentityTokenVerifier(properties: AppleProperties): AppleIdentityTokenVerifier =
        AppleIdentityTokenVerifier.create(properties.bundleId)

    /** 꺼져 있으면 개인키가 없을 수 있으므로 만들지 않는다. 켜져 있으면 키를 여기서 읽어 잘못된 키를 기동 때 드러낸다. */
    @Bean
    @ConditionalOnProperty(prefix = "app.apple", name = ["enabled"], havingValue = "true")
    fun appleClientSecretGenerator(properties: AppleProperties): AppleClientSecretGenerator =
        AppleClientSecretGenerator.from(properties)
}
