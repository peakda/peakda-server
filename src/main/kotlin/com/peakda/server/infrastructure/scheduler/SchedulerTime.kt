package com.peakda.server.infrastructure.scheduler

import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal object SchedulerTime {
    val KST: ZoneId = ZoneId.of("Asia/Seoul")
    val YMD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    val HH00: DateTimeFormatter = DateTimeFormatter.ofPattern("HH00")
}
