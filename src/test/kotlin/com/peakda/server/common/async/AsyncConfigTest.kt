package com.peakda.server.common.async

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AsyncConfigTest {

    private val decorator = AsyncConfig().mdcTaskDecorator()
    private val executor = Executors.newSingleThreadExecutor()

    @AfterEach
    fun tearDown() {
        MDC.clear()
        executor.shutdownNow()
    }

    @Test
    fun `호출한 스레드의 MDC 를 작업 스레드로 넘긴다`() {
        MDC.put("requestId", "req-1")
        var seen: String? = null

        executor.submit(decorator.decorate { seen = MDC.get("requestId") }).get(5, TimeUnit.SECONDS)

        assertThat(seen).isEqualTo("req-1")
    }

    @Test
    fun `작업이 끝나면 풀 스레드에 이전 요청의 MDC 가 남지 않는다`() {
        MDC.put("requestId", "req-1")
        executor.submit(decorator.decorate {}).get(5, TimeUnit.SECONDS)
        MDC.clear()

        var leaked: String? = "not-run"
        executor.submit { leaked = MDC.get("requestId") }.get(5, TimeUnit.SECONDS)

        assertThat(leaked).isNull()
    }
}
