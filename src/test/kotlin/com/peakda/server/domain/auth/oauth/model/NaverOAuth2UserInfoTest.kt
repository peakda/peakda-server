package com.peakda.server.domain.auth.oauth.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class NaverOAuth2UserInfoTest {

    @Test
    fun `response 객체에서 id_email_profile_image 를 추출한다`() {
        val userInfo = NaverOAuth2UserInfo(
            mapOf(
                "resultcode" to "00",
                "message" to "success",
                "response" to mapOf(
                    "id" to "naver-12345",
                    "email" to "peakda@naver.com",
                    "profile_image" to "https://image.naver.com/profile.jpg",
                ),
            ),
        )

        assertThat(userInfo.getProviderId()).isEqualTo("naver-12345")
        assertThat(userInfo.getEmail()).isEqualTo("peakda@naver.com")
        assertThat(userInfo.getProfileImageUrl()).isEqualTo("https://image.naver.com/profile.jpg")
    }

    @Test
    fun `email_profile_image 가 없으면 null 을 반환한다`() {
        val userInfo = NaverOAuth2UserInfo(
            mapOf("response" to mapOf("id" to "naver-12345")),
        )

        assertThat(userInfo.getEmail()).isNull()
        assertThat(userInfo.getProfileImageUrl()).isNull()
    }

    @Test
    fun `response 에 id 가 없으면 예외를 던진다`() {
        val userInfo = NaverOAuth2UserInfo(mapOf("response" to emptyMap<String, Any>()))

        assertThatThrownBy { userInfo.getProviderId() }
            .isInstanceOf(IllegalStateException::class.java)
    }
}
