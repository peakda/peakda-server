package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.seasonal.entity.BloomCategory

/**
 * 관광사진 갤러리의 꽃 사진을 촬영 시·군·구별로 묶은 색인. 태깅 배치 실행마다 한 번 만든다.
 *
 * 명소는 같은 시·군·구에서 찍힌 꽃 사진의 제목·키워드에 명소 이름(괄호 부분 제외)이 나오면 그 꽃의 근거를 얻는다.
 * 광역시의 구(`남구`)는 여러 시에 있으므로 시·도 이름 앞 두 글자까지 명소 주소에 있어야 한다.
 */
class GalleryFlowerIndex(photos: List<GalleryFlowerPhoto>) {

    val photoCount: Int = photos.size

    private val photosBySigungu: Map<String, List<GalleryFlowerPhoto>> = photos
        .filter { it.sigungu.isNotEmpty() }
        .groupBy { it.sigungu }

    /** 명소의 꽃별 근거 사진 id. 사진이 여러 장이면 id 가 가장 작은 사진을 근거로 남긴다. */
    fun match(attraction: Attraction): Map<BloomCategory, String> {
        val address = attraction.addressMain ?: return emptyMap()
        val title = FestivalPlaceTokenizer.normalize(attraction.title.replace(PARENTHESIZED, ""))
        if (title.length < MIN_TITLE_LENGTH) return emptyMap()

        val evidence = mutableMapOf<BloomCategory, String>()
        for ((sigungu, photos) in photosBySigungu) {
            if (!address.contains(sigungu)) continue
            for (photo in photos) {
                if (sigungu.endsWith(DISTRICT_SUFFIX) && !address.contains(photo.sido.take(SIDO_PREFIX_LENGTH))) continue
                if (!photo.mentions(title)) continue
                photo.categories.forEach { category -> evidence.merge(category, photo.contentId, ::minOf) }
            }
        }
        return evidence
    }

    companion object {
        private val PARENTHESIZED = Regex("""\([^)]*\)""")
        private const val MIN_TITLE_LENGTH = 2
        private const val DISTRICT_SUFFIX = "구"
        private const val SIDO_PREFIX_LENGTH = 2
    }
}
