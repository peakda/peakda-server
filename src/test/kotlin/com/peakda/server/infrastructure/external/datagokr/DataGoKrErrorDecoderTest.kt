package com.peakda.server.infrastructure.external.datagokr

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.infrastructure.external.common.ExternalApiException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class DataGoKrErrorDecoderTest {
    private val decoder = DataGoKrErrorDecoder()

    @Test
    fun `0000 resultCode is successful`() {
        val body = DataGoKrBody(
            items = listOf(TestItem("1")),
            numOfRows = 10,
            pageNo = 1,
            totalCount = 1,
        )

        val decoded = decoder.decode(envelope("0000", body = body))

        assertThat(decoded.item).containsExactly(TestItem("1"))
        assertThat(decoded.totalCount).isEqualTo(1)
    }

    @Test
    fun `00 resultCode is successful`() {
        val body = DataGoKrBody(items = listOf(TestItem("1")))

        val decoded = decoder.decode(envelope("00", body = body))

        assertThat(decoded.item).containsExactly(TestItem("1"))
    }

    @Test
    fun `03 resultCode returns empty body`() {
        val decoded = decoder.decode(envelope<TestItem>("03", resultMsg = "NODATA_ERROR"))

        assertThat(decoded.item).isEmpty()
        assertThat(decoded.totalCount).isZero()
    }

    @Test
    fun `10 resultCode maps to bad request`() {
        assertExternalApiError("10", ErrorCode.EXTERNAL_API_BAD_REQUEST)
    }

    @Test
    fun `11 resultCode maps to bad request`() {
        assertExternalApiError("11", ErrorCode.EXTERNAL_API_BAD_REQUEST)
    }

    @Test
    fun `22 resultCode maps to quota exceeded`() {
        assertExternalApiError("22", ErrorCode.EXTERNAL_API_QUOTA_EXCEEDED)
    }

    @Test
    fun `99 resultCode maps to unavailable`() {
        assertExternalApiError("99", ErrorCode.EXTERNAL_API_UNAVAILABLE)
    }

    @Test
    fun `XML error envelope maps by returnReasonCode`() {
        val xml = """
            <OpenAPI_ServiceResponse>
              <cmmMsgHeader>
                <errMsg>SERVICE ERROR</errMsg>
                <returnAuthMsg>LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS</returnAuthMsg>
                <returnReasonCode>22</returnReasonCode>
              </cmmMsgHeader>
            </OpenAPI_ServiceResponse>
        """.trimIndent()

        assertThatThrownBy { decoder.throwIfXmlError(xml) }
            .isInstanceOf(ExternalApiException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.EXTERNAL_API_QUOTA_EXCEEDED)
    }

    @Test
    fun `non XML body is ignored by XML detector`() {
        decoder.throwIfXmlError("""{"response":{}}""")
    }

    private fun assertExternalApiError(resultCode: String, expected: ErrorCode) {
        assertThatThrownBy { decoder.decode(envelope<TestItem>(resultCode, resultMsg = "ERROR")) }
            .isInstanceOf(ExternalApiException::class.java)
            .extracting("errorCode")
            .isEqualTo(expected)
    }

    private fun <T> envelope(
        resultCode: String,
        resultMsg: String = "OK",
        body: DataGoKrBody<T> = DataGoKrBody.empty(),
    ): DataGoKrEnvelope<T> {
        return DataGoKrEnvelope(
            response = DataGoKrResponse(
                header = DataGoKrHeader(resultCode = resultCode, resultMsg = resultMsg),
                body = body,
            )
        )
    }

    data class TestItem(
        val id: String,
    )
}
