package com.peakda.server.infrastructure.scheduler

import org.springframework.stereotype.Component

@Component
class ManualJobRegistry(
    jobs: List<ManualTriggerableJob>,
) {
    private val byName = jobs.associateBy { it.jobName }

    fun find(jobName: String): ManualTriggerableJob? = byName[jobName]

    fun names(): List<String> = byName.keys.sorted()
}
