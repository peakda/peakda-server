package com.peakda.server.infrastructure.scheduler

import com.peakda.server.infrastructure.external.datagokr.DataGoKrBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PagingSyncTest {
    @Test
    fun `서버가 요청보다 작은 상한으로 잘라 응답하면 이후 페이지를 그 상한 기준으로 요청해 누락 없이 모은다`() {
        val source = (1..250).toList()
        val serverCap = 100
        val requests = mutableListOf<Pair<Int, Int>>()
        val collected = mutableListOf<Int>()

        val result = runPaging(
            pageSize = 1000,
            fetch = { params ->
                val rows = params["numOfRows"] as Int
                val page = params["pageNo"] as Int
                requests += rows to page
                val size = minOf(rows, serverCap)
                val from = (page - 1) * size
                DataGoKrBody(items = source.drop(from).take(size), totalCount = source.size)
            },
            upsert = { collected += it; it.size },
        )

        assertThat(collected).containsExactlyElementsOf(source)
        assertThat(result.processed).isEqualTo(250)
        assertThat(requests).containsExactly(1000 to 1, 100 to 2, 100 to 3)
    }

    @Test
    fun `첫 페이지에 전부 담기면 추가 호출하지 않는다`() {
        var calls = 0
        val result = runPaging(
            pageSize = 1000,
            fetch = { calls++; DataGoKrBody(items = (1..750).toList(), totalCount = 750) },
            upsert = { it.size },
        )

        assertThat(calls).isEqualTo(1)
        assertThat(result.processed).isEqualTo(750)
    }
}
