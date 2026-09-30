package com.peakda.server.domain.congestion.application

import com.peakda.server.domain.admin.application.AdminAuditRecorder
import com.peakda.server.domain.admin.application.RecordAdminAuditCommand
import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.congestion.entity.CongestionAttractionLink
import com.peakda.server.domain.congestion.entity.CongestionLinkStatus
import com.peakda.server.domain.congestion.entity.CongestionMatchType
import com.peakda.server.domain.congestion.exception.CongestionLinkAttractionRequiredException
import com.peakda.server.domain.congestion.exception.CongestionLinkNotFoundException
import com.peakda.server.domain.congestion.presentation.response.CongestionLinkAdminResponse
import com.peakda.server.domain.congestion.repository.CongestionAttractionLinkRepository
import com.peakda.server.domain.spot.exception.AttractionNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 매칭 잡이 자동 확정하지 못한 혼잡도 연결을 관리자가 확정·거절한다. */
@Service
class CongestionLinkAdminService(
    private val linkRepository: CongestionAttractionLinkRepository,
    private val attractionRepository: AttractionRepository,
    private val adminAuditRecorder: AdminAuditRecorder,
) {
    @Transactional(readOnly = true)
    fun list(status: CongestionLinkStatus, pageable: Pageable): Page<CongestionLinkAdminResponse> {
        val page = linkRepository.findByStatusOrderByIdAsc(status, pageable)
        val attractionIds = page.content.flatMap { listOfNotNull(it.attractionId) + it.candidateIds }.toSet()
        val attractions = attractionRepository.findAllById(attractionIds).associateBy { requireNotNull(it.id) }
        return page.map { CongestionLinkAdminResponse.from(it, attractions) }
    }

    @Transactional
    fun review(adminId: Long, linkId: Long, command: ReviewCongestionLinkCommand): CongestionLinkAdminResponse {
        val link = linkRepository.findById(linkId).orElseThrow { CongestionLinkNotFoundException() }
        val before = "${link.status}/${link.attractionId}"
        when (command.action) {
            CongestionLinkReviewAction.CONFIRM -> confirm(link, command.attractionId)
            CongestionLinkReviewAction.REJECT ->
                link.apply(CongestionLinkStatus.REJECTED, link.matchType, null, link.candidateIds)
        }

        adminAuditRecorder.record(
            RecordAdminAuditCommand(
                adminId = adminId,
                action = when (command.action) {
                    CongestionLinkReviewAction.CONFIRM -> AdminAuditAction.CONGESTION_LINK_CONFIRM
                    CongestionLinkReviewAction.REJECT -> AdminAuditAction.CONGESTION_LINK_REJECT
                },
                targetType = AdminAuditTargetType.CONGESTION_LINK,
                targetId = linkId,
                memo = "${link.touristAttractionName}: $before → ${link.status}/${link.attractionId}",
            ),
        )

        val attractionIds = listOfNotNull(link.attractionId) + link.candidateIds
        val attractions = attractionRepository.findAllById(attractionIds).associateBy { requireNotNull(it.id) }
        return CongestionLinkAdminResponse.from(link, attractions)
    }

    private fun confirm(link: CongestionAttractionLink, requestedAttractionId: Long?) {
        val attractionId = requestedAttractionId ?: link.attractionId ?: throw CongestionLinkAttractionRequiredException()
        if (!attractionRepository.existsById(attractionId)) throw AttractionNotFoundException()
        // 제안된 명소를 그대로 확정하면 매칭 근거를 유지하고, 다른 명소를 고르면 수동 지정으로 남긴다.
        val matchType = link.matchType.takeIf { attractionId == link.attractionId } ?: CongestionMatchType.MANUAL
        link.apply(CongestionLinkStatus.CONFIRMED, matchType, attractionId, link.candidateIds)
    }
}
