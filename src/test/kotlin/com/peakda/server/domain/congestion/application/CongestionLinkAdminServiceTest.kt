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
import com.peakda.server.domain.congestion.repository.CongestionAttractionLinkRepository
import com.peakda.server.domain.spot.exception.AttractionNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant
import java.util.Optional

class CongestionLinkAdminServiceTest {
    private val linkRepository = mock(CongestionAttractionLinkRepository::class.java)
    private val attractionRepository = mock(AttractionRepository::class.java)
    private val auditRecorder = mock(AdminAuditRecorder::class.java)
    private val service = CongestionLinkAdminService(linkRepository, attractionRepository, auditRecorder)

    @Test
    fun `명소를 지정하지 않고 확정하면 제안된 명소와 매칭 근거를 그대로 확정한다`() {
        val link = pendingLink(attractionId = 3, candidates = "3")
        `when`(attractionRepository.existsById(3)).thenReturn(true)

        service.review(1, 42, ReviewCongestionLinkCommand(CongestionLinkReviewAction.CONFIRM, null))

        assertThat(link.status).isEqualTo(CongestionLinkStatus.CONFIRMED)
        assertThat(link.attractionId).isEqualTo(3)
        assertThat(link.matchType).isEqualTo(CongestionMatchType.CONTAINS)
        assertThat(auditAction()).isEqualTo(AdminAuditAction.CONGESTION_LINK_CONFIRM)
    }

    @Test
    fun `다른 명소를 지정해 확정하면 수동 지정으로 남긴다`() {
        val link = pendingLink(attractionId = null, candidates = "3,4")
        `when`(attractionRepository.existsById(4)).thenReturn(true)

        service.review(1, 42, ReviewCongestionLinkCommand(CongestionLinkReviewAction.CONFIRM, 4))

        assertThat(link.attractionId).isEqualTo(4)
        assertThat(link.matchType).isEqualTo(CongestionMatchType.MANUAL)
    }

    @Test
    fun `제안도 지정도 없이 확정하면 거부한다`() {
        pendingLink(attractionId = null, candidates = "3,4")

        assertThatThrownBy { service.review(1, 42, ReviewCongestionLinkCommand(CongestionLinkReviewAction.CONFIRM, null)) }
            .isInstanceOf(CongestionLinkAttractionRequiredException::class.java)
    }

    @Test
    fun `존재하지 않는 명소로는 확정할 수 없다`() {
        pendingLink(attractionId = null, candidates = null)
        `when`(attractionRepository.existsById(99)).thenReturn(false)

        assertThatThrownBy { service.review(1, 42, ReviewCongestionLinkCommand(CongestionLinkReviewAction.CONFIRM, 99)) }
            .isInstanceOf(AttractionNotFoundException::class.java)
    }

    @Test
    fun `거절하면 연결 명소를 비우고 후보는 남긴다`() {
        val link = pendingLink(attractionId = 3, candidates = "3")

        service.review(1, 42, ReviewCongestionLinkCommand(CongestionLinkReviewAction.REJECT, null))

        assertThat(link.status).isEqualTo(CongestionLinkStatus.REJECTED)
        assertThat(link.attractionId).isNull()
        assertThat(link.candidateIds).containsExactly(3L)
        assertThat(auditAction()).isEqualTo(AdminAuditAction.CONGESTION_LINK_REJECT)
    }

    private fun pendingLink(attractionId: Long?, candidates: String?): CongestionAttractionLink {
        val link = CongestionAttractionLink(
            areaCode = "41",
            sigunguCode = "41590",
            touristAttractionName = "수도사",
            attractionId = attractionId,
            candidateAttractionIds = candidates,
            matchType = CongestionMatchType.CONTAINS,
            status = CongestionLinkStatus.PENDING_REVIEW,
        )
        ReflectionTestUtils.setField(link, "id", 42L)
        ReflectionTestUtils.setField(link, "updatedAt", Instant.parse("2026-09-30T00:40:00Z"))
        `when`(linkRepository.findById(42)).thenReturn(Optional.of(link))
        return link
    }

    private fun auditAction(): AdminAuditAction {
        val captor = ArgumentCaptor.forClass(RecordAdminAuditCommand::class.java)
        verify(auditRecorder).record(captureCommand(captor))
        return captor.value.action
    }

    // Kotlin 비nullable 파라미터에 Mockito captor 가 null 을 넘기지 않도록 자리 채움 값을 돌려준다.
    private fun captureCommand(captor: ArgumentCaptor<RecordAdminAuditCommand>): RecordAdminAuditCommand =
        captor.capture() ?: RecordAdminAuditCommand(0, AdminAuditAction.CONGESTION_LINK_CONFIRM, AdminAuditTargetType.CONGESTION_LINK, 0)
}
