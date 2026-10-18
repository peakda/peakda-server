package com.peakda.server.domain.auth.session.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그인된 기기 목록")
data class DeviceSessionListResponse(
    @field:Schema(description = "로그인된 기기 수", example = "3")
    val count: Int,

    @field:Schema(description = "최대 기기 수. 넘으면 가장 오래 안 쓴 기기가 자동으로 로그아웃된다.", example = "5")
    val maxCount: Int,

    @field:Schema(description = "기기 목록. 등록일 최신순")
    val devices: List<DeviceSessionResponse>,
)
