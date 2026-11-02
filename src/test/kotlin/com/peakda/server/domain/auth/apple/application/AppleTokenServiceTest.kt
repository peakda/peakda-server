package com.peakda.server.domain.auth.apple.application

import com.peakda.server.domain.auth.apple.entity.AppleToken
import com.peakda.server.domain.auth.apple.repository.AppleTokenRepository
import com.peakda.server.infrastructure.external.apple.AppleAuthClient
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.util.Optional

class AppleTokenServiceTest {

    private val appleAuthClient = mock(AppleAuthClient::class.java)
    private val repository = mock(AppleTokenRepository::class.java)
    private val properties = AppleProperties(enabled = true, teamId = "TEAM", keyId = "KEY", privateKeyBase64 = "unused")
    private val generator = AppleClientSecretGenerator(
        teamId = "TEAM",
        keyId = "KEY",
        clientId = BUNDLE_ID,
        privateKey = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }
            .generateKeyPair().private,
    )
    private val service = AppleTokenService(generator, properties, appleAuthClient, repository)

    @Test
    fun `로그인 때 받은 코드를 교환해 애플 리프레시 토큰을 저장한다`() {
        `when`(appleAuthClient.exchangeCode(eq(BUNDLE_ID), anyString(), eq(CODE)))
            .thenReturn(AppleAuthClient.AppleTokenResponse(refreshToken = REFRESH_TOKEN))

        service.store(APPLE_USER_ID, CODE)

        verify(repository).upsert(APPLE_USER_ID, REFRESH_TOKEN)
    }

    @Test
    fun `코드 교환이 실패해도 예외를 던지지 않고 저장만 건너뛴다`() {
        `when`(appleAuthClient.exchangeCode(eq(BUNDLE_ID), anyString(), eq(CODE)))
            .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY, """{"error":"invalid_grant"}""".toByteArray(), null))

        service.store(APPLE_USER_ID, CODE)

        verifyNoInteractions(repository)
    }

    @Test
    fun `애플 로그인이 꺼져 있으면 애플 API 를 부르지 않는다`() {
        AppleTokenService(null, properties, appleAuthClient, repository).store(APPLE_USER_ID, CODE)

        verifyNoInteractions(appleAuthClient, repository)
    }

    @Test
    fun `탈퇴한 애플 회원의 토큰을 폐기하고 지운다`() {
        `when`(repository.findById(APPLE_USER_ID)).thenReturn(Optional.of(AppleToken(APPLE_USER_ID, REFRESH_TOKEN)))

        service.revoke(USER_ID, APPLE_USER_ID)

        verify(appleAuthClient).revokeRefreshToken(eq(BUNDLE_ID), anyString(), eq(REFRESH_TOKEN))
        verify(repository).deleteById(APPLE_USER_ID)
    }

    @Test
    fun `폐기가 실패하면 다시 시도할 수 있게 토큰을 남긴다`() {
        `when`(repository.findById(APPLE_USER_ID)).thenReturn(Optional.of(AppleToken(APPLE_USER_ID, REFRESH_TOKEN)))
        doThrow(ResourceAccessException("timeout"))
            .`when`(appleAuthClient).revokeRefreshToken(eq(BUNDLE_ID), anyString(), eq(REFRESH_TOKEN))

        service.revoke(USER_ID, APPLE_USER_ID)

        verify(repository, never()).deleteById(APPLE_USER_ID)
    }

    @Test
    fun `저장된 토큰이 없으면 애플 API 를 부르지 않는다`() {
        `when`(repository.findById(APPLE_USER_ID)).thenReturn(Optional.empty())

        service.revoke(USER_ID, APPLE_USER_ID)

        verifyNoInteractions(appleAuthClient)
    }

    // Kotlin non-null 파라미터에 Mockito eq() 의 null 반환값이 들어가지 않게 한다.
    private fun eq(value: String): String = ArgumentMatchers.eq(value) ?: value

    companion object {
        private const val BUNDLE_ID = "com.peakda.app"
        private const val USER_ID = 42L
        private const val APPLE_USER_ID = "001234.0123456789abcdef0123456789abcdef.0123"
        private const val CODE = "apple-authorization-code"
        private const val REFRESH_TOKEN = "apple-refresh-token"
    }
}
