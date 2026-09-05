package com.peakda.server.common.page

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest as SpringPageRequest

class PageResponseTest {

    @Test
    fun `Page 로부터 메타데이터를 보존하며 변환된다`() {
        val items = listOf("a", "b", "c")
        val page = PageImpl(items, SpringPageRequest.of(0, 3), 7)

        val response = PageResponse.of(page)

        assertThat(response.content).containsExactly("a", "b", "c")
        assertThat(response.page).isEqualTo(0)
        assertThat(response.size).isEqualTo(3)
        assertThat(response.totalElements).isEqualTo(7)
        assertThat(response.totalPages).isEqualTo(3)
        assertThat(response.hasNext).isTrue()
    }

    @Test
    fun `마지막 페이지면 hasNext 는 false`() {
        val page = PageImpl(listOf("z"), SpringPageRequest.of(2, 3), 7)

        val response = PageResponse.of(page)

        assertThat(response.page).isEqualTo(2)
        assertThat(response.totalPages).isEqualTo(3)
        assertThat(response.hasNext).isFalse()
    }

    @Test
    fun `빈 페이지도 정상 변환된다`() {
        val page = PageImpl(emptyList<String>(), SpringPageRequest.of(0, 20), 0)

        val response = PageResponse.of(page)

        assertThat(response.content).isEmpty()
        assertThat(response.totalElements).isEqualTo(0)
        assertThat(response.totalPages).isEqualTo(0)
        assertThat(response.hasNext).isFalse()
    }

    @Test
    fun `확장함수 toPageResponse 는 of 와 동일하게 동작한다`() {
        val page = PageImpl(listOf(1, 2), SpringPageRequest.of(0, 2), 5)

        val viaExt = page.toPageResponse()
        val viaOf = PageResponse.of(page)

        assertThat(viaExt).isEqualTo(viaOf)
    }
}
