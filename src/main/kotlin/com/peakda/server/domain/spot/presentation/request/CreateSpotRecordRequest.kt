package com.peakda.server.domain.spot.presentation.request

import com.peakda.server.domain.spot.entity.BloomStage
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(
    description = "스팟 기록 생성 — DRAFT 면 임시저장, PUBLISHED 면 즉시 게시. " +
        "사용자에게 기존 DRAFT 가 있으면 같은 행을 덮어쓰거나 promote 한다.",
)
data class CreateSpotRecordRequest(
    @field:Valid
    @field:Schema(description = "스팟 식별·생성 입력")
    val spotInput: SpotInputRequest,

    @field:Schema(description = "방문 일자 (PUBLISHED 시 필수)", example = "2026-05-22")
    val visitedDate: LocalDate? = null,

    @field:Schema(description = "개화/단풍 상태 (PUBLISHED 시 필수)", example = "PEAK")
    val bloomStage: BloomStage? = null,

    @field:Size(max = 1000)
    @field:Schema(description = "메모 (최대 1000자)", example = "벚꽃이 정말 만개했어요.")
    val memo: String? = null,

    @field:Size(max = 10)
    @field:Schema(description = "선택한 식물 id 리스트. PUBLISHED 시 1개 이상, ACTIVE 상태만 허용.")
    val plantIds: List<Long> = emptyList(),

    @field:Size(max = 5)
    @field:Schema(
        description = "사전 업로드된 사진 key 리스트 (순서 = 표시 순서). PUBLISHED 시 1~5장.",
        example = "[\"spot-records/42/2026/05/uuid/main.jpg\"]",
    )
    val photoKeys: List<String> = emptyList(),

    @field:Schema(description = "기록 상태", example = "DRAFT")
    val status: SpotRecordStatus,
)
