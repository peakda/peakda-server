package com.peakda.server.infrastructure.scheduler.history

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

interface SchedulerJobRunRecorder {
    fun start(jobName: String): Long?
    fun complete(runId: Long?, processedCount: Int?, totalCount: Int?)
    fun fail(runId: Long?, throwable: Throwable)
}

@Component
class JpaSchedulerJobRunRecorder(
    private val repository: SchedulerJobRunRepository,
) : SchedulerJobRunRecorder {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun start(jobName: String): Long {
        val run = SchedulerJobRun(jobName = jobName, startedAt = Instant.now())
        return repository.save(run).id!!
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun complete(runId: Long?, processedCount: Int?, totalCount: Int?) {
        val runIdNonNull = runId ?: return
        val run = repository.findById(runIdNonNull).orElse(null) ?: return
        run.complete(Instant.now(), processedCount, totalCount)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun fail(runId: Long?, throwable: Throwable) {
        val runIdNonNull = runId ?: return
        val run = repository.findById(runIdNonNull).orElse(null) ?: return
        run.fail(Instant.now(), throwable.message, stackTraceOf(throwable))
    }

    private fun stackTraceOf(throwable: Throwable): String {
        val writer = StringWriter()
        PrintWriter(writer).use(throwable::printStackTrace)
        return writer.toString().take(STACK_LIMIT)
    }

    companion object {
        private const val STACK_LIMIT = 8000
    }
}
