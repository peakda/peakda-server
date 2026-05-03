package com.peakda.server.infrastructure.external.common

import com.fasterxml.jackson.dataformat.xml.XmlMapper

class DataGoKrErrorDecoder(
    private val xmlMapper: XmlMapper = XmlMapper(),
) {
    fun <T> decode(envelope: DataGoKrEnvelope<T>): DataGoKrBody<T> {
        val header = envelope.response.header
        return when (header.resultCode) {
            "0000", "00" -> envelope.response.body
            "03" -> DataGoKrBody.empty()
            else -> throw exceptionFor(header.resultCode, header.resultMsg)
        }
    }

    fun throwIfXmlError(rawBody: String) {
        if (!rawBody.trimStart().startsWith("<")) {
            return
        }

        val envelope = runCatching {
            xmlMapper.readValue(rawBody, DataGoKrErrorEnvelope::class.java)
        }.getOrElse {
            throw ExternalApiException(
                ExternalApiErrorCode.EXTERNAL_API_INVALID_RESPONSE,
                "외부 API XML 에러 응답을 파싱할 수 없습니다.",
                it,
            )
        }

        val header = envelope.cmmMsgHeader
        throw exceptionFor(header.returnReasonCode, header.returnAuthMsg.ifBlank { header.errMsg })
    }

    fun exceptionFor(resultCode: String, resultMsg: String): ExternalApiException {
        val externalApiErrorCode = when (resultCode) {
            "10", "11", "12" -> ExternalApiErrorCode.EXTERNAL_API_BAD_REQUEST
            "20", "30", "31", "32" -> ExternalApiErrorCode.EXTERNAL_API_AUTH_FAILED
            "22" -> ExternalApiErrorCode.EXTERNAL_API_QUOTA_EXCEEDED
            "04" -> ExternalApiErrorCode.EXTERNAL_API_TIMEOUT
            "01", "99" -> ExternalApiErrorCode.EXTERNAL_API_UNAVAILABLE
            else -> ExternalApiErrorCode.EXTERNAL_API_INVALID_RESPONSE
        }

        val detail = resultMsg.ifBlank { "resultCode=$resultCode" }
        return ExternalApiException(externalApiErrorCode, "외부 API 오류: $detail")
    }
}
