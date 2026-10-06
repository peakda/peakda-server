package com.peakda.server.domain.attraction.repository

import com.peakda.server.common.test.IntegrationTestSupport
import com.peakda.server.domain.attraction.entity.Attraction
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Transactional
class AttractionThumbnailCheckRepositoryTest : IntegrationTestSupport() {

    @Autowired
    lateinit var repository: AttractionRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    fun `현재 썸네일 URL 을 확인하지 않은 공개 관광지만 id 순으로 고른다`() {
        val neverChecked = attraction("c-1", thumbnail = url("c-1"))
        val urlChanged = attraction("c-2", thumbnail = url("c-2-new"), checkedUrl = url("c-2"), checkedAt = OLD)
        attraction("c-3", thumbnail = url("c-3"), checkedUrl = url("c-3"), checkedAt = OLD)
        attraction("c-4", thumbnail = null)
        attraction("c-5", thumbnail = url("c-5"), visible = false)
        attraction("c-6", thumbnail = url("c-6"), contentTypeCode = "39")

        val targets = repository.findThumbnailUncheckedTargets(setOf("12"), PageRequest.of(0, 10))

        assertThat(targets).containsExactly(
            AttractionThumbnailCheckTarget(neverChecked.id!!, url("c-1")),
            AttractionThumbnailCheckTarget(urlChanged.id!!, url("c-2-new")),
        )
        assertThat(repository.findThumbnailUncheckedTargets(setOf("12"), PageRequest.of(0, 1))).hasSize(1)
    }

    @Test
    fun `기준 시각 전에 확인한 썸네일만 오래된 순으로 고른다`() {
        val older = attraction("c-1", thumbnail = url("c-1"), checkedUrl = url("c-1"), checkedAt = Instant.parse("2026-08-01T00:00:00Z"))
        val old = attraction("c-2", thumbnail = url("c-2"), checkedUrl = url("c-2"), checkedAt = OLD, missing = true)
        attraction("c-3", thumbnail = url("c-3"), checkedUrl = url("c-3"), checkedAt = RECENT)
        attraction("c-4", thumbnail = url("c-4-new"), checkedUrl = url("c-4"), checkedAt = OLD)

        val targets = repository.findThumbnailCheckedBefore(setOf("12"), Instant.parse("2026-09-15T00:00:00Z"), PageRequest.of(0, 10))

        assertThat(targets.map { it.attractionId }).containsExactly(older.id, old.id)
    }

    @Test
    fun `확인 결과를 저장한다`() {
        val attraction = attraction("c-1", thumbnail = url("c-1"))

        repository.updateThumbnailCheck(attraction.id!!, url("c-1"), missing = true, checkedAt = RECENT)
        entityManager.clear()

        val saved = repository.findById(attraction.id!!).orElseThrow()
        assertThat(saved.thumbnailCheckedUrl).isEqualTo(url("c-1"))
        assertThat(saved.thumbnailMissing).isTrue()
        assertThat(saved.thumbnailCheckedAt).isEqualTo(RECENT)
        assertThat(saved.cardImageUrl()).isEqualTo(saved.primaryImageUrl)
    }

    private fun attraction(
        contentId: String,
        thumbnail: String?,
        contentTypeCode: String = "12",
        visible: Boolean = true,
        checkedUrl: String? = null,
        checkedAt: Instant? = null,
        missing: Boolean = false,
    ): Attraction = repository.save(
        Attraction(
            tourApiContentId = contentId,
            contentTypeCode = contentTypeCode,
            title = "명소 $contentId",
            primaryImageUrl = "http://tong.visitkorea.or.kr/cms/resource/30/${contentId}_image2_1.jpg",
            thumbnailImageUrl = thumbnail,
            visible = visible,
            thumbnailCheckedUrl = checkedUrl,
            thumbnailCheckedAt = checkedAt,
            thumbnailMissing = missing,
        ),
    ).also { entityManager.flush() }

    private fun url(name: String) = "http://tong.visitkorea.or.kr/cms/resource/30/${name}_image3_1.jpg"

    companion object {
        private val OLD = Instant.parse("2026-09-01T00:00:00Z")
        private val RECENT = Instant.parse("2026-10-01T00:00:00Z")
    }
}
