package com.peakda.server.domain.visittiming.presentation.response

import com.peakda.server.domain.festival.application.FestivalPhase
import com.peakda.server.domain.visittiming.application.CongestionLevel
import com.peakda.server.domain.visittiming.application.VisitReason
import com.peakda.server.domain.weather.application.WeatherSky
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(
    description = "방문 타이밍 — 개화 상태와 함께 볼 향후 혼잡도·날씨·주변 축제와 추천 방문일. " +
        "데이터가 없는 항목은 null(목록은 빈 배열)로 내려간다.",
)
data class VisitTimingResponse(
    @field:Schema(description = "향후 혼잡도 예측. 혼잡도 데이터가 연결되지 않은 명소는 null", nullable = true)
    val congestion: CongestionForecast?,

    @field:Schema(description = "향후 날씨. 예보 구역이 매핑되지 않은 명소는 null", nullable = true)
    val weather: WeatherForecast?,

    @field:Schema(description = "주변(기본 5km)에서 진행 중이거나 곧 시작하는 축제. 진행 중 우선, 가까운 순")
    val nearbyFestivals: List<NearbyFestivalItem>,

    @field:Schema(description = "추천 방문일. 근거가 부족하거나 꽃을 볼 수 있는 날이 없으면 null", nullable = true)
    val recommendation: Recommendation?,
) {
    @Schema(description = "일별 혼잡도 예측 (한국관광공사 관광지 집중률 기반, 일 단위)")
    data class CongestionForecast(
        @field:Schema(description = "오늘 혼잡도. 오늘 예측이 없으면 null", nullable = true)
        val today: DailyCongestion?,
        @field:Schema(description = "오늘부터 날짜순 예측")
        val days: List<DailyCongestion>,
        @field:Schema(description = "기간 중 가장 한산한 날", example = "2026-10-01", nullable = true)
        val quietestDate: LocalDate?,
    )

    @Schema(description = "하루 혼잡도")
    data class DailyCongestion(
        @field:Schema(description = "날짜", example = "2026-10-03")
        val date: LocalDate,
        @field:Schema(description = "집중률 (관광지 자체 기준 0~100 상대 지수, 방문객 수 아님)", example = "70.6")
        val rate: Double,
        @field:Schema(description = "혼잡 등급", example = "BUSY")
        val level: CongestionLevel,
    )

    @Schema(description = "향후 날씨 (3일 이내 단기예보, 이후 중기예보)")
    data class WeatherForecast(
        @field:Schema(description = "오늘부터 날짜순 일별 요약")
        val days: List<DailyWeather>,
        @field:Schema(description = "3일 이내 비·눈이 이어지는 시간 구간 (현재 이후만)")
        val rainWindows: List<RainWindow>,
    )

    @Schema(description = "하루 날씨")
    data class DailyWeather(
        @field:Schema(description = "날짜", example = "2026-10-01")
        val date: LocalDate,
        @field:Schema(description = "하늘 상태 (낮 시간대 대표값)", example = "CLEAR", nullable = true)
        val sky: WeatherSky?,
        @field:Schema(description = "낮 시간대 최대 강수확률(%)", example = "20", nullable = true)
        val precipitationProbability: Int?,
        @field:Schema(description = "최저기온(℃)", example = "12.0", nullable = true)
        val minTemperature: Double?,
        @field:Schema(description = "최고기온(℃)", example = "22.0", nullable = true)
        val maxTemperature: Double?,
    )

    @Schema(description = "비·눈 구간 (start 포함, end 제외)")
    data class RainWindow(
        @field:Schema(description = "시작 시각", example = "2026-10-02T06:00:00")
        val start: LocalDateTime,
        @field:Schema(description = "종료 시각", example = "2026-10-02T12:00:00")
        val end: LocalDateTime,
        @field:Schema(description = "강수 형태", example = "RAIN")
        val sky: WeatherSky,
    )

    @Schema(description = "주변 축제")
    data class NearbyFestivalItem(
        @field:Schema(description = "축제 id", example = "301")
        val festivalId: Long,
        @field:Schema(description = "축제명", example = "진해군항제")
        val name: String,
        @field:Schema(description = "진행 상태", example = "ONGOING")
        val phase: FestivalPhase,
        @field:Schema(description = "시작일", example = "2026-03-27")
        val startsOn: LocalDate,
        @field:Schema(description = "종료일", example = "2026-04-05")
        val endsOn: LocalDate,
        @field:Schema(description = "명소로부터 거리(m)", example = "420")
        val distanceMeters: Int,
    )

    @Schema(description = "추천 방문일과 이유")
    data class Recommendation(
        @field:Schema(description = "추천 방문일", example = "2026-10-01")
        val date: LocalDate,
        @field:Schema(description = "추천 이유 코드. 문장 조합은 클라이언트가 한다", example = "[\"BLOOM_PEAK\", \"LEAST_CROWDED\"]")
        val reasons: List<VisitReason>,
    )
}
