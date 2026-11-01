package com.peakda.server.domain.festival.application

import com.peakda.server.domain.admin.application.AdminAuditRecorder
import com.peakda.server.domain.admin.application.RecordAdminAuditCommand
import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.festival.entity.Festival
import com.peakda.server.domain.festival.entity.FestivalSource
import com.peakda.server.domain.festival.exception.FestivalDuplicatedException
import com.peakda.server.domain.festival.exception.FestivalInvalidCoordinatesException
import com.peakda.server.domain.festival.exception.FestivalInvalidPeriodException
import com.peakda.server.domain.festival.exception.FestivalNotEditableException
import com.peakda.server.domain.festival.exception.FestivalNotFoundException
import com.peakda.server.domain.festival.presentation.response.FestivalAdminDetailResponse
import com.peakda.server.domain.festival.repository.FestivalEditorialRepository
import com.peakda.server.domain.festival.repository.FestivalHighlightRepository
import com.peakda.server.domain.festival.repository.FestivalRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 공공데이터에 없는 축제·트리 명소를 관리자가 직접 등록·수정·삭제한다.
 * 공공데이터 행은 동기화가 소유하므로 조회만 하고 고치지 않는다.
 */
@Service
class FestivalAdminService(
    private val festivalRepository: FestivalRepository,
    private val festivalEditorialRepository: FestivalEditorialRepository,
    private val festivalHighlightRepository: FestivalHighlightRepository,
    private val adminAuditRecorder: AdminAuditRecorder,
    private val festivalDetailProperties: FestivalDetailProperties,
) {

    @Transactional(readOnly = true)
    fun detail(festivalId: Long, today: LocalDate): FestivalAdminDetailResponse {
        val festival = festivalRepository.findById(festivalId).orElseThrow { FestivalNotFoundException() }
        return festival.toAdminDetail(today)
    }

    @Transactional
    fun create(adminId: Long, command: SaveFestivalCommand): Long {
        command.validate()
        ensureNotDuplicated(command, festivalId = null)
        val festival = translateDuplicate {
            festivalRepository.saveAndFlush(
                Festival(
                    name = command.name,
                    venue = command.venue,
                    startDate = command.startsOn.toString(),
                    source = FestivalSource.MANUAL,
                    kind = command.kind,
                ).apply { applyCommand(command) },
            )
        }
        val festivalId = requireNotNull(festival.id)
        record(adminId, AdminAuditAction.FESTIVAL_CREATE, festivalId)
        return festivalId
    }

    @Transactional
    fun update(adminId: Long, festivalId: Long, command: SaveFestivalCommand) {
        val festival = findManual(festivalId)
        command.validate()
        ensureNotDuplicated(command, festivalId)
        festival.applyCommand(command)
        translateDuplicate { festivalRepository.flush() }
        record(adminId, AdminAuditAction.FESTIVAL_UPDATE, festivalId)
    }

    /** 직접 등록 축제를 에디토리얼·주요 볼거리와 함께 지운다. */
    @Transactional
    fun delete(adminId: Long, festivalId: Long) {
        val festival = findManual(festivalId)
        festivalEditorialRepository.findByFestivalId(festivalId)?.let { editorial ->
            festivalHighlightRepository.deleteByFestivalEditorialId(requireNotNull(editorial.id))
            festivalEditorialRepository.delete(editorial)
        }
        festivalRepository.delete(festival)
        record(adminId, AdminAuditAction.FESTIVAL_DELETE, festivalId)
    }

    private fun findManual(festivalId: Long): Festival {
        val festival = festivalRepository.findById(festivalId).orElseThrow { FestivalNotFoundException() }
        if (festival.source != FestivalSource.MANUAL) throw FestivalNotEditableException()
        return festival
    }

    private fun SaveFestivalCommand.validate() {
        if (endsOn != null && endsOn.isBefore(startsOn)) throw FestivalInvalidPeriodException()
        if ((latitude == null) != (longitude == null)) throw FestivalInvalidCoordinatesException()
    }

    /** 공공데이터 행과 같은 이름·장소·시작일이면 유니크 키가 겹쳐 동기화와 충돌하므로 막는다. */
    private fun ensureNotDuplicated(command: SaveFestivalCommand, festivalId: Long?) {
        val existing = festivalRepository.findByNameAndVenueAndStartDate(
            command.name,
            command.venue,
            command.startsOn.toString(),
        ) ?: return
        if (existing.id != festivalId) throw FestivalDuplicatedException()
    }

    /** 사전 검사와 저장 사이에 동기화가 같은 키를 먼저 넣은 경우도 중복으로 응답한다. */
    private fun <T> translateDuplicate(block: () -> T): T = try {
        block()
    } catch (e: DataIntegrityViolationException) {
        throw FestivalDuplicatedException()
    }

    private fun Festival.applyCommand(command: SaveFestivalCommand) {
        kind = command.kind
        name = command.name
        venue = command.venue
        startDate = command.startsOn.toString()
        endDate = command.endsOn?.toString()
        startsOn = command.startsOn
        endsOn = command.endsOn
        roadAddress = command.roadAddress
        latitude = command.latitude
        longitude = command.longitude
        homepageUrl = command.homepageUrl
        phoneNumber = command.phoneNumber
        hostOrganization = command.hostOrganization
        organizingInstitution = command.organizingInstitution
    }

    private fun record(adminId: Long, action: AdminAuditAction, festivalId: Long) {
        adminAuditRecorder.record(
            RecordAdminAuditCommand(
                adminId = adminId,
                action = action,
                targetType = AdminAuditTargetType.FESTIVAL,
                targetId = festivalId,
            ),
        )
    }

    private fun Festival.toAdminDetail(today: LocalDate): FestivalAdminDetailResponse = FestivalAdminDetailResponse(
        id = requireNotNull(id),
        source = source,
        kind = kind,
        name = name,
        venue = venue,
        startsOn = startsOn,
        endsOn = endsOn,
        roadAddress = roadAddress,
        latitude = latitude,
        longitude = longitude,
        homepageUrl = homepageUrl,
        phoneNumber = phoneNumber,
        hostOrganization = hostOrganization,
        organizingInstitution = organizingInstitution,
        phase = FestivalPhaseResolver.resolve(startsOn, endsOn, today, festivalDetailProperties.endingSoonDays),
    )
}
