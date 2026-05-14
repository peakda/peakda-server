package com.peakda.server.infrastructure.scheduler

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.peakda.server.infrastructure.external.common.DataGoKrErrorDecoder
import com.peakda.server.infrastructure.external.common.ExternalApiLoggingInterceptor
import com.peakda.server.infrastructure.external.common.JsonOnlyInterceptor
import com.peakda.server.infrastructure.external.common.KtoCommonParamInterceptor
import com.peakda.server.infrastructure.external.common.ServiceKeyInterceptor
import com.peakda.server.infrastructure.scheduler.history.SchedulerJobRunRecorder
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestClient

internal data class ScheduledClientFixture<T>(
    val client: T,
    val server: MockRestServiceServer,
)

internal fun <T> ktoFixture(
    baseUrl: String,
    service: String,
    build: (RestClient) -> T,
): ScheduledClientFixture<T> {
    val builder = RestClient.builder()
        .baseUrl(baseUrl)
        .requestInterceptors {
            it.add(ServiceKeyInterceptor("test-key"))
            it.add(KtoCommonParamInterceptor("peakda-test"))
            it.add(JsonOnlyInterceptor())
            it.add(ExternalApiLoggingInterceptor("KTO", service))
        }
    val server = MockRestServiceServer.bindTo(builder).build()
    return ScheduledClientFixture(build(builder.build()), server)
}

internal fun <T> kmaFixture(
    baseUrl: String,
    service: String,
    build: (RestClient) -> T,
): ScheduledClientFixture<T> {
    val builder = RestClient.builder()
        .baseUrl(baseUrl)
        .requestInterceptors {
            it.add(ServiceKeyInterceptor("test-key"))
            it.add(JsonOnlyInterceptor())
            it.add(ExternalApiLoggingInterceptor("KMA", service))
        }
    val server = MockRestServiceServer.bindTo(builder).build()
    return ScheduledClientFixture(build(builder.build()), server)
}

internal fun <T> pubdataFixture(
    baseUrl: String,
    service: String,
    build: (RestClient) -> T,
): ScheduledClientFixture<T> {
    val builder = RestClient.builder()
        .baseUrl(baseUrl)
        .requestInterceptors {
            it.add(ServiceKeyInterceptor("test-key"))
            it.add(JsonOnlyInterceptor())
            it.add(ExternalApiLoggingInterceptor("PUBDATA", service))
        }
    val server = MockRestServiceServer.bindTo(builder).build()
    return ScheduledClientFixture(build(builder.build()), server)
}

internal val testObjectMapper get() = jacksonObjectMapper()
internal val testErrorDecoder get() = DataGoKrErrorDecoder()

internal class NoOpSchedulerJobRunRecorder : SchedulerJobRunRecorder {
    override fun start(jobName: String): Long? = null
    override fun complete(runId: Long?, processedCount: Int?, totalCount: Int?) = Unit
    override fun fail(runId: Long?, throwable: Throwable) = Unit
    override fun skip(jobName: String, reason: String) = Unit
}

internal object NoOpSchedulerJobLock : SchedulerJobLock {
    override fun <T> withLock(jobName: String, block: () -> T): SchedulerJobLockResult<T> =
        SchedulerJobLockResult.Acquired(block())
}

internal fun testJobLogger(): JobLogger = JobLogger(NoOpSchedulerJobRunRecorder(), SimpleMeterRegistry(), NoOpSchedulerJobLock)
