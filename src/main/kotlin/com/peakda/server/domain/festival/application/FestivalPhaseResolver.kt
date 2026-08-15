package com.peakda.server.domain.festival.application

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 축제 상세·목록에서 동일한 날짜 경계와 종료 임박 기준을 적용한다. */
object FestivalPhaseResolver {
    fun resolve(
        startsOn: LocalDate?,
        endsOn: LocalDate?,
        today: LocalDate,
        endingSoonDays: Long,
    ): FestivalPhase? {
        if (startsOn == null) return null
        val effectiveEndsOn = endsOn ?: startsOn
        return when {
            today.isBefore(startsOn) -> FestivalPhase.UPCOMING
            today.isAfter(effectiveEndsOn) -> FestivalPhase.ENDED
            ChronoUnit.DAYS.between(today, effectiveEndsOn) <= endingSoonDays -> FestivalPhase.ENDING_SOON
            else -> FestivalPhase.ONGOING
        }
    }

    fun effectiveEndsOn(startsOn: LocalDate?, endsOn: LocalDate?): LocalDate? =
        startsOn?.let { endsOn ?: it }
}
