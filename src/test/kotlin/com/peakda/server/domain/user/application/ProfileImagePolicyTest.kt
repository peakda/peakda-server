package com.peakda.server.domain.user.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProfileImagePolicyTest {

    @Test
    fun `업로드마다 prefix 가 달라 같은 사용자라도 주소가 겹치지 않는다`() {
        val first = ProfileImagePolicy.prefixOf(42L, "9b1deb4d")
        val second = ProfileImagePolicy.prefixOf(42L, "c2f4a1e0")

        assertThat(first).isEqualTo("profile-images/42/9b1deb4d")
        assertThat(second).isNotEqualTo(first)
    }

    @Test
    fun `prefix 가 고정이던 과거 key 도 우리가 관리하는 이미지로 본다`() {
        assertThat(ProfileImagePolicy.isManagedKey(42L, "profile-images/42/main.jpg")).isTrue()
        assertThat(ProfileImagePolicy.isManagedKey(42L, "profile-images/42/9b1deb4d/main.jpg")).isTrue()
    }

    @Test
    fun `다른 사용자나 외부 URL 은 관리 대상이 아니다`() {
        assertThat(ProfileImagePolicy.isManagedKey(42L, "profile-images/7/main.jpg")).isFalse()
        assertThat(ProfileImagePolicy.isManagedKey(42L, "https://k.kakaocdn.net/profile.jpg")).isFalse()
    }

    @Test
    fun `main key 에서 나머지 variant key 를 만든다`() {
        val thumbnail = ProfileImagePolicy.VARIANTS.first { it.name == ProfileImagePolicy.THUMBNAIL_VARIANT }

        assertThat(ProfileImagePolicy.variantKeyOf("profile-images/42/9b1deb4d/main.jpg", thumbnail))
            .isEqualTo("profile-images/42/9b1deb4d/thumbnail.jpg")
    }
}
