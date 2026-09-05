package com.peakda.server.infrastructure.scheduler

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.LocalDate

@ConfigurationProperties(prefix = "external.scheduler")
data class SchedulerProperties(
    val enabled: Boolean = false,
    val kto: KtoSchedulerProps = KtoSchedulerProps(),
    val kma: KmaSchedulerProps = KmaSchedulerProps(),
    val pubdata: PubdataSchedulerProps = PubdataSchedulerProps(),
    val seasonal: SeasonalSchedulerProps = SeasonalSchedulerProps(),
    val notification: NotificationSchedulerProps = NotificationSchedulerProps(),
) {
    data class JobProps(
        val cron: String = "",
        val enabled: Boolean = true,
    )

    data class FixedDelayJobProps(
        val fixedDelay: String = "30s",
        val enabled: Boolean = true,
    )

    data class VilageFcstJobProps(
        val cron: String = "",
        val enabled: Boolean = true,
        val grids: List<Grid> = emptyList(),
    ) {
        data class Grid(
            val name: String = "",
            val nx: Int = 0,
            val ny: Int = 0,
        )
    }

    data class AsosDalyJobProps(
        val cron: String = "",
        val enabled: Boolean = true,
        /** 관측 이력이 전혀 없는 지점을 이 날짜부터 백필한다. 카테고리별 누적 시작일과 무관하게 연초부터 보관한다. */
        val backfillFrom: LocalDate = LocalDate.of(2026, 1, 1),
        /** 1회 실행당 지점별 최대 조회 일수. 첫 실행이 과도하게 커지는 것을 막는다. */
        val maxBackfillDays: Long = 400,
        /** 종관관측 지점번호 목록. */
        val stations: List<String> = emptyList(),
    )

    data class KtoSchedulerProps(
        val korService: JobProps = JobProps(),
        val durunubi: JobProps = JobProps(),
        val tatsCnctr: JobProps = JobProps(),
        val dataLab: JobProps = JobProps(),
        val photo: JobProps = JobProps(),
    )

    data class KmaSchedulerProps(
        val vilageFcst: VilageFcstJobProps = VilageFcstJobProps(),
        val midFcst: JobProps = JobProps(),
        val asosDaly: AsosDalyJobProps = AsosDalyJobProps(),
        val flowerObservation: JobProps = JobProps(),
    )

    data class PubdataSchedulerProps(
        val festival: JobProps = JobProps(),
    )

    data class SeasonalSchedulerProps(
        val attractionBloomTagging: JobProps = JobProps(),
        val attractionStationMapping: JobProps = JobProps(),
        val bloomEstimate: JobProps = JobProps(),
    )

    data class NotificationSchedulerProps(
        val bloomTimingAlert: JobProps = JobProps(),
        val noticeDispatch: FixedDelayJobProps = FixedDelayJobProps(),
        val deviceTokenCleanup: JobProps = JobProps(),
    )
}
