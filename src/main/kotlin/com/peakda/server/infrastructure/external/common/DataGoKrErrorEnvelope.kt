package com.peakda.server.infrastructure.external.common

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "OpenAPI_ServiceResponse")
data class DataGoKrErrorEnvelope(
    @param:JacksonXmlProperty(localName = "cmmMsgHeader")
    val cmmMsgHeader: DataGoKrErrorHeader = DataGoKrErrorHeader(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DataGoKrErrorHeader(
    val errMsg: String = "",
    val returnAuthMsg: String = "",
    val returnReasonCode: String = "",
)
