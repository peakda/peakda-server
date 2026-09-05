package com.peakda.server.infrastructure.external.kma.midfcst

enum class MidRegionCode(
    val displayName: String,
    val landRegId: String,
    val temperatureRegId: String,
    val seaRegId: String? = null,
) {
    SEOUL("서울", "11B00000", "11B10101", null),
    INCHEON("인천", "11B00000", "11B20201", "12A20000"),
    GYEONGGI("경기도", "11B00000", "11B20601", null),
    GANGWON_YEONGSEO("강원 영서", "11D10000", "11D10301", null),
    GANGWON_YEONGDONG("강원 영동", "11D20000", "11D20501", "12C20000"),
    DAEJEON("대전", "11C20000", "11C20401", null),
    SEJONG("세종", "11C20000", "11C20404", null),
    CHUNGBUK("충청북도", "11C10000", "11C10301", null),
    CHUNGNAM("충청남도", "11C20000", "11C20101", "12A20000"),
    GWANGJU("광주", "11F20000", "11F20501", null),
    JEONBUK("전라북도", "11F10000", "11F10201", "12B10000"),
    JEONNAM("전라남도", "11F20000", "11F20401", "12B10000"),
    DAEGU("대구", "11H10000", "11H10701", null),
    GYEONGBUK("경상북도", "11H10000", "11H10501", "12C10000"),
    BUSAN("부산", "11H20000", "11H20201", "12C10000"),
    ULSAN("울산", "11H20000", "11H20101", "12C10000"),
    GYEONGNAM("경상남도", "11H20000", "11H20301", "12C10000"),
    JEJU("제주", "11G00000", "11G00201", "12G00000");

    companion object {
        fun fromDisplayName(displayName: String): MidRegionCode? {
            return entries.firstOrNull { it.displayName == displayName }
        }
    }
}
