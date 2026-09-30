package com.peakda.server.domain.weather.application

/** 화면에 쓰는 하늘 상태. 뒤에 선언할수록 방문에 불리하다(하루 대표값을 고를 때 더 나쁜 쪽을 택한다). */
enum class WeatherSky {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    SHOWER,
    RAIN,
    RAIN_SNOW,
    SNOW,
    ;

    val precipitating: Boolean
        get() = this >= SHOWER

    companion object {
        /** 단기예보 SKY 코드 (1 맑음, 3 구름많음, 4 흐림). */
        fun fromShortSkyCode(code: String): WeatherSky? = when (code.trim()) {
            "1" -> CLEAR
            "3" -> PARTLY_CLOUDY
            "4" -> CLOUDY
            else -> null
        }

        /** 단기예보 PTY 코드 (0 없음, 1 비, 2 비/눈, 3 눈, 4 소나기). 강수가 없으면 null. */
        fun fromPrecipitationCode(code: String): WeatherSky? = when (code.trim()) {
            "1" -> RAIN
            "2" -> RAIN_SNOW
            "3" -> SNOW
            "4" -> SHOWER
            else -> null
        }

        /** 중기예보 날씨 문구 (예: `맑음`, `구름많고 비`, `흐리고 비/눈`, `구름많고 소나기`). */
        fun fromMidForecastText(text: String?): WeatherSky? {
            val value = text?.trim().orEmpty()
            if (value.isEmpty()) return null
            return when {
                "비/눈" in value || "눈/비" in value -> RAIN_SNOW
                "소나기" in value -> SHOWER
                "비" in value -> RAIN
                "눈" in value -> SNOW
                value.startsWith("흐") -> CLOUDY
                value.startsWith("구름") -> PARTLY_CLOUDY
                value.startsWith("맑") -> CLEAR
                else -> null
            }
        }
    }
}
