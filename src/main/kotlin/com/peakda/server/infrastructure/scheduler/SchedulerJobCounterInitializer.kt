package com.peakda.server.infrastructure.scheduler

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * 기동 시 잡별 성공·실패 카운터를 0 으로 미리 만든다.
 *
 * 카운터는 처음 증가할 때 생기므로 첫 실패는 시계열이 값 1 로 "나타나는" 모양이 된다.
 * Prometheus 의 increase() 는 없던 시계열의 첫 값을 증가로 보지 않아, 배포(재기동) 뒤 첫 실패가
 * 알림과 대시보드에서 빠진다. 대부분 하루 1회 잡이라 그 첫 실패가 가장 중요하다.
 */
@Component
class SchedulerJobCounterInitializer(
    private val meterRegistry: MeterRegistry,
    private val registry: ManualJobRegistry,
) {
    @EventListener(ApplicationReadyEvent::class)
    fun initialize() {
        registry.names().forEach { jobName ->
            meterRegistry.counter(SUCCESS_METRIC, "job", jobName)
            meterRegistry.counter(FAILURE_METRIC, "job", jobName)
        }
    }

    companion object {
        const val SUCCESS_METRIC = "scheduler.job.success_total"
        const val FAILURE_METRIC = "scheduler.job.failure_total"
    }
}
