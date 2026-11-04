package com.peakda.server.domain.feed.presentation.v2.response

import io.swagger.v3.oas.annotations.media.Schema

data class EmojiDefinitionResponse(
    @field:Schema(description = "API에서 사용할 이모지 키", example = "u:1f44d")
    val key: String,
    @field:Schema(description = "완전한 Unicode 이모지 시퀀스", example = "👍")
    val emoji: String,
    @field:Schema(description = "Unicode 이모지 그룹", example = "People & Body")
    val group: String,
)
