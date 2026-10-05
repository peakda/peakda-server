package com.peakda.server.domain.weather.repository

import com.peakda.server.common.test.IntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@Transactional
class AttractionForecastAreaRepositoryTest : IntegrationTestSupport() {

    @Autowired
    lateinit var repository: AttractionForecastAreaRepository

    @Test
    fun `같은 명소를 다시 매핑하면 격자를 갱신하고, 격자는 명소가 많은 순으로 상한만큼 읽는다`() {
        repository.upsert(AttractionForecastAreaUpsertCommand(1, 60, 127, "SEOUL"))
        repository.upsert(AttractionForecastAreaUpsertCommand(2, 91, 106, "GYEONGNAM"))
        repository.upsert(AttractionForecastAreaUpsertCommand(3, 91, 106, "GYEONGNAM"))
        repository.upsert(AttractionForecastAreaUpsertCommand(4, 52, 38, "JEJU"))
        repository.upsert(AttractionForecastAreaUpsertCommand(1, 91, 106, "GYEONGNAM"))

        val grids = repository.findGridsByAttractionCount(PageRequest.of(0, 2))

        assertThat(grids).containsExactly(ForecastGrid(91, 106, 3), ForecastGrid(52, 38, 1))
        assertThat(repository.findByAttractionId(1)?.midRegionCode).isEqualTo("GYEONGNAM")
    }

    @Test
    fun `대상 명소 밖의 매핑만 지운다`() {
        repository.upsert(AttractionForecastAreaUpsertCommand(1, 60, 127, "SEOUL"))
        repository.upsert(AttractionForecastAreaUpsertCommand(2, 91, 106, "GYEONGNAM"))

        val removed = repository.deleteByAttractionIdNotIn(setOf(1L))

        assertThat(removed).isEqualTo(1)
        assertThat(repository.findAll()).extracting<Long> { it.attractionId }.containsExactly(1L)
    }
}
