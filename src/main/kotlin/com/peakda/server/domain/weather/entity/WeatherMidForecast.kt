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
    uniqueConstraints = [UniqueConstraint(name = "uk_weather_mid_forecasts_reg_tmfc", columnNames = ["reg_id", "tm_fc"])],
)
class WeatherMidForecast(
    @Column(name = "reg_id", nullable = false, columnDefinition = "TEXT")
    val regId: String,

    @Column(name = "tm_fc", nullable = false, columnDefinition = "TEXT")
    val tmFc: String,

    @Column(name = "wf3_am", columnDefinition = "TEXT") var wf3Am: String? = null,
    @Column(name = "wf3_pm", columnDefinition = "TEXT") var wf3Pm: String? = null,
    @Column(name = "wf4_am", columnDefinition = "TEXT") var wf4Am: String? = null,
    @Column(name = "wf4_pm", columnDefinition = "TEXT") var wf4Pm: String? = null,
    @Column(name = "wf5_am", columnDefinition = "TEXT") var wf5Am: String? = null,
    @Column(name = "wf5_pm", columnDefinition = "TEXT") var wf5Pm: String? = null,
    @Column(name = "wf6_am", columnDefinition = "TEXT") var wf6Am: String? = null,
    @Column(name = "wf6_pm", columnDefinition = "TEXT") var wf6Pm: String? = null,
    @Column(name = "wf7_am", columnDefinition = "TEXT") var wf7Am: String? = null,
    @Column(name = "wf7_pm", columnDefinition = "TEXT") var wf7Pm: String? = null,
    @Column(name = "wf8", columnDefinition = "TEXT") var wf8: String? = null,
    @Column(name = "wf9", columnDefinition = "TEXT") var wf9: String? = null,
    @Column(name = "wf10", columnDefinition = "TEXT") var wf10: String? = null,

    @Column(name = "rn_st3_am") var rnSt3Am: Int? = null,
    @Column(name = "rn_st3_pm") var rnSt3Pm: Int? = null,
    @Column(name = "rn_st4_am") var rnSt4Am: Int? = null,
    @Column(name = "rn_st4_pm") var rnSt4Pm: Int? = null,
    @Column(name = "rn_st5_am") var rnSt5Am: Int? = null,
    @Column(name = "rn_st5_pm") var rnSt5Pm: Int? = null,
    @Column(name = "rn_st6_am") var rnSt6Am: Int? = null,
    @Column(name = "rn_st6_pm") var rnSt6Pm: Int? = null,
    @Column(name = "rn_st7_am") var rnSt7Am: Int? = null,
    @Column(name = "rn_st7_pm") var rnSt7Pm: Int? = null,
    @Column(name = "rn_st8") var rnSt8: Int? = null,
    @Column(name = "rn_st9") var rnSt9: Int? = null,
    @Column(name = "rn_st10") var rnSt10: Int? = null,

    @Column(name = "ta_min3") var taMin3: Int? = null,
    @Column(name = "ta_max3") var taMax3: Int? = null,
    @Column(name = "ta_min4") var taMin4: Int? = null,
    @Column(name = "ta_max4") var taMax4: Int? = null,
    @Column(name = "ta_min5") var taMin5: Int? = null,
    @Column(name = "ta_max5") var taMax5: Int? = null,
    @Column(name = "ta_min6") var taMin6: Int? = null,
    @Column(name = "ta_max6") var taMax6: Int? = null,
    @Column(name = "ta_min7") var taMin7: Int? = null,
    @Column(name = "ta_max7") var taMax7: Int? = null,
    @Column(name = "ta_min8") var taMin8: Int? = null,
    @Column(name = "ta_max8") var taMax8: Int? = null,
    @Column(name = "ta_min9") var taMin9: Int? = null,
    @Column(name = "ta_max9") var taMax9: Int? = null,
    @Column(name = "ta_min10") var taMin10: Int? = null,
    @Column(name = "ta_max10") var taMax10: Int? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
