package com.peakda.server.domain.auth.oauth.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class GoogleOAuth2UserInfoTest {

    @Test
    fun `sub_email_picture 을 추출한다`() {
        val userInfo = GoogleOAuth2UserInfo(
            mapOf(
                "sub" to "google-12345",
                "email" to "peakda@gmail.com",
                "picture" to "https://lh3.googleusercontent.com/profile.jpg",
            ),
        )

        assertThat(userInfo.getProviderId()).isEqualTo("google-12345")
        assertThat(userInfo.getEmail()).isEqualTo("peakda@gmail.com")
        assertThat(userInfo.getProfileImageUrl()).isEqualTo("https://lh3.googleusercontent.com/profile.jpg")
    }

    @Test
    fun `email_picture 이 없으면 null 을 반환한다`() {
        val userInfo = GoogleOAuth2UserInfo(mapOf("sub" to "google-12345"))

        assertThat(userInfo.getEmail()).isNull()
        assertThat(userInfo.getProfileImageUrl()).isNull()
    }

    @Test
    fun `sub 가 없으면 예외를 던진다`() {
        val userInfo = GoogleOAuth2UserInfo(emptyMap())

        assertThatThrownBy { userInfo.getProviderId() }
            .isInstanceOf(IllegalStateException::class.java)
    }
}
