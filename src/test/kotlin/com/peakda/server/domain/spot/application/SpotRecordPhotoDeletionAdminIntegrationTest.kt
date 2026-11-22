package com.peakda.server.domain.spot.application

import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.common.test.IntegrationTestSupport
import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.repository.AdminAuditLogRepository
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.spot.entity.PhotoDeletionStatus
import com.peakda.server.domain.spot.entity.SpotRecordPhotoDeletion
import com.peakda.server.domain.spot.entity.SpotRecordPhotoUpload
import com.peakda.server.domain.spot.repository.SpotRecordPhotoDeletionRepository
import com.peakda.server.domain.spot.repository.SpotRecordPhotoUploadRepository
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.entity.UserRole
import com.peakda.server.domain.user.repository.UserRepository
import com.peakda.server.infrastructure.scheduler.ManualJobExecutor
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.history.SchedulerJobRunRepository
import com.peakda.server.infrastructure.scheduler.history.SchedulerJobStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import org.redisson.api.RLock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

class SpotRecordPhotoDeletionAdminIntegrationTest : IntegrationTestSupport() {
    @Autowired lateinit var context: WebApplicationContext
    @Autowired lateinit var jobs: SpotRecordPhotoDeletionRepository
    @Autowired lateinit var uploads: SpotRecordPhotoUploadRepository
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var audits: AdminAuditLogRepository
    @Autowired lateinit var runs: SchedulerJobRunRepository
    @MockitoBean lateinit var executor: ManualJobExecutor
    @MockitoBean lateinit var storage: ObjectStorage

    @Test fun `기존 관리자 권한과 감사 경로에서 FAILED를 재처리하고 202 envelope를 유지한다`() {
        val admin = users.saveAndFlush(User(OAuth2LoginType.KAKAO, UUID.randomUUID().toString(), "복구관리자", role = UserRole.ADMIN))
        val member = users.saveAndFlush(User(OAuth2LoginType.KAKAO, UUID.randomUUID().toString(), "복구일반"))
        val key = "spot-records/${member.id}/2026/11/${UUID.randomUUID()}/main.jpg"
        val jobName = "spotRecordPhotoDeletionRetryFailed"
        val keyUpload = uploads.saveAndFlush(SpotRecordPhotoUpload(key, member.id!!, "thumbnail,main", "SERVER_UPLOAD", available = false))
        val job = jobs.saveAndFlush(SpotRecordPhotoDeletion(key, member.id!!, "SERVER_UPLOAD", "thumbnail,main", Instant.now()).apply {
            status = PhotoDeletionStatus.FAILED
            attempts = 5
            totalAttempts = 5
            deletionStarted = true
            completedVariants = "thumbnail"
            lastError = "StorageException"
        })
        try {
            val lock = mock(RLock::class.java)
            `when`(redissonClient.getLock(anyString())).thenReturn(lock)
            `when`(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(true)
            `when`(lock.isHeldByCurrentThread).thenReturn(true)
            val registered = context.getBean(com.peakda.server.infrastructure.scheduler.ManualJobRegistry::class.java).find(jobName)!!
            doAnswer { it.getArgument<ManualTriggerableJob>(0).runNow(); null }.`when`(executor).execute(registered)
            val mvc = MockMvcBuilders.webAppContextSetup(context).apply<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(springSecurity()).build()
            val path = "/api/admin/jobs/$jobName/trigger"
            mvc.perform(post(path)).andExpect(status().isUnauthorized)
            mvc.perform(post(path).with(user(PrincipalDetails(member)))).andExpect(status().isForbidden)
            verifyNoInteractions(storage, executor)
            mvc.perform(get("/api/admin/jobs").with(user(PrincipalDetails(admin))))
                .andExpect(status().isOk).andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[?(@.jobName == '$jobName')]").isNotEmpty)
            mvc.perform(post(path).with(user(PrincipalDetails(admin))))
                .andExpect(status().isAccepted).andExpect(jsonPath("$.code").value("SUCCESS"))
            val completed = jobs.findById(job.id!!).orElseThrow()
            assertThat(completed.status).isEqualTo(PhotoDeletionStatus.COMPLETED)
            assertThat(completed.reprocessCount).isEqualTo(1)
            assertThat(completed.totalAttempts).isEqualTo(6)
            assertThat(completed.lastError).isEqualTo("StorageException")
            verify(storage, never()).delete("${key.substringBeforeLast('/')}/thumbnail.jpg")
            verify(storage).delete(key)
            assertThat(audits.findAll().filter { it.adminId == admin.id && it.memo == jobName }.single().action)
                .isEqualTo(AdminAuditAction.SCHEDULER_JOB_TRIGGER)
            assertThat(runs.findAll().filter { it.jobName == jobName }.single().status).isEqualTo(SchedulerJobStatus.COMPLETED)
        } finally {
            jobs.deleteById(job.id!!)
            uploads.delete(keyUpload)
            audits.deleteAll(audits.findAll().filter { it.adminId == admin.id && it.memo == jobName })
            runs.deleteAll(runs.findAll().filter { it.jobName == jobName })
            users.deleteAll(listOf(admin, member))
        }
    }
}
