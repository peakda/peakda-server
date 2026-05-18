package com.peakda.server.infrastructure.external.kto.photo

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.peakda.server.infrastructure.external.common.DataGoKrErrorDecoder
import com.peakda.server.infrastructure.external.common.ExternalApiLoggingInterceptor
import com.peakda.server.infrastructure.external.common.ExternalApiResilienceExecutor
import com.peakda.server.infrastructure.external.common.JsonOnlyInterceptor
import com.peakda.server.infrastructure.external.common.KtoCommonParamInterceptor
import com.peakda.server.infrastructure.external.common.ServiceKeyInterceptor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class PhotoGalleryClientTest {
    private val fixture = clientFixture()
    private val client = fixture.client
    private val server = fixture.server

    @Test
    fun `galleryList1 success decodes items`() {
        server.expect(requestTo("https://example.test/photo/galleryList1?serviceKey=test-key&MobileOS=ETC&MobileApp=peakda-test&_type=json"))
            .andRespond(withSuccess(successJson(), MediaType.APPLICATION_JSON))

        val body = client.galleryList(emptyMap())

        assertThat(body.item).hasSize(1)
        assertThat(body.item[0].galContentId).isEqualTo("G001")
    }

    @Test
    fun `gallerySearchList1 NODATA returns empty body`() {
        server.expect(requestTo("https://example.test/photo/gallerySearchList1?serviceKey=test-key&MobileOS=ETC&MobileApp=peakda-test&_type=json"))
            .andRespond(withSuccess(nodataJson(), MediaType.APPLICATION_JSON))

        val body = client.gallerySearchList(emptyMap())

        assertThat(body.item).isEmpty()
    }

    private fun clientFixture(): ClientFixture {
        val builder = RestClient.builder()
            .baseUrl("https://example.test/photo")
            .requestInterceptors {
                it.add(ServiceKeyInterceptor("test-key"))
                it.add(KtoCommonParamInterceptor("peakda-test"))
                it.add(JsonOnlyInterceptor())
                it.add(ExternalApiLoggingInterceptor("KTO", "PhotoGalleryService1"))
            }
        val server = MockRestServiceServer.bindTo(builder).build()
        return ClientFixture(
            PhotoGalleryClient(builder.build(), jacksonObjectMapper(), DataGoKrErrorDecoder(), ExternalApiResilienceExecutor.noop()),
            server,
        )
    }

    private fun successJson(): String =
        """{ "response": { "header": { "resultCode": "0000", "resultMsg": "OK" }, "body": { "items": { "item": [ { "galContentId": "G001", "galTitle": "사진" } ] }, "totalCount": 1 } } }"""

    private fun nodataJson(): String =
        """{ "response": { "header": { "resultCode": "03", "resultMsg": "NODATA_ERROR" }, "body": { "items": { "item": [] } } } }"""

    private data class ClientFixture(
        val client: PhotoGalleryClient,
        val server: MockRestServiceServer,
    )
}
