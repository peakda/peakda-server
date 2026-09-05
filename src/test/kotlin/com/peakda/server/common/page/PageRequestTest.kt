package com.peakda.server.common.page

import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort

class PageRequestTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `기본값은 page=0 size=20`() {
        val request = PageRequest()

        assertThat(request.page).isEqualTo(0)
        assertThat(request.size).isEqualTo(20)
    }

    @Test
    fun `toPageable 은 주입된 정렬을 적용한다`() {
        val request = PageRequest(page = 2, size = 10)
        val sort = Sort.by(Sort.Direction.DESC, "createdAt")

        val pageable = request.toPageable(sort)

        assertThat(pageable.pageNumber).isEqualTo(2)
        assertThat(pageable.pageSize).isEqualTo(10)
        assertThat(pageable.sort).isEqualTo(sort)
    }

    @Test
    fun `toPageable 정렬 기본값은 unsorted`() {
        val pageable = PageRequest().toPageable()

        assertThat(pageable.sort.isUnsorted).isTrue()
    }

    @Test
    fun `page 가 음수면 검증 실패`() {
        val violations = validator.validate(PageRequest(page = -1, size = 20))

        assertThat(violations).hasSize(1)
        assertThat(violations.first().propertyPath.toString()).isEqualTo("page")
    }

    @Test
    fun `size 가 0 이하면 검증 실패`() {
        val violations = validator.validate(PageRequest(page = 0, size = 0))

        assertThat(violations).hasSize(1)
        assertThat(violations.first().propertyPath.toString()).isEqualTo("size")
    }

    @Test
    fun `size 가 50 을 넘으면 검증 실패`() {
        val violations = validator.validate(PageRequest(page = 0, size = 51))

        assertThat(violations).hasSize(1)
        assertThat(violations.first().propertyPath.toString()).isEqualTo("size")
    }

    @Test
    fun `정상 범위는 검증 통과`() {
        val violations = validator.validate(PageRequest(page = 0, size = 50))

        assertThat(violations).isEmpty()
    }
}
