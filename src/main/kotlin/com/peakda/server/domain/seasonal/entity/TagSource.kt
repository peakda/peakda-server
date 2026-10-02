package com.peakda.server.domain.seasonal.entity

/**
 * [AttractionBloom] 태그가 만들어진 출처. 한 명소가 같은 카테고리에 대해 출처별로 여러 행을 가질 수 있다.
 *
 * [KEYWORD]·[FESTIVAL]·[CATEGORY]·[OBSERVATION]·[GALLERY] 는 태깅 배치가 매 실행 다시 만드는 자동 태그이고,
 * [MANUAL]·[EXIF_BOOST] 는 배치가 지우지 않는다.
 */
enum class TagSource {
    /** 명소 제목에 꽃 이름이 들어 있다. */
    KEYWORD,

    /** 꽃축제 장소명과 명소 제목이 일치한다. */
    FESTIVAL,

    /** TourAPI 소분류(국립공원·수목원 등)가 해당 꽃의 명소 유형이다. */
    CATEGORY,

    /** 기상청 유명산 단풍 관측 산에 속한 명소다. */
    OBSERVATION,

    /** 한국관광공사 관광사진 갤러리에 이 명소에서 찍은 그 꽃 사진이 있다. */
    GALLERY,
    MANUAL,
    EXIF_BOOST,
}
