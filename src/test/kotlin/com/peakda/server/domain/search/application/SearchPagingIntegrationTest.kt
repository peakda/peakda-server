package com.peakda.server.domain.search.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.test.IntegrationTestSupport
import com.peakda.server.domain.search.presentation.SearchController
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Estimator
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import com.peakda.server.domain.spot.entity.BloomStage
import com.peakda.server.domain.spot.entity.Plant
import com.peakda.server.domain.spot.entity.PlantStatus
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordPlant
import com.peakda.server.domain.spot.entity.SpotRecordPlantId
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.repository.PlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordPlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

@Transactional
class SearchPagingIntegrationTest : IntegrationTestSupport() {
    @Autowired lateinit var service: SearchService
    @Autowired lateinit var controller: SearchController
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var spots: SpotRepository
    @Autowired lateinit var estimates: SeasonalBloomEstimateRepository
    @Autowired lateinit var records: SpotRecordRepository
    @Autowired lateinit var plants: PlantRepository
    @Autowired lateinit var tags: SpotRecordPlantRepository

    private val today get() = LocalDate.now(ZoneId.of("Asia/Seoul"))

    @Test
    fun `첫 원본 페이지가 불일치해도 같은 이름의 배치 경계를 지나 모든 일치를 센다`() {
        val candidates = spots.saveAllAndFlush((0 until 140).map { index ->
            val nameOrder = if (index in 126..130) 126 else index
            Spot(
                type = SpotType.ATTRACTION,
                attractionId = 900000L + index,
                name = "B4-regression-%03d".format(nameOrder),
                latitude = 37.0,
                longitude = 127.0,
            )
        })
        val matches = listOf(125, 127, 128, 130, 139).map(candidates::get)
        val hidden = spots.saveAndFlush(
            Spot(SpotType.ATTRACTION, attractionId = 900200L, name = "B4-regression-hidden",
                latitude = 37.0, longitude = 127.0, visible = false),
        )
        val baseDate = today
        estimates.saveAllAndFlush((matches + hidden).map { spot ->
            SeasonalBloomEstimate(
                requireNotNull(spot.attractionId), BloomCategory.CHERRY, baseDate,
                BloomStatus.PEAK, 0.8, Estimator.CALENDAR,
            )
        })
        estimates.saveAndFlush(
            SeasonalBloomEstimate(900001L, BloomCategory.CHERRY, baseDate.minusDays(1), BloomStatus.PEAK, 0.8, Estimator.CALENDAR),
        )

        val first = service.searchSpots(null, " b4-REGRESSION ", PageRequest(0, 3), BloomCategory.CHERRY)
        val last = service.searchSpots(null, "B4-regression", PageRequest(1, 3), BloomCategory.CHERRY)
        val empty = service.searchSpots(null, "B4-regression", PageRequest(2, 3), BloomCategory.CHERRY)
        val unfiltered = service.searchSpots(null, "B4-regression", PageRequest(0, 3))

        assertThat(first.content.map { it.spotId }).containsExactlyElementsOf(matches.take(3).map { it.id })
        assertThat(first.totalElements).isEqualTo(5)
        assertThat(first.totalPages).isEqualTo(2)
        assertThat(first.hasNext).isTrue()
        assertThat(last.content.map { it.spotId }).containsExactlyElementsOf(matches.drop(3).map { it.id })
        assertThat(last.totalElements).isEqualTo(5)
        assertThat(last.totalPages).isEqualTo(2)
        assertThat(last.hasNext).isFalse()
        assertThat(empty.content).isEmpty()
        assertThat(empty.totalElements).isEqualTo(5)
        assertThat(empty.totalPages).isEqualTo(2)
        assertThat(empty.hasNext).isFalse()
        assertThat(unfiltered.content.map { it.spotId }).containsExactlyElementsOf(candidates.take(3).map { it.id })
        assertThat(unfiltered.totalElements).isEqualTo(140)
        assertThat(unfiltered.totalPages).isEqualTo(47)
        assertThat(unfiltered.hasNext).isTrue()
    }

    @Test
    fun `동네형은 유효한 게시 기록만 사용하며 검색 JSON 계약과 리터럴 부분일치를 유지한다`() {
        val query = "B4-local%_\\"
        val local = saveLocal("${query}A")
        val stale = saveLocal("${query}B")
        val hidden = saveLocal("${query}C")
        val untagged = saveLocal("${query}D")
        val wildcardOnly = saveLocal("B4-localXXA")
        val cherry = requireNotNull(plants.saveAndFlush(Plant("B4-cherry", status = PlantStatus.ACTIVE, bloomCategory = BloomCategory.CHERRY)).id)
        val otherCherry = requireNotNull(plants.saveAndFlush(Plant("B4-cherry-2", status = PlantStatus.ACTIVE, bloomCategory = BloomCategory.CHERRY)).id)
        val plum = requireNotNull(plants.saveAndFlush(Plant("B4-plum", status = PlantStatus.ACTIVE, bloomCategory = BloomCategory.PLUM)).id)
        val date = today

        addRecord(local, date.minusDays(30), BloomStage.PEAK, cherry)
        val latest = addRecord(local, date.minusDays(30), BloomStage.LATE, cherry)
        tags.saveAndFlush(SpotRecordPlant(SpotRecordPlantId(latest, otherCherry)))
        addRecord(local, date, BloomStage.EARLY, plum)
        addRecord(stale, date.minusDays(31), BloomStage.PEAK, cherry)
        addRecord(hidden, date, BloomStage.PEAK, cherry, SpotRecordStatus.HIDDEN)
        addRecord(untagged, date, null, cherry)
        addRecord(wildcardOnly, date, BloomStage.PEAK, cherry)

        val result = controller.searchSpots(null, query, BloomCategory.CHERRY, PageRequest())
        val json = objectMapper.valueToTree<JsonNode>(result.body)
        val item = json.at("/data/content/0")

        assertThat(result.statusCode.value()).isEqualTo(200)
        assertThat(json["code"].asText()).isEqualTo("SUCCESS")
        assertThat(json.at("/data/totalElements").asLong()).isEqualTo(1)
        assertThat(json.at("/data/totalPages").asInt()).isEqualTo(1)
        assertThat(json.at("/data/hasNext").asBoolean()).isFalse()
        assertThat(item["spotId"].asLong()).isEqualTo(local.id)
        assertThat(item["type"].asText()).isEqualTo("LOCAL")
        assertThat(item["name"].asText()).isEqualTo(local.name)
        assertThat(item["favorited"].asBoolean()).isFalse()
        assertThat(item["notifyEnabled"].asBoolean()).isFalse()
        assertThat(item.at("/bloom/category").asText()).isEqualTo("CHERRY")
        assertThat(item.at("/bloom/status").asText()).isEqualTo("ENDED")

        val unfiltered = service.searchSpots(null, query, PageRequest())
        assertThat(unfiltered.totalElements).isEqualTo(4)
        assertThat(unfiltered.content.first().bloom?.category).isEqualTo(BloomCategory.PLUM)
    }

    private fun saveLocal(name: String): Spot = spots.saveAndFlush(
        Spot(SpotType.LOCAL, name = name, latitude = 37.0, longitude = 127.0),
    )

    private fun addRecord(
        spot: Spot,
        date: LocalDate,
        stage: BloomStage?,
        plantId: Long,
        status: SpotRecordStatus = SpotRecordStatus.PUBLISHED,
    ): Long {
        val record = records.saveAndFlush(
            SpotRecord(spotId = requireNotNull(spot.id), userId = 900000L, visitedDate = date, bloomStage = stage, status = status),
        )
        val recordId = requireNotNull(record.id)
        tags.saveAndFlush(SpotRecordPlant(SpotRecordPlantId(recordId, plantId)))
        return recordId
    }
}
