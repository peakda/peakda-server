package com.peakda.server.domain.weather.application

import com.peakda.server.infrastructure.external.kma.midfcst.MidRegionCode

/**
 * 명소의 법정동 코드(행정구역 개편 후)로 중기예보 구역을 정한다.
 * 반환값은 `weather_mid_forecasts.region_code` 에 저장되는 [MidRegionCode] 이름이다.
 */
object MidForecastRegionResolver {
    /** 강원 영동 시군구(강릉·동해·태백·속초·삼척·고성·양양). 나머지 강원은 영서. */
    private val GANGWON_YEONGDONG_SUFFIXES = setOf("150", "170", "190", "210", "230", "820", "830")

    /** 전남광주통합특별시(12) 가운데 옛 광주광역시 자치구(동·서·남·북·광산구). 나머지는 옛 전남. */
    private val GWANGJU_DISTRICTS = setOf("12210", "12240", "12270", "12300", "12330")

    private val BY_AREA: Map<String, MidRegionCode> = mapOf(
        "11" to MidRegionCode.SEOUL,
        "26" to MidRegionCode.BUSAN,
        "27" to MidRegionCode.DAEGU,
        "28" to MidRegionCode.INCHEON,
        "29" to MidRegionCode.GWANGJU,
        "30" to MidRegionCode.DAEJEON,
        "31" to MidRegionCode.ULSAN,
        "36" to MidRegionCode.SEJONG,
        "41" to MidRegionCode.GYEONGGI,
        "43" to MidRegionCode.CHUNGBUK,
        "44" to MidRegionCode.CHUNGNAM,
        "45" to MidRegionCode.JEONBUK,
        "52" to MidRegionCode.JEONBUK,
        "46" to MidRegionCode.JEONNAM,
        "47" to MidRegionCode.GYEONGBUK,
        "48" to MidRegionCode.GYEONGNAM,
        "50" to MidRegionCode.JEJU,
    )

    fun resolve(legalDongAreaCode: String?, legalDongSigunguCode: String?): String? {
        val area = legalDongAreaCode ?: legalDongSigunguCode?.take(2) ?: return null
        val region = when (area) {
            "42", "51" -> if (legalDongSigunguCode?.drop(2) in GANGWON_YEONGDONG_SUFFIXES) {
                MidRegionCode.GANGWON_YEONGDONG
            } else {
                MidRegionCode.GANGWON_YEONGSEO
            }
            "12" -> if (legalDongSigunguCode in GWANGJU_DISTRICTS) MidRegionCode.GWANGJU else MidRegionCode.JEONNAM
            else -> BY_AREA[area]
        }
        return region?.name
    }
}
