package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.festival.entity.Festival
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateUpsertCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Q1 산출 — 한 카테고리의 명소 묶음을 추정·융합해 [SeasonalBloomEstimate] 로 upsert 한다.
 *
 * 배치 잡이 카테고리별 페이지 단위로 [estimatePage] 를 호출하며, 페이지마다 별도 트랜잭션으로 커밋한다(단일 거대 트랜잭션 회피).
 */
@Service
class BloomEstimateService(
    private val attractionRepository: AttractionRepository,
    private val seasonalBloomEstimateRepository: SeasonalBloomEstimateRepository,
    private val fusionService: BloomStatusFusionService,
) {
    /** 주어진 명소 id 묶음을 융합 추정해 upsert 하고 적재한 행 수를 반환. festivals 는 사전 로드된 좌표 보유 축제 목록. */
    @Transactional
    fun estimatePage(
        attractionIds: List<Long>,
        category: BloomCategory,
        baseDate: LocalDate,
        festivals: List<Festival>,
        gdd: Map<Long, GddSnapshot>,
        observations: Map<Long, ObservationSnapshot> = emptyMap(),
    ): Int {
        var count = 0
        for (attraction in attractionRepository.findAllById(attractionIds)) {
            val attractionId = attraction.id ?: continue
            val context = BloomEstimationContext(
                attraction = attraction,
                category = category,
                baseDate = baseDate,
                festivals = festivals,
                gdd = gdd[attractionId],
                observation = observations[attractionId],
            )
            val estimation = fusionService.fuse(context) ?: continue
            seasonalBloomEstimateRepository.upsert(
                SeasonalBloomEstimateUpsertCommand(
                    attractionId = attractionId,
                    bloomCategory = category.name,
                    baseDate = baseDate,
                    status = estimation.status.name,
                    confidence = estimation.confidence,
                    chosenEstimator = estimation.estimator.name,
                    peakStartDate = estimation.peakStartDate,
                    peakEndDate = estimation.peakEndDate,
                    peakDurationDays = estimation.peakDurationDays,
                    gddRatio = estimation.gddRatio,
                    evidence = estimation.evidence,
                ),
            )
            count++
        }
        return count
    }
}
