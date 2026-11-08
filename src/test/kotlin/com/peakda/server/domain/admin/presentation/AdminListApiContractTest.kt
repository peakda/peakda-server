package com.peakda.server.domain.admin.presentation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.peakda.server.common.security.jwt.JwtTokenGenerator
import com.peakda.server.common.test.IntegrationTestSupport
import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.entity.AdminAuditLog
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.admin.repository.AdminAuditLogRepository
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.festival.entity.Festival
import com.peakda.server.domain.festival.entity.FestivalEditorial
import com.peakda.server.domain.festival.entity.FestivalEditorialStatus
import com.peakda.server.domain.festival.entity.FestivalKind
import com.peakda.server.domain.festival.repository.FestivalEditorialRepository
import com.peakda.server.domain.festival.repository.FestivalRepository
import com.peakda.server.domain.location.entity.LocationAccessChannel
import com.peakda.server.domain.location.entity.LocationServiceType
import com.peakda.server.domain.location.entity.LocationUsageLog
import com.peakda.server.domain.location.repository.LocationUsageLogRepository
import com.peakda.server.domain.spot.entity.Plant
import com.peakda.server.domain.spot.entity.PlantStatus
import com.peakda.server.domain.spot.entity.Season
import com.peakda.server.domain.spot.repository.PlantRepository
import com.peakda.server.domain.user.entity.SignupChannel
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.entity.UserRole
import com.peakda.server.domain.user.entity.UserStatus
import com.peakda.server.domain.user.repository.UserRepository
import com.peakda.server.infrastructure.scheduler.history.SchedulerJobRun
import com.peakda.server.infrastructure.scheduler.history.SchedulerJobRunRepository
import com.peakda.server.infrastructure.scheduler.history.SchedulerJobStatus
import jakarta.persistence.EntityManager
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Transactional
class AdminListApiContractTest : IntegrationTestSupport() {
    @Autowired lateinit var context: WebApplicationContext
    @Autowired lateinit var json: ObjectMapper
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var entityManager: EntityManager
    @Autowired lateinit var tokens: JwtTokenGenerator
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var audits: AdminAuditLogRepository
    @Autowired lateinit var jobs: SchedulerJobRunRepository
    @Autowired lateinit var locations: LocationUsageLogRepository
    @Autowired lateinit var plants: PlantRepository
    @Autowired lateinit var festivals: FestivalRepository
    @Autowired lateinit var editorials: FestivalEditorialRepository

    private lateinit var mvc: MockMvc
    private lateinit var admin: User
    private lateinit var adminToken: String

    @BeforeEach
    fun setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
        admin = saveUser("C1Admin", role = UserRole.ADMIN)
        adminToken = accessToken(admin)
    }

    @Test
    fun `관리자 목록은 인증과 관리자 권한을 요구하고 쿠키와 Bearer를 받는다`() {
        val member = saveUser("C1Member")
        val memberToken = accessToken(member)
        for (path in listOf(USERS, AUDITS, RUNS, LOCATIONS, PLANTS, FESTIVALS)) {
            val unauthorized = mvc.perform(get(path)).andExpect(status().isUnauthorized).andReturn().response
            assertJson(unauthorized.getContentAsString(UTF_8), """
                {"status":401,"code":"UNAUTHORIZED","message":"인증이 필요합니다."}
            """.trimIndent())
            val forbidden = mvc.perform(get(path).header("Authorization", "Bearer $memberToken"))
                .andExpect(status().isForbidden).andReturn().response
            assertJson(forbidden.getContentAsString(UTF_8), """
                {"status":403,"code":"FORBIDDEN","message":"접근 권한이 없습니다."}
            """.trimIndent())
        }
        mvc.perform(get(AUDITS).cookie(Cookie("access-token", adminToken)))
            .andExpect(status().isOk)
        mvc.perform(get(AUDITS).cookie(Cookie("access-token", adminToken)).header("Authorization", "Bearer $memberToken"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `사용자 목록은 닉네임 리터럴 검색과 상태 역할 가입채널 필터를 조합한다`() {
        val older = saveUser("C1%_\\One", role = UserRole.ADMIN, channel = SignupChannel.APP)
        val newer = saveUser("C1%_\\Two", role = UserRole.ADMIN, channel = SignupChannel.APP)
        val suspended = saveUser("C1%_\\Off", UserStatus.SUSPENDED, UserRole.ADMIN, SignupChannel.APP)
        val member = saveUser("C1%_\\User", channel = SignupChannel.APP)
        val web = saveUser("C1%_\\Web", role = UserRole.ADMIN, channel = SignupChannel.WEB)
        val percentDecoy = saveUser("C1X_\\One")
        val underscoreDecoy = saveUser("C1%X\\One")
        val backslashDecoy = saveUser("C1%_XOne")
        val total = users.count()
        fixedTimestamp("users", newer.id!!)

        page(USERS, listOf(backslashDecoy.id, underscoreDecoy.id), total)
        page(USERS, listOf(backslashDecoy.id, underscoreDecoy.id), total, "q" to "   ")
        page(USERS, listOf(backslashDecoy.id, underscoreDecoy.id), 7, "q" to "c1%")
        page(USERS, listOf(backslashDecoy.id, web.id), 6, "q" to "%_")
        page(USERS, listOf(percentDecoy.id, web.id), 6, "q" to "_\\")
        page(USERS, listOf(web.id, member.id), 5, "q" to "  c1%_\\  ")
        page(USERS, listOf(member.id, newer.id), 3, "q" to "c1%_\\", "status" to "ACTIVE", "signupChannel" to "APP")
        page(USERS, listOf(web.id, member.id), 5, "q" to "c1%_\\")
        page(USERS, listOf(suspended.id), 5, "q" to "c1%_\\", "page" to "2", "size" to "1")
        val filters = arrayOf("q" to "c1%_\\", "status" to "ACTIVE", "role" to "ADMIN", "signupChannel" to "APP")
        val row = page(USERS, listOf(newer.id), 2, *filters, "size" to "1").single()
        assertJson(row.toString(), """
            {"id":${newer.id},"nickname":"C1%_\\Two","email":null,"provider":"KAKAO",
             "status":"ACTIVE","role":"ADMIN","signupChannel":"APP","createdAt":"$AT"}
        """.trimIndent())
        page(USERS, listOf(older.id), 2, *filters, "page" to "1", "size" to "1")
        page(USERS, emptyList(), 2, *filters, "page" to "2", "size" to "1")
        page(USERS, emptyList(), 0, "q" to "C1-no-match")
        assertThat(list(USERS).path("data").path("size").intValue()).isEqualTo(20)
        mvc.perform(get(USERS).header("Authorization", "Bearer $adminToken").param("size", "51"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `감사 목록은 세 선택 필터의 모든 조합과 감사자 배치 매핑을 유지한다`() {
        val other = saveUser("C1Auditor", role = UserRole.ADMIN)
        val target = 700001L
        val expected = audits.saveAndFlush(AdminAuditLog(admin.id!!, AdminAuditAction.USER_SUSPEND, AdminAuditTargetType.USER, target))
        val wrongAdmin = audits.saveAndFlush(AdminAuditLog(other.id!!, AdminAuditAction.USER_UNSUSPEND, AdminAuditTargetType.USER, target))
        val wrongTargetId = audits.saveAndFlush(AdminAuditLog(admin.id!!, AdminAuditAction.USER_SUSPEND, AdminAuditTargetType.USER, target + 1))
        val wrongType = audits.saveAndFlush(AdminAuditLog(admin.id!!, AdminAuditAction.USER_SUSPEND, AdminAuditTargetType.PLANT, target))
        val total = audits.count()
        fixedTimestamp("admin_audit_logs", expected.id!!)
        val type = "targetType" to "USER"
        val targetId = "targetId" to target.toString()
        val adminId = "adminId" to admin.id.toString()

        page(AUDITS, listOf(wrongType.id, wrongTargetId.id), total)
        page(AUDITS, listOf(wrongTargetId.id, wrongAdmin.id), 3, type)
        page(AUDITS, listOf(wrongType.id, wrongAdmin.id), 3, targetId)
        page(AUDITS, listOf(wrongType.id, wrongTargetId.id), 3, adminId)
        page(AUDITS, listOf(wrongAdmin.id, expected.id), 2, type, targetId)
        page(AUDITS, listOf(wrongTargetId.id, expected.id), 2, type, adminId)
        page(AUDITS, listOf(wrongType.id, expected.id), 2, targetId, adminId)
        page(AUDITS, listOf(expected.id), 2, type, targetId, "page" to "1", "size" to "1")
        val row = page(AUDITS, listOf(expected.id), 1, type, targetId, adminId).single()
        assertJson(row.toString(), """
            {"id":${expected.id},"adminId":${admin.id},"adminNickname":"C1Admin","action":"USER_SUSPEND",
             "targetType":"USER","targetId":$target,"memo":null,"createdAt":"$AT"}
        """.trimIndent())
    }

    @Test
    fun `잡 실행 목록은 since 경계와 시작 시각이 아닌 ID 내림차순을 유지한다`() {
        val before = jobs.saveAndFlush(SchedulerJobRun("c1Job", AT.minusSeconds(1), status = SchedulerJobStatus.FAILED))
        val after = jobs.saveAndFlush(SchedulerJobRun("c1Job", AT.plusSeconds(1), status = SchedulerJobStatus.FAILED))
        val boundary = jobs.saveAndFlush(SchedulerJobRun("c1Job", AT, status = SchedulerJobStatus.FAILED, errorMessage = "fixture error"))
        val completed = jobs.saveAndFlush(SchedulerJobRun("c1Job", AT, status = SchedulerJobStatus.COMPLETED))
        val other = jobs.saveAndFlush(SchedulerJobRun("c1Other", AT, status = SchedulerJobStatus.FAILED))
        val job = "jobName" to "c1Job"
        val failed = "status" to "FAILED"
        val since = "since" to AT.toString()

        page(RUNS, listOf(other.id, completed.id), jobs.count())
        page(RUNS, listOf(completed.id, boundary.id), 4, job)
        page(RUNS, listOf(other.id, boundary.id), 4, failed)
        page(RUNS, listOf(other.id, completed.id), 4, since)
        page(RUNS, listOf(other.id, boundary.id), 3, failed, since)
        page(RUNS, listOf(boundary.id, after.id), 3, job, failed)
        page(RUNS, listOf(before.id), 3, job, failed, "page" to "1")
        page(RUNS, listOf(completed.id, boundary.id), 3, job, since)
        val row = page(RUNS, listOf(boundary.id), 2, job, failed, since, "size" to "1").single()
        assertJson(row.toString(), """
            {"id":${boundary.id},"jobName":"c1Job","startedAt":"$AT","finishedAt":null,"status":"FAILED",
             "processedCount":null,"totalCount":null,"errorMessage":"fixture error","skipReason":null}
        """.trimIndent())
        page(RUNS, listOf(after.id), 2, job, failed, since, "page" to "1", "size" to "1")
        page(RUNS, emptyList(), 0, job, failed, "since" to AT.plusSeconds(2).toString())
    }

    @Test
    fun `위치 이력은 이메일 선조회와 양끝 포함 기간 및 사용자 행이 없는 이력을 유지한다`() {
        val owner = saveUser("C1Location", email = "c1%_\\@example.test")
        val decoy = saveUser("C1LocOther", email = "c1xx@example.test")
        val upper = AT.plusSeconds(10)
        val end = saveLocation(owner.id!!, upper)
        val start = saveLocation(owner.id!!, AT)
        val before = saveLocation(owner.id!!, AT.minusSeconds(1))
        val after = saveLocation(owner.id!!, upper.plusSeconds(1))
        val otherService = saveLocation(owner.id!!, AT, LocationServiceType.SPOT_MATCH)
        val otherOwner = saveLocation(decoy.id!!, AT)
        val missingOwner = saveLocation(800001L, AT)
        val email = "email" to "  C1%_\\  "
        val service = "service" to "BLOOM_MAP"
        val from = "from" to AT.toString()
        val to = "to" to upper.toString()

        page(LOCATIONS, listOf(missingOwner.id, otherOwner.id), locations.count())
        page(LOCATIONS, listOf(otherService.id, after.id), 5, email)
        page(LOCATIONS, listOf(after.id, start.id), 3, email, service, from)
        page(LOCATIONS, listOf(before.id, start.id), 3, email, service, to)
        val row = page(LOCATIONS, listOf(start.id), 2, email, service, from, to, "size" to "1").single()
        assertJson(row.toString(), """
            {"id":${start.id},"userId":${owner.id},"email":"c1%_\\@example.test","nickname":"C1Location",
             "channel":"ANDROID","service":"BLOOM_MAP","usedAt":"$AT"}
        """.trimIndent())
        page(LOCATIONS, listOf(end.id), 2, email, service, from, to, "page" to "1", "size" to "1")
        page(LOCATIONS, emptyList(), 0, "email" to "no-c1-user@example.test", service, from, to)
        val orphan = page(LOCATIONS, listOf(missingOwner.id, otherOwner.id), 4, service, from, to).first()
        assertJson(orphan.toString(), """
            {"id":${missingOwner.id},"userId":800001,"email":null,"nickname":null,
             "channel":"ANDROID","service":"BLOOM_MAP","usedAt":"$AT"}
        """.trimIndent())
    }

    @Test
    fun `식물 목록은 제안 여부와 상태의 네 조합을 ID 내림차순으로 조회한다`() {
        val pending = plants.saveAndFlush(Plant("C1 pending", 1, PlantStatus.PENDING, admin.id,
            seasons = mutableSetOf(Season.AUTUMN_WINTER, Season.SPRING)))
        val suggested = plants.saveAndFlush(Plant("C1 suggested", 0, PlantStatus.ACTIVE, admin.id))
        val curated = plants.saveAndFlush(Plant("C1 curated", 20, PlantStatus.ACTIVE))
        val notSuggested = plants.saveAndFlush(Plant("C1 no suggestion", 10, PlantStatus.PENDING))
        val total = plants.count()
        fixedTimestamp("plants", pending.id!!)
        val all = "suggestedOnly" to "false"
        val state = "status" to "PENDING"

        page(PLANTS, listOf(suggested.id, pending.id), 2)
        page(PLANTS, listOf(notSuggested.id, curated.id), total, all)
        page(PLANTS, listOf(notSuggested.id, pending.id), 2, all, state)
        val row = page(PLANTS, listOf(pending.id), 1, state, "suggestedOnly" to "true").single()
        assertJson(row.toString(), """
            {"id":${pending.id},"name":"C1 pending","sortOrder":1,"status":"PENDING",
             "suggestedByUserId":${admin.id},"suggestedByNickname":"C1Admin","approvedAt":null,
             "bloomCategory":null,"seasons":["SPRING","AUTUMN_WINTER"],"createdAt":"$AT","updatedAt":"$AT"}
        """.trimIndent())
        page(PLANTS, listOf(pending.id), 2, "page" to "1", "size" to "1")
        page(PLANTS, emptyList(), 0, "status" to "REJECTED")
    }

    @Test
    fun `축제 목록은 검색어 종류의 네 조합과 에디토리얼 병합을 유지한다`() {
        val older = festivals.saveAndFlush(Festival("C1%_\\ first", "fixture", "2025-01-01",
            startsOn = LocalDate.parse("2025-01-01"), endsOn = LocalDate.parse("2025-01-02")))
        val tree = festivals.saveAndFlush(Festival("C1%_\\ tree", "fixture", "2025-01-01", kind = FestivalKind.TREE))
        val newer = festivals.saveAndFlush(Festival("C1%_\\ second", "fixture", "2025-01-01"))
        val decoy = festivals.saveAndFlush(Festival("C1XX first", "fixture", "2025-01-01"))
        editorials.saveAndFlush(FestivalEditorial(older.id!!, status = FestivalEditorialStatus.PUBLISHED, pinnedAt = AT))
        val q = "q" to "  c1%_\\  "
        val kind = "kind" to "FESTIVAL"
        val total = festivals.count()

        page(FESTIVALS, listOf(decoy.id, newer.id), total)
        page(FESTIVALS, listOf(decoy.id, newer.id), total, "q" to "   ")
        page(FESTIVALS, listOf(newer.id, tree.id), 3, q)
        page(FESTIVALS, listOf(decoy.id, newer.id), 3, kind)
        val noEditorial = page(FESTIVALS, listOf(newer.id), 2, q, kind, "size" to "1").single()
        assertJson(noEditorial.toString(), """
            {"id":${newer.id},"source":"PUBLIC_DATA","kind":"FESTIVAL","name":"C1%_\\ second","venue":"fixture",
             "startsOn":null,"endsOn":null,"phase":null,"hasEditorial":false,"editorialStatus":null,"pinnedAt":null}
        """.trimIndent())
        val row = page(FESTIVALS, listOf(older.id), 2, q, kind, "size" to "1", "page" to "1").single()
        assertJson(row.toString(), """
            {"id":${older.id},"source":"PUBLIC_DATA","kind":"FESTIVAL","name":"C1%_\\ first","venue":"fixture",
             "startsOn":"2025-01-01","endsOn":"2025-01-02","phase":"ENDED","hasEditorial":true,
             "editorialStatus":"PUBLISHED","pinnedAt":"$AT"}
        """.trimIndent())
        page(FESTIVALS, emptyList(), 0, "q" to "C1-no-match", kind)
    }

    private fun list(path: String, vararg params: Pair<String, String>): JsonNode {
        val request = get(path).header("Authorization", "Bearer $adminToken")
        params.forEach { (name, value) -> request.param(name, value) }
        val response = mvc.perform(request).andExpect(status().isOk).andReturn().response
        return json.readTree(response.getContentAsString(UTF_8))
    }

    private fun page(path: String, ids: List<Long?>, total: Long, vararg params: Pair<String, String>): List<JsonNode> {
        val query = params.toMap()
        val page = query["page"]?.toInt() ?: 0
        val size = query["size"]?.toInt() ?: 2
        val result = list(path, *query.plus("size" to size.toString()).toList().toTypedArray())
        val content = result.path("data").path("content")
        val pages = (total + size - 1) / size
        assertJson(result.toString(), """
            {"status":200,"code":"SUCCESS","message":"OK","data":{"content":$content,"page":$page,
             "size":$size,"totalElements":$total,"totalPages":$pages,"hasNext":${page + 1 < pages}}}
        """.trimIndent())
        assertThat(content.isArray).isTrue()
        assertThat(content.map { it.path("id").takeIf(JsonNode::isIntegralNumber)?.longValue() })
            .describedAs("%s %s content IDs", path, query)
            .containsExactlyElementsOf(ids)
        return content.toList()
    }

    private fun assertJson(actual: String, expected: String) {
        assertThat(json.readTree(actual)).isEqualTo(json.readTree(expected))
    }

    private fun saveUser(
        nickname: String,
        status: UserStatus = UserStatus.ACTIVE,
        role: UserRole = UserRole.USER,
        channel: SignupChannel = SignupChannel.UNKNOWN,
        email: String? = null,
    ): User = users.saveAndFlush(User(OAuth2LoginType.KAKAO, "fixture-$nickname", nickname,
        email = email, status = status, role = role, signupChannel = channel))

    private fun saveLocation(userId: Long, at: Instant, service: LocationServiceType = LocationServiceType.BLOOM_MAP) =
        locations.saveAndFlush(LocationUsageLog(userId, LocationAccessChannel.ANDROID, service, at))

    private fun fixedTimestamp(table: String, id: Long) {
        jdbc.update("update $table set created_at = cast(? as timestamptz), updated_at = cast(? as timestamptz) where id = ?",
            AT.toString(), AT.toString(), id)
        entityManager.clear()
    }

    private fun accessToken(user: User): String = tokens.generateToken(
        userId = user.id!!,
        email = user.email,
        authorities = listOf("ROLE_${user.role.name}"),
        sessionId = UUID.randomUUID(),
        refreshTokenValidityInSeconds = 3600,
    ).accessToken

    companion object {
        private val AT = Instant.parse("2026-01-02T03:04:05Z")
        private const val USERS = "/api/admin/users"
        private const val AUDITS = "/api/admin/audit-logs"
        private const val RUNS = "/api/admin/jobs/runs"
        private const val LOCATIONS = "/api/admin/location-usage-logs"
        private const val PLANTS = "/api/admin/plants"
        private const val FESTIVALS = "/api/admin/festivals"
    }
}
