package com.peakda.server.infrastructure.scheduler

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 잡별 마지막 성공 시각(epoch seconds)을 게이지로 발행한다.
 *
 * 잡이 실패하면 `scheduler.job.failure_total` 이 오르지만, **아예 실행되지 않은 경우**
 * (스케줄러 비활성화, 앱 다운, 락 미해제)는 어떤 카운터도 움직이지 않아 조용히 지나간다.
 * 미실행을 감시하려면 "마지막으로 성공한 시각"이라는 절대 기준이 따로 있어야 한다.
 *
 * 한 번도 성공한 적 없는 잡은 시계열이 만들어지지 않는다. 0 으로 채우면 잡을 새로 추가하는
 * 즉시 "임계값 넘게 미실행" 으로 오탐하기 때문이다. 첫 성공 전까지는 실패 카운터가 감시한다.
 *
 * 게이지는 프로세스 메모리에만 있어 재기동하면 사라진다. 그 공백은
 * [SchedulerJobSuccessGaugeRestorer] 가 실행 이력에서 되살려 메운다.
 */
@Component
class SchedulerJobSuccessGauge(
    private val meterRegistry: MeterRegistry,
) {
    private val lastSuccessEpochSeconds = ConcurrentHashMap<String, AtomicLong>()

    fun record(jobName: String, succeededAt: Instant) {
        gaugeOf(jobName).set(succeededAt.epochSecond)
    }

    /**
     * 게이지는 잡당 한 번만 등록하고 이후에는 값만 갱신한다.
     * Micrometer 는 게이지 대상을 약참조로 잡으므로 [lastSuccessEpochSeconds] 가
     * 참조를 붙들고 있지 않으면 수집 도중 값이 NaN 으로 사라진다.
     */
    private fun gaugeOf(jobName: String): AtomicLong =
        lastSuccessEpochSeconds.computeIfAbsent(jobName) { name ->
            AtomicLong().also { holder ->
                meterRegistry.gauge(METRIC_NAME, Tags.of("job", name), holder) { it.get().toDouble() }
            }
        }

    companion object {
        const val METRIC_NAME = "scheduler.job.last_success_timestamp"
    }
}
