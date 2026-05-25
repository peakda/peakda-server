package com.peakda.server.domain.spot.presentation.request

import com.peakda.server.domain.spot.entity.BloomStage
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(
    description = "스팟 기록 부분 수정 — null 필드는 기존 값을 유지한다. " +
        "plantIds/photoKeys 는 제공 시 전체를 교체한다 (orphan 사진은 자동 정리).",
)
data class UpdateSpotRecordRequest(
    @field:Schema(description = "방문 일자 교체")
    val visitedDate: LocalDate? = null,

    @field:Schema(description = "개화/단풍 상태 교체")
    val bloomStage: BloomStage? = null,

    @field:Size(max = 1000)
    @field:Schema(description = "메모 교체 (최대 1000자, 빈 문자열을 보내면 메모 삭제)")
    val memo: String? = null,

    @field:Size(max = 10)
    @field:Schema(description = "식물 id 리스트 교체 (null 이면 변경하지 않음)")
    val plantIds: List<Long>? = null,

    @field:Size(max = 5)
    @field:Schema(description = "사진 key 리스트 교체 (null 이면 변경하지 않음, 빠진 key 는 자동 삭제)")
    val photoKeys: List<String>? = null,
)
