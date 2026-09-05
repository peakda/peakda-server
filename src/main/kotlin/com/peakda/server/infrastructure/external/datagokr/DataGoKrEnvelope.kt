package com.peakda.server.infrastructure.external.datagokr

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

@JsonIgnoreProperties(ignoreUnknown = true)
data class DataGoKrEnvelope<T>(
    val response: DataGoKrResponse<T> = DataGoKrResponse(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DataGoKrResponse<T>(
    val header: DataGoKrHeader = DataGoKrHeader(),
    val body: DataGoKrBody<T> = DataGoKrBody(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DataGoKrHeader(
    val resultCode: String = "",
    val resultMsg: String = "",
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DataGoKrBody<T>(
    @param:JsonDeserialize(using = DataGoKrItemsDeserializer::class)
    val items: List<T> = emptyList(),
    val numOfRows: Int = 0,
    val pageNo: Int = 0,
    val totalCount: Int = 0,
) {
    @get:JsonIgnore
    val item: List<T>
        get() = items

    companion object {
        fun <T> empty(): DataGoKrBody<T> = DataGoKrBody()
    }
}
