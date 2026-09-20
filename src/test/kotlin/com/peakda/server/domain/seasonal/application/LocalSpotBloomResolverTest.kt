package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.seasonal.application.estimator.UserRecordEstimatorProperties
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.spot.entity.BloomStage
import com.peakda.server.domain.spot.entity.Plant
import com.peakda.server.domain.spot.entity.PlantStatus
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordPlant
import com.peakda.server.domain.spot.entity.SpotRecordPlantId
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.repository.PlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordPlantRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class LocalSpotBloomResolverTest {

    private val spotRecordPlantRepository = mock(SpotRecordPlantRepository::class.java)
    private val plantRepository = mock(PlantRepository::class.java)

    private val resolver = resolverOn(TODAY)

    @Test
    fun `카테고리별로 가장 최근 기록의 단계를 상태로 환산한다`() {
        val older = record(1L, visitedDate = LocalDate.of(2026, 3, 20), stage = BloomStage.EARLY)
        val newer = record(2L, visitedDate = LocalDate.of(2026, 4, 1), stage = BloomStage.STARTING)
        stubCherry(older, newer)

        val signals = resolver.resolve(listOf(older, newer))

        assertThat(signals).containsOnlyKeys(SPOT_ID)
        assertThat(signals.getValue(SPOT_ID))
            .containsExactly(LocalBloomSignal(BloomCategory.CHERRY, BloomStatus.STARTED))
    }

    @Test
    fun `최신 기록이 LATE 면 과거 절정 기록으로 되돌아가지 않고 늦었다로 남는다`() {
        val peak = record(1L, visitedDate = LocalDate.of(2026, 4, 1), stage = BloomStage.PEAK)
        val late = record(2L, visitedDate = LocalDate.of(2026, 4, 9), stage = BloomStage.LATE)
        stubCherry(peak, late)

        val signals = resolver.resolve(listOf(peak, late))

        assertThat(signals.getValue(SPOT_ID))
            .containsExactly(LocalBloomSignal(BloomCategory.CHERRY, BloomStatus.ENDED))
    }

    @Test
    fun `방문일이 같으면 나중에 작성한 기록이 이긴다`() {
        val sameDay = LocalDate.of(2026, 4, 1)
        val first = record(
            id = 1L,
            visitedDate = sameDay,
            stage = BloomStage.PEAK,
            createdAt = Instant.parse("2026-04-01T01:00:00Z"),
        )
        val second = record(
            id = 2L,
            visitedDate = sameDay,
            stage = BloomStage.EARLY,
            createdAt = Instant.parse("2026-04-01T09:00:00Z"),
        )
        stubCherry(first, second)

        // 레포지토리는 정렬을 보장하지 않으므로 오래된 기록이 먼저 오는 순서로 넘긴다.
        val signals = resolver.resolve(listOf(first, second))

        assertThat(signals.getValue(SPOT_ID))
            .containsExactly(LocalBloomSignal(BloomCategory.CHERRY, BloomStatus.PREPARING))
    }

    @Test
    fun `카테고리를 알 수 없는 식물만 달린 기록은 신호를 만들지 않는다`() {
        val untagged = record(1L, visitedDate = LocalDate.of(2026, 4, 1), stage = BloomStage.PEAK)
        `when`(spotRecordPlantRepository.findByIdSpotRecordIdIn(listOf(1L)))
            .thenReturn(listOf(SpotRecordPlant(SpotRecordPlantId(1L, PLANT_ID))))
        `when`(plantRepository.findAllById(setOf(PLANT_ID)))
            .thenReturn(listOf(plant(PLANT_ID, category = null)))

        assertThat(resolver.resolve(listOf(untagged))).isEmpty()
    }

    @Test
    fun `관측일이 유효 기간을 넘긴 기록은 현재 상태의 근거로 쓰지 않는다`() {
        // 봄(3월)에 절정이었다고 가을(9월)에 올린 기록 — 9월 스팟 상태를 절정으로 만들면 안 된다.
        val spring = record(1L, visitedDate = LocalDate.of(2026, 3, 28), stage = BloomStage.PEAK)
        stubCherry(spring)

        val signals = resolverOn(LocalDate.of(2026, 9, 16)).resolve(listOf(spring))

        assertThat(signals).isEmpty()
    }

    @Test
    fun `유효 기간 경계일의 기록은 아직 인정한다`() {
        val observed = LocalDate.of(2026, 4, 1)
        val record = record(1L, visitedDate = observed, stage = BloomStage.PEAK)
        stubCherry(record)

        val lastValidDay = observed.plusDays(UserRecordEstimatorProperties().maxAgeDays)
        val signals = resolverOn(lastValidDay).resolve(listOf(record))

        assertThat(signals.getValue(SPOT_ID))
            .containsExactly(LocalBloomSignal(BloomCategory.CHERRY, BloomStatus.PEAK))
        assertThat(resolverOn(lastValidDay.plusDays(1)).resolve(listOf(record))).isEmpty()
    }

    @Test
    fun `철 지난 절정 기록이 있어도 신선한 기록이 있으면 그 기록을 따른다`() {
        val spring = record(1L, visitedDate = LocalDate.of(2026, 3, 28), stage = BloomStage.PEAK)
        val recent = record(2L, visitedDate = LocalDate.of(2026, 9, 14), stage = BloomStage.STARTING)
        stubCherry(spring, recent)

        val signals = resolverOn(LocalDate.of(2026, 9, 16)).resolve(listOf(spring, recent))

        assertThat(signals.getValue(SPOT_ID))
            .containsExactly(LocalBloomSignal(BloomCategory.CHERRY, BloomStatus.STARTED))
    }

    private fun resolverOn(today: LocalDate) = LocalSpotBloomResolver(
        spotRecordPlantRepository,
        plantRepository,
        UserRecordEstimatorProperties(),
        Clock.fixed(today.atStartOfDay(KST).toInstant(), KST),
    )

    private fun stubCherry(vararg records: SpotRecord) {
        `when`(spotRecordPlantRepository.findByIdSpotRecordIdIn(records.map { requireNotNull(it.id) }))
            .thenReturn(records.map { SpotRecordPlant(SpotRecordPlantId(requireNotNull(it.id), PLANT_ID)) })
        `when`(plantRepository.findAllById(setOf(PLANT_ID)))
            .thenReturn(listOf(plant(PLANT_ID, BloomCategory.CHERRY)))
    }

    private fun record(
        id: Long,
        visitedDate: LocalDate,
        stage: BloomStage,
        createdAt: Instant = Instant.parse("2026-04-01T00:00:00Z"),
    ): SpotRecord {
        val record = SpotRecord(
            spotId = SPOT_ID,
            userId = 7L,
            visitedDate = visitedDate,
            bloomStage = stage,
            status = SpotRecordStatus.PUBLISHED,
        )
        ReflectionTestUtils.setField(record, "id", id)
        ReflectionTestUtils.setField(record, "createdAt", createdAt)
        return record
    }

    private fun plant(id: Long, category: BloomCategory?): Plant {
        val plant = Plant(name = "p-$id", status = PlantStatus.ACTIVE, bloomCategory = category)
        ReflectionTestUtils.setField(plant, "id", id)
        return plant
    }

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
        private val TODAY: LocalDate = LocalDate.of(2026, 4, 10)
        private const val SPOT_ID = 100L
        private const val PLANT_ID = 10L
    }
}
