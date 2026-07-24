package com.peakda.server.domain.spot.repository

import com.peakda.server.domain.auth.application.RefreshTokenService
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotFavorite
import com.peakda.server.domain.spot.entity.SpotType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class SpotFavoriteRepositoryTest {

    @MockitoBean
    lateinit var refreshTokenService: RefreshTokenService

    @MockitoBean
    lateinit var redissonClient: RedissonClient

    @Autowired
    lateinit var spotRepository: SpotRepository

    @Autowired
    lateinit var spotFavoriteRepository: SpotFavoriteRepository

    @BeforeEach
    fun cleanUp() {
        spotFavoriteRepository.deleteAll()
        spotRepository.deleteAll()
    }

    @Test
    @Transactional
    fun `알림이 켜진 노출 명소형 찜을 프로젝션으로 조회한다`() {
        val spot = saveSpot(attractionId = 100L, name = "서울숲")
        val spotId = requireNotNull(spot.id)
        spotFavoriteRepository.saveAndFlush(
            SpotFavorite(userId = 1L, spotId = spotId, notifyEnabled = true),
        )

        val result = spotFavoriteRepository.findAlertTargets(
            attractionType = SpotType.ATTRACTION,
            pageable = PageRequest.of(0, 20),
        )

        assertThat(result.content).hasSize(1)
        val row = result.content.single()
        assertThat(row.userId).isEqualTo(1L)
        assertThat(row.spotId).isEqualTo(spotId)
        assertThat(row.spotName).isEqualTo("서울숲")
        assertThat(row.attractionId).isEqualTo(100L)
    }

    @Test
    @Transactional
    fun `알림 OFF 비노출 로컬 스팟의 찜은 알림 대상에서 제외한다`() {
        val eligible = saveSpot(attractionId = 100L, name = "알림대상")
        val notifyOff = saveSpot(attractionId = 200L, name = "알림꺼짐")
        val hidden = saveSpot(attractionId = 300L, name = "비노출", visible = false)
        val local = saveSpot(attractionId = null, name = "동네스팟", type = SpotType.LOCAL)
        spotFavoriteRepository.saveAllAndFlush(
            listOf(
                favorite(userId = 1L, spot = eligible),
                favorite(userId = 2L, spot = notifyOff, notifyEnabled = false),
                favorite(userId = 3L, spot = hidden),
                favorite(userId = 4L, spot = local),
            ),
        )

        val result = spotFavoriteRepository.findAlertTargets(
            attractionType = SpotType.ATTRACTION,
            pageable = PageRequest.of(0, 20),
        )

        assertThat(result.content).hasSize(1)
        assertThat(result.content.single().spotId).isEqualTo(requireNotNull(eligible.id))
    }

    private fun saveSpot(
        attractionId: Long?,
        name: String,
        type: SpotType = SpotType.ATTRACTION,
        visible: Boolean = true,
    ): Spot = spotRepository.saveAndFlush(
        Spot(
            type = type,
            attractionId = attractionId,
            name = name,
            latitude = 37.5,
            longitude = 127.0,
            visible = visible,
        ),
    )

    private fun favorite(
        userId: Long,
        spot: Spot,
        notifyEnabled: Boolean = true,
    ) = SpotFavorite(
        userId = userId,
        spotId = requireNotNull(spot.id),
        notifyEnabled = notifyEnabled,
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
