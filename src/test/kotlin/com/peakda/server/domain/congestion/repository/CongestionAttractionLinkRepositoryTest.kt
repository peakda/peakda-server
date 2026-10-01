package com.peakda.server.domain.congestion.repository

import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.attraction.repository.AttractionUpsertCommand
import com.peakda.server.domain.auth.application.RefreshTokenService
import com.peakda.server.domain.congestion.entity.CongestionAttractionLink
import com.peakda.server.domain.congestion.entity.CongestionLinkStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CongestionAttractionLinkRepositoryTest {

    @MockitoBean
    lateinit var refreshTokenService: RefreshTokenService

    @MockitoBean
    lateinit var redissonClient: RedissonClient

    @Autowired
    lateinit var congestionRepository: CongestionRepository

    @Autowired
    lateinit var linkRepository: CongestionAttractionLinkRepository

    @Autowired
    lateinit var attractionRepository: AttractionRepository

    @Test
    fun `기준일 이후 예측이 있는 관광지 자연키를 중복 없이 읽고, 한 관광지의 기간 예측을 날짜순으로 읽는다`() {
        upsertCongestion("20260929", "석촌호수", "40")
        upsertCongestion("20261001", "석촌호수", "55")
        upsertCongestion("20261002", "석촌호수", "70")
        upsertCongestion("20261001", "롯데월드", "80")

        val keys = congestionRepository.findAttractionKeysFrom("20260930")
        val forecasts = congestionRepository
            .findByAreaCodeAndSigunguCodeAndTouristAttractionNameAndBaseDateBetweenOrderByBaseDateAsc(
                "11", "11710", "석촌호수", "20260930", "20261006",
            )

        assertThat(keys).containsExactlyInAnyOrder(
            CongestionAttractionKey("11", "11710", "석촌호수"),
            CongestionAttractionKey("11", "11710", "롯데월드"),
        )
        assertThat(forecasts).extracting<String> { it.baseDate }.containsExactly("20261001", "20261002")
    }

    @Test
    fun `이름 후보는 공개된 서비스 대상 명소만 법정동 시군구·시도 코드로 읽는다`() {
        upsertAttraction("1", "석촌호수", "11710", contentType = "12", visible = true)
        upsertAttraction("2", "잠실 문화센터", "11710", contentType = "14", visible = true)
        upsertAttraction("3", "숨김 명소", "11710", contentType = "12", visible = false)
        upsertAttraction("4", "양림동", "12210", contentType = "12", visible = true)

        val bySigungu = attractionRepository.findNameCandidatesBySigungu("11710", setOf("12"))
        val byArea = attractionRepository.findNameCandidatesByAreas(setOf("12"), setOf("12"))

        assertThat(bySigungu).extracting<String> { it.title }.containsExactly("석촌호수")
        assertThat(byArea).extracting<String> { it.title }.containsExactly("양림동")
    }

    @Test
    fun `명소의 확정 연결만 골라 읽는다`() {
        linkRepository.save(link("석촌호수", attractionId = 7, CongestionLinkStatus.CONFIRMED))
        linkRepository.save(link("석촌호수 둘레길", attractionId = 7, CongestionLinkStatus.PENDING_REVIEW))

        val confirmed = linkRepository.findByAttractionIdAndStatusOrderByIdAsc(7, CongestionLinkStatus.CONFIRMED)

        assertThat(confirmed).extracting<String> { it.touristAttractionName }.containsExactly("석촌호수")
        assertThat(linkRepository.countByStatus(CongestionLinkStatus.PENDING_REVIEW)).isEqualTo(1)
    }

    private fun upsertCongestion(baseDate: String, name: String, rate: String) {
        congestionRepository.upsert(CongestionUpsertCommand(baseDate, "11", "11710", name, rate))
    }

    private fun upsertAttraction(contentId: String, title: String, sigungu: String, contentType: String, visible: Boolean) {
        attractionRepository.upsert(
            AttractionUpsertCommand(
                tourApiContentId = contentId,
                contentTypeCode = contentType,
                title = title,
                addressMain = null,
                addressDetail = null,
                areaCode = null,
                sigunguCode = null,
                legalDongAreaCode = sigungu.take(2),
                legalDongSigunguCode = sigungu,
                longitude = null,
                latitude = null,
                primaryImageUrl = null,
                thumbnailImageUrl = null,
                categoryMajor = null,
                categoryMedium = null,
                categoryMinor = null,
                externalCreatedAt = null,
                externalModifiedAt = null,
                visible = visible,
            ),
        )
    }

    private fun link(name: String, attractionId: Long, status: CongestionLinkStatus) = CongestionAttractionLink(
        areaCode = "11",
        sigunguCode = "11710",
        touristAttractionName = name,
        attractionId = attractionId,
        status = status,
    )

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
            .withDatabaseName("peakda")
            .withUsername("peakda")
            .withPassword("peakda")
    }
}
