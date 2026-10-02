package com.peakda.server.domain.attraction.repository

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.auth.application.RefreshTokenService
import org.assertj.core.api.Assertions.assertThat
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
@Transactional
class AttractionOperatingInfoRepositoryTest {

    @MockitoBean
    lateinit var refreshTokenService: RefreshTokenService

    @MockitoBean
    lateinit var redissonClient: RedissonClient

    @Autowired
    lateinit var repository: AttractionOperatingInfoRepository

    @Autowired
    lateinit var attractionRepository: AttractionRepository

    @Test
    fun `같은 명소를 다시 받으면 비워진 항목까지 새 값으로 덮어쓴다`() {
        val attractionId = attraction("c-1").id!!
        repository.upsert(command(attractionId, operatingHours = "09:00~18:00", parking = "가능", modifiedAt = "1"))
        repository.upsert(command(attractionId, operatingHours = "10:00~17:00", parking = null, modifiedAt = "2"))

        val saved = repository.findByAttractionId(attractionId)!!

        assertThat(repository.count()).isEqualTo(1)
        assertThat(saved.operatingHours).isEqualTo("10:00~17:00")
        assertThat(saved.parking).isNull()
        assertThat(saved.sourceModifiedAt).isEqualTo("2")
    }

    @Test
    fun `운영 정보가 없는 공개 관광지만 id 순으로 고른다`() {
        val first = attraction("c-1")
        val second = attraction("c-2")
        val fetched = attraction("c-3")
        attraction("c-4", visible = false)
        attraction("c-5", contentTypeCode = "39")
        repository.upsert(command(fetched.id!!, modifiedAt = "20260901"))

        val targets = repository.findTargetsWithoutOperatingInfo(setOf("12"), PageRequest.of(0, 10))

        assertThat(targets.map { it.attractionId }).containsExactly(first.id, second.id)
        assertThat(targets.first()).isEqualTo(AttractionOperatingInfoTarget(first.id!!, "c-1", "12", "20260901"))
        assertThat(repository.findTargetsWithoutOperatingInfo(setOf("12"), PageRequest.of(0, 1))).hasSize(1)
    }

    @Test
    fun `받은 뒤 관광공사 수정 시각이 바뀐 명소만 다시 고른다`() {
        val unchanged = attraction("c-1", modifiedAt = "20260901")
        val modified = attraction("c-2", modifiedAt = "20260915")
        val noModifiedAt = attraction("c-3", modifiedAt = null)
        repository.upsert(command(unchanged.id!!, modifiedAt = "20260901"))
        repository.upsert(command(modified.id!!, modifiedAt = "20260901"))
        repository.upsert(command(noModifiedAt.id!!, modifiedAt = null))

        val targets = repository.findTargetsWithOutdatedOperatingInfo(setOf("12"), PageRequest.of(0, 10))

        assertThat(targets.map { it.attractionId }).containsExactly(modified.id)
        assertThat(targets.single().externalModifiedAt).isEqualTo("20260915")
    }

    private fun attraction(
        contentId: String,
        contentTypeCode: String = "12",
        visible: Boolean = true,
        modifiedAt: String? = "20260901",
    ): Attraction = attractionRepository.save(
        Attraction(
            tourApiContentId = contentId,
            contentTypeCode = contentTypeCode,
            title = "명소 $contentId",
            externalModifiedAt = modifiedAt,
            visible = visible,
        ),
    )

    private fun command(
        attractionId: Long,
        operatingHours: String? = null,
        parking: String? = null,
        modifiedAt: String?,
    ) = AttractionOperatingInfoUpsertCommand(
        attractionId = attractionId,
        operatingHours = operatingHours,
        closedDays = null,
        admissionFee = null,
        parking = parking,
        sourceModifiedAt = modifiedAt,
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
