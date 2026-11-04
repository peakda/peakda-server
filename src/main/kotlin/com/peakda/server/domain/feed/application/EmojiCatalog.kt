package com.peakda.server.domain.feed.application

import com.peakda.server.domain.spot.entity.ReactionType
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.security.MessageDigest

/** 버전이 고정된 Unicode 키만 허용한다. 런타임에 외부 API를 호출하지 않는다. */
@Component
class EmojiCatalog {
    val version: String = "17.0"
    private val source = ClassPathResource("emoji/emoji-test-$version.txt").inputStream.use { it.readBytes() }
    val etag: String = "\"" + MessageDigest.getInstance("SHA-256")
        .digest(source).joinToString("") { "%02x".format(it) } + "\""
    val items: List<EmojiDefinition> = parse(source.toString(Charsets.UTF_8))
    private val byKey = items.associateBy { it.key }

    init {
        check(items.isNotEmpty() && byKey.size == items.size) { "Emoji catalog must contain unique keys" }
        legacyKeys.values.forEach { check(it in byKey) { "Missing legacy emoji: $it" } }
    }

    fun require(key: String): EmojiDefinition = byKey[key] ?: throw InvalidEmojiException()

    /** 원래 두 종류는 기존 TEXT 값으로 저장해 v1/v2가 같은 유니크 키를 사용한다. */
    fun toStorageKey(key: String): String {
        require(key)
        return legacyKeys.entries.firstOrNull { it.value == key }?.key?.name ?: key
    }

    fun fromStorageKey(storageKey: String): EmojiDefinition {
        val key = legacyKeys.entries.firstOrNull { it.key.name == storageKey }?.value ?: storageKey
        return checkNotNull(byKey[key]) { "Stored reaction is missing from the emoji catalog" }
    }

    fun legacyKey(type: ReactionType): String = legacyKeys.getValue(type)

    private fun parse(text: String): List<EmojiDefinition> {
        check(text.lineSequence().any { it == "# Version: $version" }) { "Unexpected Unicode Emoji version" }
        var group = ""
        return buildList {
            text.lineSequence().forEach { line ->
                if (line.startsWith("# group: ")) group = line.removePrefix("# group: ")
                val data = line.substringBefore('#').trim()
                if (data.isNotEmpty() && data.substringAfter(';').trim() == "fully-qualified") {
                    val points = data.substringBefore(';').trim().split(Regex("\\s+")).map { it.toInt(16) }
                    val key = "u:" + points.joinToString("-") { it.toString(16) }
                    val emoji = buildString { points.forEach { appendCodePoint(it) } }
                    check(group.isNotBlank()) { "Missing emoji group" }
                    add(EmojiDefinition(key, emoji, group))
                }
            }
        }
    }

    companion object {
        private val legacyKeys = mapOf(
            ReactionType.HEART to "u:2764-fe0f",
            ReactionType.SMILE to "u:1f600",
        )
    }
}
