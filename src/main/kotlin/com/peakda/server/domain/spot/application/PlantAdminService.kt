package com.peakda.server.domain.spot.application

import com.peakda.server.domain.admin.application.AdminAuditRecorder
import com.peakda.server.domain.admin.application.RecordAdminAuditCommand
import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.spot.entity.PlantStatus
import com.peakda.server.domain.spot.entity.Season
import com.peakda.server.domain.spot.exception.PlantInvalidNameException
import com.peakda.server.domain.spot.exception.PlantNotFoundException
import com.peakda.server.domain.spot.exception.PlantSuggestionDuplicateException
import com.peakda.server.domain.spot.presentation.response.PlantAdminResponse
import com.peakda.server.domain.spot.repository.PlantRepository
import com.peakda.server.domain.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PlantAdminService(
    private val plantRepository: PlantRepository,
    private val userRepository: UserRepository,
    private val adminAuditRecorder: AdminAuditRecorder,
) {

    @Transactional(readOnly = true)
    fun list(status: PlantStatus?, suggestedOnly: Boolean, pageable: Pageable): Page<PlantAdminResponse> {
        val plants = when {
            suggestedOnly && status != null -> plantRepository.findByStatusAndSuggestedByUserIdIsNotNullOrderByIdDesc(
                status,
                pageable,
            )
            suggestedOnly -> plantRepository.findBySuggestedByUserIdIsNotNullOrderByIdDesc(pageable)
            status != null -> plantRepository.findByStatusOrderByIdDesc(status, pageable)
            else -> plantRepository.findAllByOrderByIdDesc(pageable)
        }
        val nicknames = userRepository.findAllById(
            plants.content.mapNotNull { it.suggestedByUserId }.distinct(),
        ).associate { requireNotNull(it.id) to it.nickname }

        return plants.map { plant ->
            PlantAdminResponse.from(plant, plant.suggestedByUserId?.let(nicknames::get))
        }
    }

    fun update(adminId: Long, plantId: Long, command: UpdatePlantCommand): PlantAdminResponse {
        val plant = plantRepository.findById(plantId).orElseThrow { PlantNotFoundException() }
        val beforeStatus = plant.status
        val changes = mutableListOf<String>()

        command.name?.let { requestedName ->
            val normalizedName = PlantNameNormalizer.normalize(requestedName)
            if (normalizedName.isEmpty()) throw PlantInvalidNameException()
            if (normalizedName != plant.name) {
                if (plantRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, plantId)) {
                    throw PlantSuggestionDuplicateException()
                }
                changes += "name: ${plant.name} → $normalizedName"
                plant.name = normalizedName
            }
        }
        command.sortOrder?.let { sortOrder ->
            if (plant.sortOrder != sortOrder) {
                changes += "sortOrder: ${plant.sortOrder} → $sortOrder"
                plant.sortOrder = sortOrder
            }
        }
        command.status?.let { status ->
            if (plant.status != status) {
                changes += "status: ${plant.status} → $status"
                plant.status = status
            }
        }
        command.bloomCategory?.let { bloomCategory ->
            if (plant.bloomCategory != bloomCategory) {
                changes += "bloomCategory: ${plant.bloomCategory?.name ?: "없음"} → ${bloomCategory.name}"
                plant.bloomCategory = bloomCategory
            }
        }
        command.seasons?.let { seasons ->
            val beforeSeasons = plant.seasons.toSet()
            plant.seasons.clear()
            plant.seasons.addAll(seasons)
            if (beforeSeasons != seasons) {
                changes += "seasons: ${beforeSeasons.display()} → ${seasons.display()}"
            }
        }

        adminAuditRecorder.record(
            RecordAdminAuditCommand(
                adminId = adminId,
                action = resolveAuditAction(beforeStatus, plant.status),
                targetType = AdminAuditTargetType.PLANT,
                targetId = plantId,
                memo = changes.joinToString(", ").ifEmpty { "변경 없음" },
            ),
        )

        val suggestedByNickname = plant.suggestedByUserId
            ?.let { userRepository.findById(it).orElse(null)?.nickname }
        return PlantAdminResponse.from(plant, suggestedByNickname)
    }

    private fun resolveAuditAction(before: PlantStatus, after: PlantStatus): AdminAuditAction = when {
        before != PlantStatus.REJECTED && after == PlantStatus.REJECTED -> AdminAuditAction.PLANT_REJECT
        before == PlantStatus.REJECTED && after == PlantStatus.ACTIVE -> AdminAuditAction.PLANT_RESTORE
        else -> AdminAuditAction.PLANT_UPDATE
    }

    private fun Set<Season>.display(): String =
        sorted().joinToString(prefix = "[", postfix = "]") { it.name }
}
