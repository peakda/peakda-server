package com.peakda.server.domain.auth.apple.repository

import com.peakda.server.common.test.IntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

class AppleTokenRepositoryTest : IntegrationTestSupport() {

    @Autowired
    lateinit var repository: AppleTokenRepository

    @BeforeEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    fun `처음 로그인하면 토큰을 저장하고 다시 로그인하면 새 토큰으로 바꾼다`() {
        repository.upsert(APPLE_USER_ID, "first-token")
        repository.upsert(APPLE_USER_ID, "second-token")

        assertThat(repository.count()).isEqualTo(1)
        val saved = repository.findByIdOrNull(APPLE_USER_ID)!!
        assertThat(saved.refreshToken).isEqualTo("second-token")
        assertThat(saved.updatedAt).isAfterOrEqualTo(saved.createdAt)
    }

    companion object {
        private const val APPLE_USER_ID = "001234.0123456789abcdef0123456789abcdef.0123"
    }
}
