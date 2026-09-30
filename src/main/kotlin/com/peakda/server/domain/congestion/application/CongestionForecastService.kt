package com.peakda.server.domain.congestion.application

import com.peakda.server.domain.congestion.entity.CongestionLinkStatus
import com.peakda.server.domain.congestion.repository.CongestionAttractionLinkRepository
import com.peakda.server.domain.congestion.repository.CongestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 명소에 확정 연결된 관광지의 일별 집중률 예측을 읽는다. */
@Service
class CongestionForecastService(
    private val linkRepository: CongestionAttractionLinkRepository,
    private val congestionRepository: CongestionRepository,
) {
    /**
     * [from]~[to] (양 끝 포함) 예측. 확정 연결이 없거나 예측이 없으면 빈 목록.
     * 한 명소에 관광지명이 여럿 연결돼 있으면 먼저 확정된 연결을 쓴다.
     */
    @Transactional(readOnly = true)
    fun findDailyForecast(attractionId: Long, from: LocalDate, to: LocalDate): List<DailyCongestion> {
        val link = linkRepository
            .findByAttractionIdAndStatusOrderByIdAsc(attractionId, CongestionLinkStatus.CONFIRMED)
            .firstOrNull()
            ?: return emptyList()
        return congestionRepository
            .findByAreaCodeAndSigunguCodeAndTouristAttractionNameAndBaseDateBetweenOrderByBaseDateAsc(
                link.areaCode,
                link.sigunguCode,
                link.touristAttractionName,
                from.format(BASIC),
                to.format(BASIC),
            )
            .mapNotNull { row ->
                val rate = row.congestionRate?.toDoubleOrNull() ?: return@mapNotNull null
                val date = runCatching { LocalDate.parse(row.baseDate, BASIC) }.getOrNull() ?: return@mapNotNull null
                DailyCongestion(date, rate)
            }
    }

    companion object {
        private val BASIC: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
    }
}
