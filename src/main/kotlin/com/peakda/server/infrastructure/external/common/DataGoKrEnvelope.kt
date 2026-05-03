package com.peakda.server.infrastructure.external.common

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

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
    val items: DataGoKrItems<T> = DataGoKrItems(),
    val numOfRows: Int = 0,
    val pageNo: Int = 0,
    val totalCount: Int = 0,
) {
    val item: List<T>
        get() = items.item

    companion object {
        fun <T> empty(): DataGoKrBody<T> = DataGoKrBody()
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class DataGoKrItems<T>(
    val item: List<T> = emptyList(),
)
