package com.peakda.server.domain.feed.application

import com.peakda.server.domain.spot.entity.ReactionType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class EmojiCatalogTest {
    private val catalog = EmojiCatalog()

    @Test
    fun `전체 정규 이모지 목록의 키와 표시 문자열은 정확히 왕복한다`() {
        assertThat(catalog.version).isEqualTo("17.0")
        assertThat(catalog.items).hasSize(3944)
        catalog.items.forEach { item ->
            val expected = "u:" + item.emoji.codePoints().toArray().joinToString("-") { it.toString(16) }
            assertThat(item.key).isEqualTo(expected)
            assertThat(catalog.fromStorageKey(catalog.toStorageKey(item.key))).isEqualTo(item)
        }
    }

    @Test
    fun `피부색 가족 국기 키캡을 각각 완전한 시퀀스로 허용한다`() {
        assertThat(catalog.require("u:1f44d-1f3fd").emoji).isEqualTo("👍🏽")
        assertThat(catalog.require("u:1f468-200d-1f469-200d-1f467-200d-1f466").emoji).isEqualTo("👨‍👩‍👧‍👦")
        assertThat(catalog.require("u:1f1f0-1f1f7").emoji).isEqualTo("🇰🇷")
        assertThat(catalog.require("u:31-fe0f-20e3").emoji).isEqualTo("1️⃣")
    }

    @Test
    fun `일반 문자 component 비정규 표기와 여러 이모지 연결은 거절한다`() {
        listOf("u:61", "u:1f3fd", "u:2764", "u:1F600", "u:01f600", "u:1f600-1f600", "HEART", "😀")
            .forEach { key ->
                assertThatThrownBy { catalog.require(key) }.isInstanceOf(InvalidEmojiException::class.java)
            }
    }

    @Test
    fun `기존 웹 클라이언트의 하트 웃음 매핑과 저장 키를 보존한다`() {
        assertThat(catalog.legacyKey(ReactionType.HEART)).isEqualTo("u:2764-fe0f")
        assertThat(catalog.legacyKey(ReactionType.SMILE)).isEqualTo("u:1f600")
        assertThat(catalog.toStorageKey("u:2764-fe0f")).isEqualTo("HEART")
        assertThat(catalog.toStorageKey("u:1f600")).isEqualTo("SMILE")
        assertThat(catalog.toStorageKey("u:1f44d")).isEqualTo("u:1f44d")
    }
}
