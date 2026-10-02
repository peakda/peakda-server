package com.peakda.server.domain.gallery.repository

import com.peakda.server.domain.auth.application.RefreshTokenService
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
class GalleryPhotoRepositoryTest {

    @MockitoBean
    lateinit var refreshTokenService: RefreshTokenService

    @MockitoBean
    lateinit var redissonClient: RedissonClient

    @Autowired
    lateinit var repository: GalleryPhotoRepository

    @Test
    fun `제목이나 검색 키워드가 정규식에 맞는 사진만 읽는다`() {
        upsert("g1", title = "마곡사", keyword = "마곡사, 단풍, 가을")
        upsert("g2", title = "진해 벚꽃", keyword = null)
        upsert("g3", title = "해운대 야경", keyword = "해운대, 야경")

        val photos = repository.findByTextMatching("단풍|벚꽃")

        assertThat(photos.map { it.tourApiContentId }).containsExactlyInAnyOrder("g1", "g2")
    }

    private fun upsert(contentId: String, title: String?, keyword: String?) {
        repository.upsert(
            GalleryPhotoUpsertCommand(
                tourApiContentId = contentId,
                contentTypeCode = null,
                title = title,
                webImageUrl = null,
                externalCreatedAt = null,
                externalModifiedAt = null,
                photographyMonth = null,
                photographyLocation = null,
                photographer = null,
                searchKeyword = keyword,
            ),
        )
    }

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
