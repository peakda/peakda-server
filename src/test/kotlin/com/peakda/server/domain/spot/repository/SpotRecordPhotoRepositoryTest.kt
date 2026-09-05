package com.peakda.server.domain.spot.repository

import com.peakda.server.domain.auth.application.RefreshTokenService
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordPhoto
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
import java.time.LocalDate

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class SpotRecordPhotoRepositoryTest {

    @MockitoBean
    lateinit var refreshTokenService: RefreshTokenService

    @MockitoBean
    lateinit var redissonClient: RedissonClient

    @Autowired
    lateinit var spotRecordRepository: SpotRecordRepository

    @Autowired
    lateinit var spotRecordPhotoRepository: SpotRecordPhotoRepository

    @BeforeEach
    fun cleanUp() {
        spotRecordPhotoRepository.deleteAll()
        spotRecordRepository.deleteAll()
    }

    @Test
    @Transactional
    fun `스팟별 최근 게시 기록 사진을 상한만큼 방문일 최신순으로 조회한다`() {
        val older = saveRecord(spotId = SPOT_ID, visitedDate = LocalDate.of(2026, 3, 1))
        val newer = saveRecord(spotId = SPOT_ID, visitedDate = LocalDate.of(2026, 3, 20))
        savePhotos(older, "old-0", "old-1")
        savePhotos(newer, "new-0", "new-1")

        val photos = spotRecordPhotoRepository.findRecentPhotosBySpotIds(
            spotIds = listOf(SPOT_ID),
            status = SpotRecordStatus.PUBLISHED.name,
            limit = 3,
        )

        // 최신 기록이 먼저, 같은 기록 안에서는 정렬 순서대로. 상한을 넘는 마지막 한 장은 잘린다.
        assertThat(photos.map { it.objectKey }).containsExactly("new-0", "new-1", "old-0")
        assertThat(photos.map { it.spotId }).containsOnly(SPOT_ID)
    }

    @Test
    @Transactional
    fun `게시되지 않은 기록의 사진과 다른 스팟의 사진은 조회되지 않는다`() {
        val published = saveRecord(spotId = SPOT_ID, visitedDate = LocalDate.of(2026, 3, 10))
        val draft = saveRecord(
            spotId = SPOT_ID,
            visitedDate = LocalDate.of(2026, 3, 21),
            status = SpotRecordStatus.DRAFT,
        )
        val otherSpot = saveRecord(spotId = OTHER_SPOT_ID, visitedDate = LocalDate.of(2026, 3, 22))
        savePhotos(published, "published-0")
        savePhotos(draft, "draft-0")
        savePhotos(otherSpot, "other-0")

        val photos = spotRecordPhotoRepository.findRecentPhotosBySpotIds(
            spotIds = listOf(SPOT_ID),
            status = SpotRecordStatus.PUBLISHED.name,
            limit = 4,
        )

        assertThat(photos.map { it.objectKey }).containsExactly("published-0")
    }

    @Test
    @Transactional
    fun `여러 스팟을 한 번에 조회하면 스팟마다 상한이 따로 적용된다`() {
        val first = saveRecord(spotId = SPOT_ID, visitedDate = LocalDate.of(2026, 3, 5))
        val second = saveRecord(spotId = OTHER_SPOT_ID, visitedDate = LocalDate.of(2026, 3, 6))
        savePhotos(first, "first-0", "first-1", "first-2")
        savePhotos(second, "second-0", "second-1", "second-2")

        val photosBySpot = spotRecordPhotoRepository.findRecentPhotosBySpotIds(
            spotIds = listOf(SPOT_ID, OTHER_SPOT_ID),
            status = SpotRecordStatus.PUBLISHED.name,
            limit = 2,
        ).groupBy({ it.spotId }, { it.objectKey })

        assertThat(photosBySpot[SPOT_ID]).containsExactly("first-0", "first-1")
        assertThat(photosBySpot[OTHER_SPOT_ID]).containsExactly("second-0", "second-1")
    }

    @Test
    @Transactional
    fun `방문일이 없는 기록은 작성일을 기준으로 정렬된다`() {
        val visited = saveRecord(spotId = SPOT_ID, visitedDate = LocalDate.of(2000, 1, 1))
        val notVisited = saveRecord(spotId = SPOT_ID, visitedDate = null)
        savePhotos(visited, "visited-0")
        savePhotos(notVisited, "not-visited-0")

        val photos = spotRecordPhotoRepository.findRecentPhotosBySpotIds(
            spotIds = listOf(SPOT_ID),
            status = SpotRecordStatus.PUBLISHED.name,
            limit = 4,
        )

        // 방문일이 없으면 오늘 작성된 것으로 보므로 과거 방문일 기록보다 앞선다.
        assertThat(photos.map { it.objectKey }).containsExactly("not-visited-0", "visited-0")
    }

    private fun saveRecord(
        spotId: Long,
        visitedDate: LocalDate?,
        status: SpotRecordStatus = SpotRecordStatus.PUBLISHED,
    ): Long = requireNotNull(
        spotRecordRepository.saveAndFlush(
            SpotRecord(
                spotId = spotId,
                userId = USER_ID,
                visitedDate = visitedDate,
                status = status,
            ),
        ).id,
    )

    /** sort_order 는 1..5 범위 제약이 있어 1부터 매긴다. */
    private fun savePhotos(recordId: Long, vararg objectKeys: String) {
        spotRecordPhotoRepository.saveAllAndFlush(
            objectKeys.mapIndexed { index, objectKey ->
                SpotRecordPhoto(spotRecordId = recordId, objectKey = objectKey, sortOrder = index + 1)
            },
        )
    }

    companion object {
        private const val USER_ID = 1L
        private const val SPOT_ID = 10L
        private const val OTHER_SPOT_ID = 20L

        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
            .withDatabaseName("peakda")
            .withUsername("peakda")
            .withPassword("peakda")
    }
}
