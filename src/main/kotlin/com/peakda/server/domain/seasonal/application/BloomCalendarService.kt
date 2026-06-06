package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.festival.repository.FestivalRepository
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.presentation.response.BloomCalendarResponse
import com.peakda.server.domain.seasonal.presentation.response.BloomCalendarResponse.BloomCalendarDay
import com.peakda.server.domain.spot.exception.AttractionNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

/**
 * 단일 명소×카테고리의 향후 [BloomCalendarProperties.calendarHorizonDays] 일 예상 만개 캘린더를 온디맨드로 시뮬레이션한다.
 *
 * 저장하지 않으며(스팟 상세 1건 조회용), 각 일자를 융합 추정기로 돌려 일별 상태 타임라인과 대표 절정 구간을 만든다.
 */
@Service
class BloomCalendarService(
    private val attractionRepository: AttractionRepository,
    private val festivalRepository: FestivalRepository,
    private val fusionService: BloomStatusFusionService,
    private val properties: BloomCalendarProperties,
) {
    @Transactional(readOnly = true)
    fun getCalendar(attractionId: Long, category: BloomCategory): BloomCalendarResponse {
        val attraction = attractionRepository.findById(attractionId)
            .orElseThrow { AttractionNotFoundException() }
        val festivals = festivalRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull()
        val today = LocalDate.now(KST)

        var representative: BloomEstimation? = null
        val days = (0 until properties.calendarHorizonDays).map { offset ->
            val date = today.plusDays(offset)
            val estimation = fusionService.fuse(BloomEstimationContext(attraction, category, date, festivals))
            if (offset == 0L) representative = estimation
            BloomCalendarDay(date = date, status = estimation?.status ?: BloomStatus.PREPARING)
        }

        return BloomCalendarResponse(
            attractionId = attractionId,
            category = category,
            displayName = category.displayName,
            peakStartDate = representative?.peakStartDate,
            peakEndDate = representative?.peakEndDate,
            peakDurationDays = representative?.peakDurationDays,
            days = days,
        )
    }

    companion object {
        private val KST = ZoneId.of("Asia/Seoul")
    }
}
