package com.peakda.server.domain.festival.application

import com.peakda.server.domain.admin.application.AdminAuditRecorder
import com.peakda.server.domain.admin.application.RecordAdminAuditCommand
import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.festival.entity.Festival
import com.peakda.server.domain.festival.entity.FestivalEditorial
import com.peakda.server.domain.festival.entity.FestivalEditorialStatus
import com.peakda.server.domain.festival.entity.FestivalKind
import com.peakda.server.domain.festival.entity.FestivalSource
import com.peakda.server.domain.festival.exception.FestivalDuplicatedException
import com.peakda.server.domain.festival.exception.FestivalInvalidCoordinatesException
import com.peakda.server.domain.festival.exception.FestivalInvalidPeriodException
import com.peakda.server.domain.festival.exception.FestivalNotEditableException
import com.peakda.server.domain.festival.exception.FestivalNotFoundException
import com.peakda.server.domain.festival.repository.FestivalEditorialRepository
import com.peakda.server.domain.festival.repository.FestivalHighlightRepository
import com.peakda.server.domain.festival.repository.FestivalRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.util.Optional

class FestivalAdminServiceTest {

    private val festivalRepository = mock(FestivalRepository::class.java)
    private val festivalEditorialRepository = mock(FestivalEditorialRepository::class.java)
    private val festivalHighlightRepository = mock(FestivalHighlightRepository::class.java)
    private val adminAuditRecorder = mock(AdminAuditRecorder::class.java)
    private val service = FestivalAdminService(
        festivalRepository,
        festivalEditorialRepository,
        festivalHighlightRepository,
        adminAuditRecorder,
        FestivalDetailProperties(endingSoonDays = 7),
    )

    @Test
    fun `직접 등록하면 출처를 MANUAL로, 시작·종료일을 ISO 원문과 정규화 날짜에 함께 저장한다`() {
        `when`(festivalRepository.saveAndFlush(anyFestival())).thenAnswer { invocation ->
            invocation.getArgument<Festival>(0).also { ReflectionTestUtils.setField(it, "id", FESTIVAL_ID) }
        }

        val festivalId = service.create(ADMIN_ID, command())

        assertThat(festivalId).isEqualTo(FESTIVAL_ID)
        val captor = ArgumentCaptor.forClass(Festival::class.java)
        verify(festivalRepository).saveAndFlush(captor.capture() ?: festival(Long.MIN_VALUE, FestivalSource.MANUAL))
        val saved = captor.value
        assertThat(saved.source).isEqualTo(FestivalSource.MANUAL)
        assertThat(saved.kind).isEqualTo(FestivalKind.TREE)
        assertThat(saved.name).isEqualTo("더현대 서울 크리스마스 트리")
        assertThat(saved.startDate).isEqualTo("2026-11-14")
        assertThat(saved.endDate).isEqualTo("2027-01-04")
        assertThat(saved.startsOn).isEqualTo(STARTS_ON)
        assertThat(saved.endsOn).isEqualTo(ENDS_ON)
        assertThat(saved.latitude).isEqualTo(37.5259)
        assertThat(saved.organizingInstitution).isEqualTo("더현대 서울")
        verify(adminAuditRecorder).record(audit(AdminAuditAction.FESTIVAL_CREATE))
    }

    @Test
    fun `종료일이 시작일보다 빠르면 등록하지 않는다`() {
        assertThatThrownBy { service.create(ADMIN_ID, command(endsOn = STARTS_ON.minusDays(1))) }
            .isInstanceOf(FestivalInvalidPeriodException::class.java)
        verify(festivalRepository, never()).saveAndFlush(anyFestival())
    }

    @Test
    fun `위도와 경도 중 하나만 있으면 등록하지 않는다`() {
        assertThatThrownBy { service.create(ADMIN_ID, command(longitude = null)) }
            .isInstanceOf(FestivalInvalidCoordinatesException::class.java)
        verify(festivalRepository, never()).saveAndFlush(anyFestival())
    }

    @Test
    fun `같은 이름·장소·시작일 축제가 있으면 등록하지 않는다`() {
        `when`(festivalRepository.findByNameAndVenueAndStartDate(NAME, VENUE, "2026-11-14"))
            .thenReturn(festival(OTHER_ID, FestivalSource.PUBLIC_DATA))

        assertThatThrownBy { service.create(ADMIN_ID, command()) }
            .isInstanceOf(FestivalDuplicatedException::class.java)
        verify(festivalRepository, never()).saveAndFlush(anyFestival())
    }

    @Test
    fun `사전 검사 뒤 같은 키가 먼저 저장돼 유니크 제약에 걸려도 중복으로 응답한다`() {
        `when`(festivalRepository.saveAndFlush(anyFestival()))
            .thenThrow(DataIntegrityViolationException("uk_festivals_name_venue_start"))

        assertThatThrownBy { service.create(ADMIN_ID, command()) }
            .isInstanceOf(FestivalDuplicatedException::class.java)
        verifyNoInteractions(adminAuditRecorder)
    }

    @Test
    fun `수정 반영 시 유니크 제약에 걸리면 중복으로 응답한다`() {
        val festival = festival(FESTIVAL_ID, FestivalSource.MANUAL)
        `when`(festivalRepository.findById(FESTIVAL_ID)).thenReturn(Optional.of(festival))
        `when`(festivalRepository.flush()).thenThrow(DataIntegrityViolationException("uk_festivals_name_venue_start"))

        assertThatThrownBy { service.update(ADMIN_ID, FESTIVAL_ID, command()) }
            .isInstanceOf(FestivalDuplicatedException::class.java)
        verifyNoInteractions(adminAuditRecorder)
    }

    @Test
    fun `직접 등록 축제를 수정하면 기본 정보를 요청 값으로 전부 교체한다`() {
        val festival = festival(FESTIVAL_ID, FestivalSource.MANUAL)
        `when`(festivalRepository.findById(FESTIVAL_ID)).thenReturn(Optional.of(festival))
        `when`(festivalRepository.findByNameAndVenueAndStartDate(NAME, VENUE, "2026-11-14")).thenReturn(festival)

        service.update(
            ADMIN_ID,
            FESTIVAL_ID,
            command(kind = FestivalKind.FESTIVAL, endsOn = null, latitude = null, longitude = null, homepageUrl = null),
        )

        assertThat(festival.kind).isEqualTo(FestivalKind.FESTIVAL)
        assertThat(festival.endDate).isNull()
        assertThat(festival.endsOn).isNull()
        assertThat(festival.latitude).isNull()
        assertThat(festival.longitude).isNull()
        assertThat(festival.homepageUrl).isNull()
        verify(adminAuditRecorder).record(audit(AdminAuditAction.FESTIVAL_UPDATE))
    }

    @Test
    fun `공공데이터 축제는 수정하지 않는다`() {
        val festival = festival(FESTIVAL_ID, FestivalSource.PUBLIC_DATA)
        `when`(festivalRepository.findById(FESTIVAL_ID)).thenReturn(Optional.of(festival))

        assertThatThrownBy { service.update(ADMIN_ID, FESTIVAL_ID, command()) }
            .isInstanceOf(FestivalNotEditableException::class.java)
        assertThat(festival.name).isEqualTo("원래 이름")
        verifyNoInteractions(adminAuditRecorder)
    }

    @Test
    fun `없는 축제를 수정하면 찾을 수 없음 예외다`() {
        `when`(festivalRepository.findById(FESTIVAL_ID)).thenReturn(Optional.empty())

        assertThatThrownBy { service.update(ADMIN_ID, FESTIVAL_ID, command()) }
            .isInstanceOf(FestivalNotFoundException::class.java)
    }

    @Test
    fun `직접 등록 축제를 지우면 볼거리·에디토리얼·축제 순으로 지운다`() {
        val festival = festival(FESTIVAL_ID, FestivalSource.MANUAL)
        val editorial = FestivalEditorial(festivalId = FESTIVAL_ID, status = FestivalEditorialStatus.PUBLISHED)
        ReflectionTestUtils.setField(editorial, "id", EDITORIAL_ID)
        `when`(festivalRepository.findById(FESTIVAL_ID)).thenReturn(Optional.of(festival))
        `when`(festivalEditorialRepository.findByFestivalId(FESTIVAL_ID)).thenReturn(editorial)

        service.delete(ADMIN_ID, FESTIVAL_ID)

        val order = inOrder(festivalHighlightRepository, festivalEditorialRepository, festivalRepository)
        order.verify(festivalHighlightRepository).deleteByFestivalEditorialId(EDITORIAL_ID)
        order.verify(festivalEditorialRepository).delete(editorial)
        order.verify(festivalRepository).delete(festival)
        verify(adminAuditRecorder).record(audit(AdminAuditAction.FESTIVAL_DELETE))
    }

    @Test
    fun `공공데이터 축제는 지우지 않는다`() {
        val festival = festival(FESTIVAL_ID, FestivalSource.PUBLIC_DATA)
        `when`(festivalRepository.findById(FESTIVAL_ID)).thenReturn(Optional.of(festival))

        assertThatThrownBy { service.delete(ADMIN_ID, FESTIVAL_ID) }
            .isInstanceOf(FestivalNotEditableException::class.java)
        verify(festivalRepository, never()).delete(anyFestival())
    }

    @Test
    fun `기본 정보 조회는 오늘 기준 단계를 함께 내려준다`() {
        `when`(festivalRepository.findById(FESTIVAL_ID))
            .thenReturn(Optional.of(festival(FESTIVAL_ID, FestivalSource.MANUAL)))

        assertThat(service.detail(FESTIVAL_ID, STARTS_ON.minusDays(1)).phase).isEqualTo(FestivalPhase.UPCOMING)
        assertThat(service.detail(FESTIVAL_ID, STARTS_ON).phase).isEqualTo(FestivalPhase.ONGOING)
        assertThat(service.detail(FESTIVAL_ID, ENDS_ON.minusDays(7)).phase).isEqualTo(FestivalPhase.ENDING_SOON)
        assertThat(service.detail(FESTIVAL_ID, ENDS_ON.plusDays(1)).phase).isEqualTo(FestivalPhase.ENDED)
    }

    private fun command(
        kind: FestivalKind = FestivalKind.TREE,
        endsOn: LocalDate? = ENDS_ON,
        latitude: Double? = 37.5259,
        longitude: Double? = 126.9284,
        homepageUrl: String? = "https://www.ehyundai.com",
    ): SaveFestivalCommand = SaveFestivalCommand(
        kind = kind,
        name = NAME,
        venue = VENUE,
        startsOn = STARTS_ON,
        endsOn = endsOn,
        roadAddress = "서울 영등포구 여의대로 108",
        latitude = latitude,
        longitude = longitude,
        homepageUrl = homepageUrl,
        phoneNumber = "02-767-2233",
        hostOrganization = "현대백화점",
        organizingInstitution = "더현대 서울",
    )

    private fun festival(id: Long, source: FestivalSource): Festival {
        val festival = Festival(
            name = "원래 이름",
            venue = "원래 장소",
            startDate = "2026-11-01",
            endDate = ENDS_ON.toString(),
            startsOn = STARTS_ON,
            endsOn = ENDS_ON,
            latitude = 37.0,
            longitude = 127.0,
            source = source,
            kind = FestivalKind.TREE,
        )
        ReflectionTestUtils.setField(festival, "id", id)
        return festival
    }

    private fun audit(action: AdminAuditAction): RecordAdminAuditCommand = RecordAdminAuditCommand(
        adminId = ADMIN_ID,
        action = action,
        targetType = AdminAuditTargetType.FESTIVAL,
        targetId = FESTIVAL_ID,
    )

    private fun anyFestival(): Festival = any(Festival::class.java) ?: festival(Long.MIN_VALUE, FestivalSource.MANUAL)

    companion object {
        private const val ADMIN_ID = 7L
        private const val FESTIVAL_ID = 9001L
        private const val OTHER_ID = 9002L
        private const val EDITORIAL_ID = 201L
        private const val NAME = "더현대 서울 크리스마스 트리"
        private const val VENUE = "더현대 서울 5층 사운즈 포레스트"
        private val STARTS_ON = LocalDate.of(2026, 11, 14)
        private val ENDS_ON = LocalDate.of(2027, 1, 4)
    }
}
