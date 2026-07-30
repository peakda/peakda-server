package com.peakda.server.domain.seasonal.application

object GddAccumulator {
    /** 기준온도 초과분만 누적한다. 하루라도 음수 기여는 없다. */
    fun accumulate(temperatures: List<DailyTemperature>, tBase: Double): Double =
        temperatures.sumOf { temperature ->
            temperature.effectiveAverage
                ?.let { effectiveAverage -> maxOf(0.0, effectiveAverage - tBase) }
                ?: 0.0
        }
}
