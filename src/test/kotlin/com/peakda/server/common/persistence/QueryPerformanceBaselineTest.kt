package com.peakda.server.common.persistence

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.test.IntegrationTestSupport
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.feed.application.FeedService
import com.peakda.server.domain.feed.entity.FeedFilter
import com.peakda.server.domain.seasonal.application.SpotBloomMapService
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.spot.entity.Plant
import com.peakda.server.domain.spot.entity.PlantStatus
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.repository.PlantRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

@Transactional
class QueryPerformanceBaselineTest : IntegrationTestSupport() {
    @Autowired lateinit var entityManager: EntityManager
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var feed: FeedService
    @Autowired lateinit var bloomMap: SpotBloomMapService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var spots: SpotRepository
    @Autowired lateinit var plants: PlantRepository

    @ParameterizedTest(name = "{0} historical observations")
    @ValueSource(ints = [32, 256, 2048])
    fun `관심 피드와 동네 개화 지도는 이력 규모가 달라도 공개 결과를 유지한다`(history: Int) {
        val viewer = saveUser("PerfView")
        val author = saveUser("PerfAuthor")
        val blocked = saveUser("PerfBlock")
        val spotId = spots.saveAndFlush(Spot(SpotType.LOCAL, name = "Query baseline", latitude = 37.123, longitude = 127.123)).id!!
        val plantId = saveCherry("Query baseline cherry")
        val secondPlant = saveCherry("Query baseline second cherry")
        jdbc.update("insert into user_favorite_categories (user_id, category) values (?, 'CHERRY')", viewer)
        jdbc.update("""
            insert into user_blocks (blocker_id, blocked_id, created_at, updated_at)
            values (?, ?, now(), now())
        """.trimIndent(), viewer, blocked)

        val freshDate = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1)
        jdbc.update("""
            with inserted as (
                insert into spot_records (spot_id, user_id, visited_date, bloom_stage, memo, status,
                                          published_at, created_at, updated_at)
                select ?, case when n % 4 = 0 then ? else ? end, ?::date - 365,
                       'PEAK', 'history-' || n, 'PUBLISHED', stamp, stamp, stamp
                from generate_series(1, ?) as series(n)
                cross join lateral (select timestamptz '2025-01-01 00:00:00Z' + n * interval '1 second' as stamp) times
                returning id
            )
            insert into spot_record_plants (spot_record_id, plant_id) select id, ? from inserted
        """.trimIndent(), spotId, blocked, author, freshDate, history, plantId)
        val peak = saveFreshObservation(spotId, author, plantId, freshDate.minusDays(1), "PEAK")
        val late = saveFreshObservation(spotId, author, plantId, freshDate, "LATE")
        jdbc.update("insert into spot_record_plants (spot_record_id, plant_id) values (?, ?)", late, secondPlant)

        val interest = measure("interest-feed", history) {
            feed.list(viewer, FeedFilter.INTEREST, PageRequest(page = 0, size = 2))
        }
        assertThat(interest.content.map { it.id }).containsExactly(late, peak)
        assertThat(interest.totalElements).isEqualTo(history.toLong() - history / 4 + 2)
        assertThat(interest.hasNext).isTrue()

        val map = measure("local-bloom-map", history) {
            bloomMap.map(37.122, 37.124, 127.122, 127.124, BloomCategory.CHERRY, date = null)
        }
        assertThat(map.pins.map { it.spotId }).containsExactly(spotId)
        assertThat(map.pins.single().blooms.map { it.category to it.status })
            .containsExactly(BloomCategory.CHERRY to BloomStatus.ENDED)
    }

    private fun saveUser(nickname: String): Long =
        users.saveAndFlush(User(OAuth2LoginType.KAKAO, "baseline-$nickname", nickname)).id!!

    private fun saveCherry(name: String): Long =
        plants.saveAndFlush(Plant(name, status = PlantStatus.ACTIVE, bloomCategory = BloomCategory.CHERRY)).id!!

    private fun saveFreshObservation(spotId: Long, author: Long, plantId: Long, date: LocalDate, stage: String): Long {
        val id = jdbc.queryForObject("""
            insert into spot_records (spot_id, user_id, visited_date, bloom_stage, status,
                                      published_at, created_at, updated_at)
            values (?, ?, ?, ?, 'PUBLISHED', ?::date, ?::date, ?::date)
            returning id
        """.trimIndent(), Long::class.java, spotId, author, date, stage, date, date, date)!!
        jdbc.update("insert into spot_record_plants (spot_record_id, plant_id) values (?, ?)", id, plantId)
        return id
    }

    /** Report local service latency and Hibernate query counters without timing-based assertions. */
    private fun <T> measure(name: String, history: Int, request: () -> T): T {
        repeat(2) {
            entityManager.clear()
            request()
        }
        val durations = (1..7).map {
            entityManager.clear()
            val start = System.nanoTime()
            request()
            (System.nanoTime() - start) / 1_000
        }.sorted()

        entityManager.clear()
        val explain = System.getenv("PEAKDA_EXPLAIN_BASELINE") == "1"
        val logStart = if (explain) {
            // Capture plans from the actual prepared statements in the Testcontainers session.
            jdbc.execute("load 'auto_explain'")
            jdbc.execute("set local auto_explain.log_analyze = true")
            jdbc.execute("set local auto_explain.log_buffers = true")
            jdbc.execute("set local auto_explain.log_timing = false")
            jdbc.execute("set local auto_explain.log_parameter_max_length = 0")
            jdbc.execute("set local auto_explain.log_min_duration = 0")
            postgres.logs.length
        } else {
            0
        }
        val statistics = entityManager.entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        val enabled = statistics.isStatisticsEnabled
        return try {
            statistics.isStatisticsEnabled = true
            statistics.clear()
            val response = request()
            println("QUERY_BASELINE $name history=$history elapsedMicros=$durations " +
                "statements=${statistics.prepareStatementCount} entityLoads=${statistics.entityLoadCount}")
            statistics.queries.sorted().forEach { query ->
                val stat = statistics.getQueryStatistics(query)
                println("QUERY_BASELINE rows=${stat.executionRowCount} runs=${stat.executionCount} $query")
            }
            if (explain) {
                jdbc.execute("set local auto_explain.log_min_duration = -1")
                val plans = postgres.logs.drop(logStart)
                check("Query Text:" in plans) { "PostgreSQL did not emit a plan for $name" }
                val maxBindings = plans.lineSequence().filter { "Query Text:" in it }
                    .flatMap { Regex("""\$(\d+)""").findAll(it) }
                    .maxOfOrNull { it.groupValues[1].toInt() } ?: 0
                println("QUERY_BASELINE $name history=$history maxBindings=$maxBindings\n$plans")
            }
            response
        } finally {
            statistics.clear()
            statistics.isStatisticsEnabled = enabled
        }
    }
}
