package com.peakda.server.domain.notification.presentation.response

import com.peakda.server.domain.notification.entity.Notice
import com.peakda.server.domain.notification.entity.NoticeStatus
import com.peakda.server.domain.notification.entity.NotificationLinkType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "관리자 공지 응답")
data class NoticeResponse(
    @field:Schema(description = "공지 id", example = "12")
    val id: Long,
    @field:Schema(description = "공지 제목", example = "서비스 점검 안내")
    val title: String,
    @field:Schema(description = "공지 본문", example = "7월 30일 오전 2시에 점검이 진행됩니다.")
    val body: String,
    @field:Schema(description = "알림 선택 시 이동 방식", example = "EXTERNAL")
    val linkType: NotificationLinkType,
    @field:Schema(description = "외부 이동 URL", nullable = true)
    val linkUrl: String?,
    @field:Schema(description = "내부 이동 대상 id", nullable = true)
    val targetId: Long?,
    @field:Schema(description = "공지 발송 상태", example = "DISPATCHING")
    val status: NoticeStatus,
    @field:Schema(description = "작성 관리자 사용자 id", example = "1")
    val createdBy: Long,
    @field:Schema(description = "전체 팬아웃 완료 시각", nullable = true)
    val dispatchedAt: Instant?,
    @field:Schema(description = "중복 방지 로그 기준 발송 사용자 수", example = "1500")
    val sentCount: Int,
    @field:Schema(description = "생성 시각")
    val createdAt: Instant,
    @field:Schema(description = "최종 수정 시각")
    val updatedAt: Instant,
) {
    companion object {
        fun from(notice: Notice): NoticeResponse = NoticeResponse(
            id = requireNotNull(notice.id),
            title = notice.title,
            body = notice.body,
            linkType = notice.linkType,
            linkUrl = notice.linkUrl,
            targetId = notice.targetId,
            status = notice.status,
            createdBy = notice.createdBy,
            dispatchedAt = notice.dispatchedAt,
            sentCount = notice.sentCount,
            createdAt = notice.createdAt,
            updatedAt = notice.updatedAt,
        )
    }
}
