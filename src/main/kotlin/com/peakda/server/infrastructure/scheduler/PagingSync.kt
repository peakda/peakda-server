package com.peakda.server.infrastructure.scheduler

import com.peakda.server.infrastructure.external.common.DataGoKrBody

internal data class PagingResult(val processed: Int, val totalCount: Int)

/**
 * 공공데이터 portal 페이지 응답을 [pageSize] 단위로 반복 호출하면서 [upsert]에 위임한다.
 *
 * 첫 페이지의 totalCount 를 기준으로 stop 조건을 잡고, 빈 페이지가 오면 즉시 종료한다.
 * extras 는 numOfRows/pageNo 외에 매 호출에 함께 보낼 쿼리 파라미터.
 */
internal inline fun <T> runPaging(
    pageSize: Int = 100,
    maxPages: Int = 50,
    extras: Map<String, Any?> = emptyMap(),
    fetch: (Map<String, Any?>) -> DataGoKrBody<T>,
    upsert: (List<T>) -> Int,
): PagingResult {
    var page = 1
    var totalCount = 0
    var processed = 0
    while (page <= maxPages) {
        val params = mapOf("numOfRows" to pageSize, "pageNo" to page) + extras
        val body = fetch(params)
        if (page == 1) totalCount = body.totalCount
        if (body.item.isEmpty()) break
        processed += upsert(body.item)
        if (totalCount > 0 && processed >= totalCount) break
        page++
    }
    return PagingResult(processed, totalCount)
}
