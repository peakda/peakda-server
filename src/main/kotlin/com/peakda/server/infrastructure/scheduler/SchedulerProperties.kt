package com.peakda.server.infrastructure.scheduler

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "external.scheduler")
data class SchedulerProperties(
    val enabled: Boolean = false,
    val kto: KtoSchedulerProps = KtoSchedulerProps(),
    val kma: KmaSchedulerProps = KmaSchedulerProps(),
    val pubdata: PubdataSchedulerProps = PubdataSchedulerProps(),
) {
    data class JobProps(
        val cron: String = "",
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
    )

    data class PubdataSchedulerProps(
        val festival: JobProps = JobProps(),
    )
}
