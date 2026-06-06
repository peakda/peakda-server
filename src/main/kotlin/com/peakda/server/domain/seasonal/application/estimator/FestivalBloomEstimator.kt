package com.peakda.server.domain.seasonal.application.estimator

import com.peakda.server.domain.festival.entity.Festival
import com.peakda.server.domain.seasonal.application.BloomEstimation
import com.peakda.server.domain.seasonal.application.BloomEstimationContext
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Estimator
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 신호 B — 꽃축제 기반 추정기. 명소 근접(≤반경) + 이름이 카테고리 festivalHints 와 일치하는 축제로 상태를 판정한다.
 *
 * 축제 기간 = 절정 구간(PEAK). 시작 직전/종료 직후 윈도우는 STARTED/ENDED 이며, 후보가 여럿이면 가장 강한(상태 우선·신뢰도) 매칭을 채택한다.
 */
@Component
class FestivalBloomEstimator(
    private val properties: FestivalEstimatorProperties,
) : BloomEstimator {

    override val estimator = Estimator.FESTIVAL

    override fun estimate(context: BloomEstimationContext): BloomEstimation? {
        val lat = context.attraction.latitude ?: return null
        val lng = context.attraction.longitude ?: return null
        val radiusMeters = properties.proximityRadiusKm * METERS_PER_KM

        var best: BloomEstimation? = null
        for (festival in context.festivals) {
            if (!matchesCategory(festival.name, context.category)) continue
            val fLat = festival.latitude ?: continue
            val fLng = festival.longitude ?: continue
            if (haversine(lat, lng, fLat, fLng) > radiusMeters) continue
            val start = parseDate(festival.startDate) ?: continue
            val end = parseDate(festival.endDate) ?: start
            val estimation = classify(context.baseDate, start, end, festival) ?: continue
            best = stronger(best, estimation)
        }
        return best
    }

    private fun classify(baseDate: LocalDate, start: LocalDate, end: LocalDate, festival: Festival): BloomEstimation? {
        val startedFrom = start.minusDays(properties.earlyWindowDays)
        val endedTo = end.plusDays(properties.lateWindowDays)
        val (status, confidence) = when {
            !baseDate.isBefore(start) && !baseDate.isAfter(end) -> BloomStatus.PEAK to properties.peakConfidence
            !baseDate.isBefore(startedFrom) && baseDate.isBefore(start) -> BloomStatus.STARTED to properties.startedConfidence
            baseDate.isAfter(end) && !baseDate.isAfter(endedTo) -> BloomStatus.ENDED to properties.endedConfidence
            else -> return null
        }
        return BloomEstimation(
            estimator = Estimator.FESTIVAL,
            status = status,
            confidence = confidence,
            peakStartDate = start,
            peakEndDate = end,
            evidence = "festival:${festival.id},name:${festival.name}",
        )
    }

    /** 상태 우선순위(PEAK>STARTED>ENDED) 후 신뢰도로 더 강한 매칭을 고른다. */
    private fun stronger(current: BloomEstimation?, candidate: BloomEstimation): BloomEstimation {
        if (current == null) return candidate
        val byStatus = statusRank(candidate.status) - statusRank(current.status)
        return when {
            byStatus < 0 -> candidate
            byStatus > 0 -> current
            candidate.confidence > current.confidence -> candidate
            else -> current
        }
    }

    private fun matchesCategory(festivalName: String, category: BloomCategory): Boolean {
        val haystack = festivalName.lowercase()
        return category.festivalHints.any { haystack.contains(it.lowercase()) }
    }

    private fun parseDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        val digits = value.filter { it.isDigit() }
        if (digits.length != 8) return null
        return try {
            LocalDate.parse(digits, DateTimeFormatter.BASIC_ISO_DATE)
        } catch (e: DateTimeParseException) {
            null
        }
    }

    private fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2.0)
        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    companion object {
        private const val METERS_PER_KM = 1_000.0
        private const val EARTH_RADIUS_METERS = 6_371_000.0

        private fun statusRank(status: BloomStatus): Int = when (status) {
            BloomStatus.PEAK -> 0
            BloomStatus.STARTED -> 1
            BloomStatus.ENDED -> 2
            BloomStatus.PREPARING -> 3
        }
    }
}
