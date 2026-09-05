package com.peakda.server.domain.seasonal.entity

/**
 * [AttractionBloom] 태그가 만들어진 출처. 한 명소가 같은 카테고리에 대해 출처별로 여러 행을 가질 수 있다.
 */
enum class TagSource {
    KEYWORD,
    FESTIVAL,
    MANUAL,
    EXIF_BOOST,
}
