package com.peakda.server.domain.notification.presentation.request

import com.peakda.server.domain.notification.application.UpsertNoticeCommand
import com.peakda.server.domain.notification.entity.NotificationLinkType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

@Schema(description = "공지 등록·수정 요청")
data class UpsertNoticeRequest(
    @field:NotBlank
    @field:Size(max = 100)
    @field:Schema(description = "푸시와 알림함에 표시할 공지 제목", example = "서비스 점검 안내")
    val title: String,

    @field:NotBlank
    @field:Size(max = 2000)
    @field:Schema(description = "푸시와 알림함에 표시할 공지 본문", example = "7월 30일 오전 2시에 점검이 진행됩니다.")
    val body: String,

    @field:NotNull
    @field:Schema(description = "알림 선택 시 이동 방식", example = "EXTERNAL")
    val linkType: NotificationLinkType,

    @field:Size(max = 2048)
    @field:Schema(
        description = "EXTERNAL 이동 URL. 빈 문자열은 null로 정규화한다.",
        example = "https://peakda.example.com/notices/maintenance",
        nullable = true,
    )
    val linkUrl: String? = null,

    @field:Positive
    @field:Schema(description = "INTERNAL 이동 대상 id", example = "42", nullable = true)
    val targetId: Long? = null,
) {
    fun toCommand(): UpsertNoticeCommand = UpsertNoticeCommand(
        title = title,
        body = body,
        linkType = linkType,
        linkUrl = linkUrl,
        targetId = targetId,
    )
}
