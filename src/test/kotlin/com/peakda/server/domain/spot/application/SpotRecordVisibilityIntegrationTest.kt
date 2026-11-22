package com.peakda.server.domain.spot.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.peakda.server.common.exception.GlobalExceptionHandler
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.common.test.IntegrationTestSupport
import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.repository.AdminAuditLogRepository
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.report.application.ReportAdminService
import com.peakda.server.domain.report.application.ReportReviewAction
import com.peakda.server.domain.report.application.ReportService
import com.peakda.server.domain.report.application.ReviewReportTargetCommand
import com.peakda.server.domain.report.entity.ReportReason
import com.peakda.server.domain.report.entity.ReportStatus
import com.peakda.server.domain.report.entity.ReportTargetType
import com.peakda.server.domain.report.exception.ReportAlreadyReviewedException
import com.peakda.server.domain.report.exception.ReportNotFoundException
import com.peakda.server.domain.report.repository.ReportRepository
import com.peakda.server.domain.spot.entity.*
import com.peakda.server.domain.spot.exception.SpotRecordInvalidStatusException
import com.peakda.server.domain.spot.presentation.SpotRecordAdminController
import com.peakda.server.domain.spot.presentation.SpotRecordController
import com.peakda.server.domain.spot.repository.*
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** 실제 PostgreSQL 커밋과 서비스 프록시를 사용한다. MVC는 기존 인증 완료 principal을 주입한다. */
class SpotRecordVisibilityIntegrationTest : IntegrationTestSupport() {
    @Autowired lateinit var records: SpotRecordRepository
    @Autowired lateinit var photos: SpotRecordPhotoRepository
    @Autowired lateinit var recordPlants: SpotRecordPlantRepository
    @Autowired lateinit var spots: SpotRepository
    @Autowired lateinit var plants: PlantRepository
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var reports: ReportRepository
    @Autowired lateinit var audits: AdminAuditLogRepository
    @Autowired lateinit var service: SpotRecordService
    @Autowired lateinit var moderation: SpotRecordModerationService
    @Autowired lateinit var admin: SpotRecordAdminService
    @Autowired lateinit var reportService: ReportService
    @Autowired lateinit var reportAdmin: ReportAdminService
    @Autowired lateinit var uploader: SpotRecordPhotoUploader
    @Autowired lateinit var backfill: SpotRecordPhotoVariantBackfillService
    @Autowired lateinit var transactions: TransactionTemplate
    @Autowired lateinit var mapper: ObjectMapper
    private lateinit var mvc: MockMvc
    private lateinit var owner: User
    private var spotId = 0L
    private var viewerId = 0L
    private val originalPublishedAt = Instant.parse("2026-11-01T01:02:03Z")

    @BeforeEach
    fun setUp() {
        owner = users.saveAndFlush(User(OAuth2LoginType.KAKAO, "visibility-owner", "숨김검증"))
        viewerId = requireNotNull(owner.id)
        spotId = requireNotNull(spots.saveAndFlush(Spot(SpotType.LOCAL, name = "숨김 테스트", latitude = 37.0, longitude = 127.0)).id)
        mvc = MockMvcBuilders.standaloneSetup(SpotRecordController(uploader, service), SpotRecordAdminController(admin, backfill))
            .setControllerAdvice(GlobalExceptionHandler())
            .setMessageConverters(MappingJackson2HttpMessageConverter(mapper))
            .setCustomArgumentResolvers(object : HandlerMethodArgumentResolver {
                override fun supportsParameter(parameter: MethodParameter) = parameter.parameterType == PrincipalDetails::class.java
                override fun resolveArgument(parameter: MethodParameter, container: ModelAndViewContainer?, request: NativeWebRequest, factory: WebDataBinderFactory?): Any {
                    val user = org.mockito.Mockito.mock(User::class.java)
                    org.mockito.Mockito.`when`(user.id).thenReturn(viewerId)
                    val principal = org.mockito.Mockito.mock(PrincipalDetails::class.java)
                    org.mockito.Mockito.`when`(principal.getUser()).thenReturn(user)
                    return principal
                }
            }).build()
    }

    @AfterEach
    fun cleanUp() {
        // 이 클래스가 생성한 데이터만 정리한다. seed 식물 및 다른 fixture는 보존한다.
        val ids = records.findAll().filter { it.spotId == spotId }.mapNotNull { it.id }
        transactions.executeWithoutResult {
            ids.forEach { photos.deleteBySpotRecordId(it); recordPlants.deleteByIdSpotRecordId(it) }
            records.deleteAllById(ids)
            reports.deleteAll(reports.findAll().filter { it.targetId in ids })
            audits.deleteAll(audits.findAll().filter { it.targetId in ids && it.adminId == owner.id })
            spots.deleteById(spotId)
            users.delete(owner)
        }
    }

    @Test
    fun `새 DB에서 관리자 숨김과 복구 및 재요청은 커밋되고 기존 HTTP envelope와 감사 로그를 유지한다`() {
        val id = saveRecord(SpotRecordStatus.PUBLISHED)
        for (status in listOf("HIDDEN", "HIDDEN", "PUBLISHED", "PUBLISHED")) {
            mvc.perform(patch("/api/admin/spot-records/$id/status").contentType(MediaType.APPLICATION_JSON).content("""{"status":"$status"}"""))
                .andExpect(status().isOk)
                .andExpect(content().json("""{"status":200,"code":"SUCCESS","message":"OK"}""", true))
            assertThat(records.findById(id).orElseThrow().status.name).isEqualTo(status)
            assertThat(records.findById(id).orElseThrow().publishedAt).isEqualTo(originalPublishedAt)
        }
        assertThat(audits.findAll().filter { it.targetId == id }.map { it.action })
            .containsExactly(AdminAuditAction.SPOT_RECORD_HIDE, AdminAuditAction.SPOT_RECORD_HIDE, AdminAuditAction.SPOT_RECORD_RESTORE, AdminAuditAction.SPOT_RECORD_RESTORE)
    }

    @Test
    fun `숨김 기록은 작성자 게시를 400으로 거부하고 타인 조회는 404로 유지한다`() {
        val id = saveRecord(SpotRecordStatus.PUBLISHED)
        moderation.hide(id)
        mvc.perform(post("/api/spots/records/$id/publish"))
            .andExpect(status().isBadRequest)
            .andExpect(content().json("""{"status":400,"code":"SPOT_RECORD_INVALID_STATUS","message":"게시에 필요한 필수 항목이 누락되었습니다."}""", true))
        assertThat(records.findById(id).orElseThrow().status).isEqualTo(SpotRecordStatus.HIDDEN)
        assertThat(records.findById(id).orElseThrow().publishedAt).isEqualTo(originalPublishedAt)
        mvc.perform(get("/api/spots/records/$id")).andExpect(status().isOk).andExpect(jsonPath("$.data.status").value("HIDDEN"))
        viewerId += 10000
        mvc.perform(get("/api/spots/records/$id")).andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("SPOT_RECORD_NOT_FOUND"))
        mvc.perform(post("/api/spots/records/$id/publish")).andExpect(status().isForbidden).andExpect(jsonPath("$.code").value("SPOT_RECORD_FORBIDDEN"))
    }

    @Test
    fun `준비된 초안은 기존 JSON으로 게시되고 재요청은 응답 전체와 게시시각을 유지한다`() {
        val id = saveRecord(SpotRecordStatus.DRAFT)
        recordPlants.saveAndFlush(SpotRecordPlant(SpotRecordPlantId(id, requireNotNull(plants.findAll().first().id))))
        // B1이 허용하는 기존 legacy 연결. publish는 사진 registry/variant를 수정하지 않는다.
        photos.saveAndFlush(SpotRecordPhoto(id, "legacy/visibility.jpg", 1, variantNames = "thumbnail,main"))
        mvc.perform(post("/api/spots/records/$id/publish"))
            .andExpect(status().isOk).andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.data.id").value(id)).andExpect(jsonPath("$.data.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.data.visitedDate").value("2026-11-01")).andExpect(jsonPath("$.data.bloomStage").value("PEAK"))
            .andExpect(jsonPath("$.data.contentLink").doesNotExist())
            .andExpect(jsonPath("$.data.photos[0].objectKey").value("legacy/visibility.jpg"))
            .andExpect(jsonPath("$.data.photos[0].sortOrder").value(1))
            .andExpect(jsonPath("$.data.photos[0].variants.medium").exists())
            .andExpect(jsonPath("$.data.reactions").exists())
        val publishedAt = records.findById(id).orElseThrow().publishedAt
        // 최초 응답 후 커밋 시 JPA auditing이 updatedAt을 변경한다. 재요청끼리는 모든 필드가 같다.
        val repeated = mvc.perform(post("/api/spots/records/$id/publish")).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.publishedAt").value(publishedAt.toString()))
            .andReturn().response.contentAsString
        mvc.perform(post("/api/spots/records/$id/publish")).andExpect(status().isOk).andExpect(content().json(repeated, true))
        assertThat(records.findById(id).orElseThrow().publishedAt).isNotNull()
        assertThat(photos.findBySpotRecordIdOrderBySortOrderAsc(id).single().variantNames).isEqualTo("thumbnail,main")
    }

    @Test
    fun `생성 API의 HIDDEN 입력은 기존 상태 오류로 거부하고 행을 만들지 않는다`() {
        mvc.perform(post("/api/spots/records").contentType(MediaType.APPLICATION_JSON).content(
            """{"spotInput":{"existingSpotId":$spotId,"type":"LOCAL","name":"숨김 테스트","latitude":37.0,"longitude":127.0},"status":"HIDDEN"}""",
        )).andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("SPOT_RECORD_INVALID_STATUS"))
        assertThat(records.findAll().filter { it.spotId == spotId }).isEmpty()
    }

    @Test
    fun `미완성 초안 게시와 관리자 초안 숨김은 기존 400이고 없는 기록은 404이다`() {
        val id = saveRecord(SpotRecordStatus.DRAFT)
        mvc.perform(post("/api/spots/records/$id/publish")).andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("SPOT_RECORD_INVALID_STATUS"))
        mvc.perform(patch("/api/admin/spot-records/$id/status").contentType(MediaType.APPLICATION_JSON).content("""{"status":"HIDDEN"}"""))
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("SPOT_RECORD_INVALID_STATUS"))
        mvc.perform(post("/api/spots/records/9223372036854775807/publish")).andExpect(status().isNotFound)
        assertThat(records.findById(id).orElseThrow().status).isEqualTo(SpotRecordStatus.DRAFT)
        assertThat(audits.findAll().filter { it.targetId == id }).isEmpty()
    }

    @Test
    fun `신고 심사 숨김은 신고 처리와 두 감사 로그를 함께 커밋하고 재심사는 복구를 되돌리지 않는다`() {
        val id = saveRecord(SpotRecordStatus.PUBLISHED)
        reportService.create(viewerId + 10000, ReportTargetType.SPOT_RECORD, id, ReportReason.SPAM, null)
        val command = ReviewReportTargetCommand(viewerId, ReportTargetType.SPOT_RECORD, id, ReportReviewAction.RESOLVE_HIDE, "검토 완료")
        reportAdmin.review(command)
        assertThat(records.findById(id).orElseThrow().status).isEqualTo(SpotRecordStatus.HIDDEN)
        val report = reports.findAll().single { it.targetId == id }
        assertThat(report.status).isEqualTo(ReportStatus.RESOLVED)
        assertThat(report.reviewedBy).isEqualTo(viewerId)
        assertThat(report.reviewedAt).isNotNull()
        assertThat(report.reviewMemo).isEqualTo("검토 완료")
        assertThat(audits.findAll().filter { it.targetId == id }.map { it.action })
            .containsExactlyInAnyOrder(AdminAuditAction.REPORT_RESOLVE_HIDE, AdminAuditAction.SPOT_RECORD_HIDE)
        moderation.restore(id)
        assertThatThrownBy { reportAdmin.review(command) }.isInstanceOf(ReportAlreadyReviewedException::class.java)
        assertThat(records.findById(id).orElseThrow().status).isEqualTo(SpotRecordStatus.PUBLISHED)
        assertThat(audits.findAll().filter { it.targetId == id }).hasSize(2)
    }

    @Test
    fun `심사할 신고가 없거나 외부 트랜잭션이 롤백되면 숨김과 감사는 남지 않는다`() {
        val id = saveRecord(SpotRecordStatus.PUBLISHED)
        assertThatThrownBy { reportAdmin.review(ReviewReportTargetCommand(viewerId, ReportTargetType.SPOT_RECORD, id, ReportReviewAction.RESOLVE_HIDE, null)) }
            .isInstanceOf(ReportNotFoundException::class.java)
        assertThat(records.findById(id).orElseThrow().status).isEqualTo(SpotRecordStatus.PUBLISHED)
        transactions.executeWithoutResult {
            admin.updateStatus(viewerId, id, SpotRecordStatus.HIDDEN)
            it.setRollbackOnly()
        }
        assertThat(records.findById(id).orElseThrow().status).isEqualTo(SpotRecordStatus.PUBLISHED)
        assertThat(audits.findAll().filter { it.targetId == id }).isEmpty()
    }

    @Test
    fun `숨김 트랜잭션이 행 잠금을 잡으면 작성자 게시가 기다린 뒤 HIDDEN을 거부한다`() {
        val id = saveRecord(SpotRecordStatus.PUBLISHED)
        val hidden = CountDownLatch(1)
        val release = CountDownLatch(1)
        val publishing = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val hide = executor.submit { transactions.executeWithoutResult {
                moderation.hide(id)
                hidden.countDown()
                check(release.await(10, TimeUnit.SECONDS))
            } }
            check(hidden.await(10, TimeUnit.SECONDS))
            val publish = executor.submit {
                publishing.countDown()
                assertThatThrownBy { service.publish(id, viewerId) }.isInstanceOf(SpotRecordInvalidStatusException::class.java)
            }
            check(publishing.await(10, TimeUnit.SECONDS))
            assertThatThrownBy { publish.get(200, TimeUnit.MILLISECONDS) }.isInstanceOf(TimeoutException::class.java)
            release.countDown()
            hide.get(10, TimeUnit.SECONDS)
            publish.get(10, TimeUnit.SECONDS)
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
        assertThat(records.findById(id).orElseThrow().status).isEqualTo(SpotRecordStatus.HIDDEN)
        assertThat(records.findById(id).orElseThrow().publishedAt).isEqualTo(originalPublishedAt)
    }

    private fun saveRecord(status: SpotRecordStatus): Long = requireNotNull(records.saveAndFlush(SpotRecord(
        spotId, requireNotNull(owner.id), visitedDate = LocalDate.of(2026, 11, 1), bloomStage = BloomStage.PEAK,
        status = status, publishedAt = if (status == SpotRecordStatus.DRAFT) null else originalPublishedAt,
    )).id)
}
