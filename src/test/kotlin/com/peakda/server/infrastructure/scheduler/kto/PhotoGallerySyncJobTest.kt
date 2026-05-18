package com.peakda.server.infrastructure.scheduler.kto

import com.peakda.server.domain.gallery.application.GalleryPhotoSyncService
import com.peakda.server.domain.gallery.repository.GalleryPhotoRepository
import com.peakda.server.infrastructure.external.kto.photo.PhotoGalleryClient
import com.peakda.server.infrastructure.external.kto.photo.response.GalleryListItem
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.ktoFixture
import com.peakda.server.infrastructure.scheduler.testErrorDecoder
import com.peakda.server.infrastructure.scheduler.testJobLogger
import com.peakda.server.infrastructure.scheduler.testObjectMapper
import com.peakda.server.infrastructure.scheduler.testResilience
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

class PhotoGallerySyncJobTest {
    private val fixture = ktoFixture("https://example.test/photo", "PhotoGalleryService1") {
        PhotoGalleryClient(it, testObjectMapper, testErrorDecoder, testResilience)
    }
    private val syncService = RecordingGallerySync()

    @Test
    fun `run 시 galleryList를 호출해 sync service에 페이지를 전달한다`() {
        fixture.server.expect(
            requestTo(startsWith("https://example.test/photo/galleryList1?numOfRows=100&pageNo=1")),
        ).andRespond(withSuccess(SUCCESS_JSON, MediaType.APPLICATION_JSON))

        val job = PhotoGallerySyncJob(fixture.client, syncService, enabled(true), testJobLogger())
        job.run()

        fixture.server.verify()
        assertThat(syncService.pages.flatten()).extracting<String> { it.galContentId }.containsExactly("G001")
    }

    @Test
    fun `enabled=false 이면 client와 sync service 모두 호출하지 않는다`() {
        val job = PhotoGallerySyncJob(fixture.client, syncService, enabled(false), testJobLogger())

        job.run()

        fixture.server.verify()
        assertThat(syncService.pages).isEmpty()
    }

    private fun enabled(jobEnabled: Boolean) = SchedulerProperties(
        enabled = true,
        kto = SchedulerProperties.KtoSchedulerProps(
            photo = SchedulerProperties.JobProps(cron = "* * * * * *", enabled = jobEnabled),
        ),
    )

    private class RecordingGallerySync :
        GalleryPhotoSyncService(Mockito.mock(GalleryPhotoRepository::class.java)) {
        val pages = mutableListOf<List<GalleryListItem>>()
        override fun upsertPage(items: List<GalleryListItem>): Int {
            pages += items.toList(); return items.size
        }
    }

    companion object {
        private val SUCCESS_JSON = """
            { "response": { "header": { "resultCode": "0000", "resultMsg": "OK" },
              "body": { "items": { "item": [ { "galContentId": "G001", "galTitle": "사진" } ] }, "totalCount": 1 } } }
        """.trimIndent()
    }
}
