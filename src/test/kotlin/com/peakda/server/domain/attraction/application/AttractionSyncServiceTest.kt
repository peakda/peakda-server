package com.peakda.server.domain.attraction.application

import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.auth.application.RefreshTokenService
import com.peakda.server.domain.congestion.application.CongestionSyncService
import com.peakda.server.domain.festival.application.FestivalSyncService
import com.peakda.server.domain.gallery.application.GalleryPhotoSyncService
import com.peakda.server.domain.trail.application.WalkingCourseSyncService
import com.peakda.server.domain.trail.application.WalkingRouteSyncService
import com.peakda.server.domain.visitor.application.RegionVisitorSyncService
import com.peakda.server.domain.weather.application.WeatherMidForecastSyncService
import com.peakda.server.domain.weather.application.WeatherShortForecastSyncService
import com.peakda.server.infrastructure.external.kma.midfcst.response.MidLandFcstItem
import com.peakda.server.infrastructure.external.kma.midfcst.response.MidTaItem
import com.peakda.server.infrastructure.external.kma.vilagefcst.response.VilageFcstItem
import com.peakda.server.infrastructure.external.kto.datalab.response.MetcoVisitrItem
import com.peakda.server.infrastructure.external.kto.durunubi.response.CourseItem
import com.peakda.server.infrastructure.external.kto.durunubi.response.RouteItem
import com.peakda.server.infrastructure.external.kto.korservice.response.AreaBasedSyncListItem
import com.peakda.server.infrastructure.external.kto.photo.response.GalleryListItem
import com.peakda.server.infrastructure.external.kto.tatscnctr.response.CnctrRateItem
import com.peakda.server.infrastructure.external.pubdata.festival.response.FestivalItem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class AttractionSyncServiceTest {

    @MockitoBean
    lateinit var refreshTokenService: RefreshTokenService

    @MockitoBean
    lateinit var redissonClient: RedissonClient

    @Autowired
    lateinit var syncService: AttractionSyncService

    @Autowired
    lateinit var repository: AttractionRepository

    @Autowired
    lateinit var festivalSyncService: FestivalSyncService

    @Autowired
    lateinit var weatherMidForecastSyncService: WeatherMidForecastSyncService

    @Autowired
    lateinit var weatherShortForecastSyncService: WeatherShortForecastSyncService

    @Autowired
    lateinit var walkingRouteSyncService: WalkingRouteSyncService

    @Autowired
    lateinit var walkingCourseSyncService: WalkingCourseSyncService

    @Autowired
    lateinit var congestionSyncService: CongestionSyncService

    @Autowired
    lateinit var regionVisitorSyncService: RegionVisitorSyncService

    @Autowired
    lateinit var galleryPhotoSyncService: GalleryPhotoSyncService

    @Test
    fun `upsertPage inserts then updates the same attraction idempotently`() {
        val inserted = syncService.upsertPage(
            listOf(
                item(
                    title = "첫 제목",
                    addressMain = "서울시 중구",
                    longitude = "126.1",
                    latitude = "37.1",
                ),
            ),
        )
        val updated = syncService.upsertPage(
            listOf(
                item(
                    title = "수정 제목",
                    addressMain = "",
                    longitude = "",
                    latitude = "",
                ),
            ),
        )

        val saved = repository.findByTourApiContentId(CONTENT_ID)

        assertThat(inserted).isEqualTo(1)
        assertThat(updated).isEqualTo(1)
        assertThat(repository.count()).isEqualTo(1)
        assertThat(saved).isNotNull
        assertThat(saved!!.title).isEqualTo("수정 제목")
        assertThat(saved.addressMain).isEqualTo("서울시 중구")
        assertThat(saved.longitude).isEqualTo(126.1)
        assertThat(saved.latitude).isEqualTo(37.1)
    }

    @Test
    fun `native upsert queries execute for all external sync tables`() {
        assertThat(
            festivalSyncService.upsertPage(
                listOf(FestivalItem(fstvlNm = "축제", opar = "광장", fstvlStartDate = "2026-05-01")),
            ),
        ).isEqualTo(1)
        assertThat(
            weatherShortForecastSyncService.upsertPage(
                listOf(
                    VilageFcstItem(
                        baseDate = "20260514",
                        baseTime = "0500",
                        category = "TMP",
                        fcstDate = "20260514",
                        fcstTime = "0600",
                        fcstValue = "20",
                        nx = 60,
                        ny = 127,
                    ),
                ),
            ),
        ).isEqualTo(1)
        assertThat(
            weatherMidForecastSyncService.upsertLandForecast(
                regionCode = "SEOUL",
                sourceRegionCode = "11B00000",
                announceTime = "202605140600",
                item = MidLandFcstItem(wf3Am = "맑음"),
            ),
        ).isEqualTo(1)
        assertThat(
            weatherMidForecastSyncService.upsertTemperatureForecast(
                regionCode = "SEOUL",
                sourceRegionCode = "11B10101",
                announceTime = "202605140600",
                item = MidTaItem(taMin3 = 10, taMax3 = 20),
            ),
        ).isEqualTo(1)
        assertThat(walkingRouteSyncService.upsertPage(listOf(RouteItem(routeIdx = "route-1")))).isEqualTo(1)
        assertThat(walkingCourseSyncService.upsertPage(listOf(CourseItem(crsIdx = "course-1")))).isEqualTo(1)
        assertThat(
            congestionSyncService.upsertPage(listOf(CnctrRateItem(baseYmd = "20260514", tAtsCd = "spot-1"))),
        ).isEqualTo(1)
        assertThat(
            regionVisitorSyncService.upsertPage(
                listOf(MetcoVisitrItem(baseYmd = "20260514", areaCd = "11", touDivCd = "1")),
            ),
        ).isEqualTo(1)
        assertThat(galleryPhotoSyncService.upsertPage(listOf(GalleryListItem(galContentId = "photo-1")))).isEqualTo(1)
    }

    private fun item(
        title: String,
        addressMain: String,
        longitude: String,
        latitude: String,
    ): AreaBasedSyncListItem = AreaBasedSyncListItem(
        contentid = CONTENT_ID,
        contenttypeid = "12",
        title = title,
        addr1 = addressMain,
        mapx = longitude,
        mapy = latitude,
        showflag = "1",
    )

    companion object {
        private const val CONTENT_ID = "test-content-1"

        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
            .withDatabaseName("peakda")
            .withUsername("peakda")
            .withPassword("peakda")
    }
}
