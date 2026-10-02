package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.seasonal.entity.BloomCategory

/**
 * 꽃 근거가 되는 관광사진 한 장. 제목·검색 키워드에 꽃 이름이 있는 사진만 만든다.
 *
 * 비교 문자열은 모두 공백을 지우고 소문자로 맞춘다([FestivalPlaceTokenizer.normalize]).
 */
data class GalleryFlowerPhoto(
    val contentId: String,
    val categories: Set<BloomCategory>,
    /** 제목과 키워드를 쉼표로 이은 문자열. */
    val haystack: String,
    /** 쉼표까지 지운 문자열. 이웃 키워드로 나뉜 명소 이름(`진해, 여좌천`)을 찾는다. */
    val joinedHaystack: String,
    /** 제목과 키워드 하나하나. 두 글자 명소 이름은 이 중 하나와 정확히 같아야 한다. */
    val tokens: Set<String>,
    /** 촬영 장소 첫 조각(시·도). */
    val sido: String,
    /** 촬영 장소 둘째 조각(시·군·구). */
    val sigungu: String,
) {
    /** [normalizedTitle] 명소 이름이 이 사진에 나오는지. */
    fun mentions(normalizedTitle: String): Boolean =
        if (normalizedTitle.length >= SUBSTRING_TITLE_LENGTH) {
            haystack.contains(normalizedTitle) || joinedHaystack.contains(normalizedTitle)
        } else {
            normalizedTitle in tokens
        }

    companion object {
        /** 이보다 짧은 명소 이름은 다른 단어의 일부(`암태도` ⊃ `태도`)와 헷갈려 키워드와 정확히 같을 때만 인정한다. */
        private const val SUBSTRING_TITLE_LENGTH = 3
        private val WHITESPACE = Regex("""\s+""")

        /** 꽃 이름이 없거나 제외어(예: `여수국가` ⊃ `수국`)에만 걸리는 사진이면 null. */
        fun of(contentId: String, title: String?, keywords: String?, location: String?): GalleryFlowerPhoto? {
            val raw = listOfNotNull(title, keywords).joinToString(KEYWORD_DELIMITER)
            val haystack = FestivalPlaceTokenizer.normalize(raw)
            val categories = BloomCategory.entries.filter { category ->
                category.keywordHints.any { haystack.contains(FestivalPlaceTokenizer.normalize(it)) } &&
                    category.keywordExclusions.none { haystack.contains(FestivalPlaceTokenizer.normalize(it)) }
            }.toSet()
            if (categories.isEmpty()) return null

            val locationParts = location.orEmpty().trim().split(WHITESPACE)
            return GalleryFlowerPhoto(
                contentId = contentId,
                categories = categories,
                haystack = haystack,
                joinedHaystack = haystack.replace(KEYWORD_DELIMITER, ""),
                tokens = raw.split(KEYWORD_DELIMITER).map(FestivalPlaceTokenizer::normalize).filter { it.isNotEmpty() }.toSet(),
                sido = locationParts.getOrElse(0) { "" }.trimEnd(','),
                sigungu = locationParts.getOrElse(1) { "" }.trimEnd(','),
            )
        }

        private const val KEYWORD_DELIMITER = ","
    }
}
