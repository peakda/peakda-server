package com.peakda.server.domain.feed.presentation.v2.response

import io.swagger.v3.oas.annotations.media.Schema

data class EmojiCatalogResponse(
    @field:Schema(description = "Unicode Emoji 데이터 버전", example = "17.0")
    val version: String,
    @field:Schema(description = "허용되는 모든 기본 이모지. 피커에서 이 키를 사용한다")
    val items: List<EmojiDefinitionResponse>,
)
