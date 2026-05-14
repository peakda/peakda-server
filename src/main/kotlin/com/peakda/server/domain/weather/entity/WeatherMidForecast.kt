package com.peakda.server.domain.weather.entity

import com.peakda.server.global.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "weather_mid_forecasts",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_weather_mid_forecasts_region_announce",
            columnNames = ["region_code", "announce_time"],
        ),
    ],
)
class WeatherMidForecast(
    @Column(name = "region_code", nullable = false, columnDefinition = "TEXT")
    val regionCode: String,

    @Column(name = "announce_time", nullable = false, columnDefinition = "TEXT")
    val announceTime: String,

    @Column(name = "weather_day3_am", columnDefinition = "TEXT") var weatherDay3Am: String? = null,
    @Column(name = "weather_day3_pm", columnDefinition = "TEXT") var weatherDay3Pm: String? = null,
    @Column(name = "weather_day4_am", columnDefinition = "TEXT") var weatherDay4Am: String? = null,
    @Column(name = "weather_day4_pm", columnDefinition = "TEXT") var weatherDay4Pm: String? = null,
    @Column(name = "weather_day5_am", columnDefinition = "TEXT") var weatherDay5Am: String? = null,
    @Column(name = "weather_day5_pm", columnDefinition = "TEXT") var weatherDay5Pm: String? = null,
    @Column(name = "weather_day6_am", columnDefinition = "TEXT") var weatherDay6Am: String? = null,
    @Column(name = "weather_day6_pm", columnDefinition = "TEXT") var weatherDay6Pm: String? = null,
    @Column(name = "weather_day7_am", columnDefinition = "TEXT") var weatherDay7Am: String? = null,
    @Column(name = "weather_day7_pm", columnDefinition = "TEXT") var weatherDay7Pm: String? = null,
    @Column(name = "weather_day8", columnDefinition = "TEXT") var weatherDay8: String? = null,
    @Column(name = "weather_day9", columnDefinition = "TEXT") var weatherDay9: String? = null,
    @Column(name = "weather_day10", columnDefinition = "TEXT") var weatherDay10: String? = null,

    @Column(name = "rain_probability_day3_am") var rainProbabilityDay3Am: Int? = null,
    @Column(name = "rain_probability_day3_pm") var rainProbabilityDay3Pm: Int? = null,
    @Column(name = "rain_probability_day4_am") var rainProbabilityDay4Am: Int? = null,
    @Column(name = "rain_probability_day4_pm") var rainProbabilityDay4Pm: Int? = null,
    @Column(name = "rain_probability_day5_am") var rainProbabilityDay5Am: Int? = null,
    @Column(name = "rain_probability_day5_pm") var rainProbabilityDay5Pm: Int? = null,
    @Column(name = "rain_probability_day6_am") var rainProbabilityDay6Am: Int? = null,
    @Column(name = "rain_probability_day6_pm") var rainProbabilityDay6Pm: Int? = null,
    @Column(name = "rain_probability_day7_am") var rainProbabilityDay7Am: Int? = null,
    @Column(name = "rain_probability_day7_pm") var rainProbabilityDay7Pm: Int? = null,
    @Column(name = "rain_probability_day8") var rainProbabilityDay8: Int? = null,
    @Column(name = "rain_probability_day9") var rainProbabilityDay9: Int? = null,
    @Column(name = "rain_probability_day10") var rainProbabilityDay10: Int? = null,

    @Column(name = "temperature_min_day3") var temperatureMinDay3: Int? = null,
    @Column(name = "temperature_max_day3") var temperatureMaxDay3: Int? = null,
    @Column(name = "temperature_min_day4") var temperatureMinDay4: Int? = null,
    @Column(name = "temperature_max_day4") var temperatureMaxDay4: Int? = null,
    @Column(name = "temperature_min_day5") var temperatureMinDay5: Int? = null,
    @Column(name = "temperature_max_day5") var temperatureMaxDay5: Int? = null,
    @Column(name = "temperature_min_day6") var temperatureMinDay6: Int? = null,
    @Column(name = "temperature_max_day6") var temperatureMaxDay6: Int? = null,
    @Column(name = "temperature_min_day7") var temperatureMinDay7: Int? = null,
    @Column(name = "temperature_max_day7") var temperatureMaxDay7: Int? = null,
    @Column(name = "temperature_min_day8") var temperatureMinDay8: Int? = null,
    @Column(name = "temperature_max_day8") var temperatureMaxDay8: Int? = null,
    @Column(name = "temperature_min_day9") var temperatureMinDay9: Int? = null,
    @Column(name = "temperature_max_day9") var temperatureMaxDay9: Int? = null,
    @Column(name = "temperature_min_day10") var temperatureMinDay10: Int? = null,
    @Column(name = "temperature_max_day10") var temperatureMaxDay10: Int? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
