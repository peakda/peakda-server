package com.peakda.server.domain.festival.presentation.request

import com.peakda.server.domain.festival.entity.FestivalKind
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "관리자 직접 등록 축제·트리 명소 등록·수정 요청")
data class SaveFestivalRequest(
    @field:NotNull
    @field:Schema(description = "축제 종류. FESTIVAL(축제·행사)·TREE(트리 명소)", example = "TREE")
    val kind: FestivalKind,

    @field:NotBlank
    @field:Size(max = 200)
    @field:Schema(description = "축제·트리 명소 이름", example = "더현대 서울 크리스마스 트리")
    val name: String,

    @field:NotBlank
    @field:Size(max = 200)
    @field:Schema(description = "장소명", example = "더현대 서울 5층 사운즈 포레스트")
    val venue: String,

    @field:NotNull
    @field:Schema(description = "운영 시작일", example = "2026-11-14")
    val startsOn: LocalDate,

    @field:Schema(description = "운영 종료일. 하루짜리면 null", example = "2027-01-04")
    val endsOn: LocalDate? = null,

    @field:Size(max = 500)
    @field:Schema(description = "도로명 주소. 없으면 null", example = "서울 영등포구 여의대로 108")
    val roadAddress: String? = null,

    @field:DecimalMin("-90.0")
    @field:DecimalMax("90.0")
    @field:Schema(description = "위도. 경도와 함께 입력하며 없으면 null", example = "37.5259")
    val latitude: Double? = null,

    @field:DecimalMin("-180.0")
    @field:DecimalMax("180.0")
    @field:Schema(description = "경도. 위도와 함께 입력하며 없으면 null", example = "126.9284")
    val longitude: Double? = null,

    @field:Size(max = 2000)
    @field:Pattern(regexp = "^https?://\\S+$")
    @field:Schema(description = "http·https 홈페이지 URL. 없으면 null", example = "https://www.ehyundai.com")
    val homepageUrl: String? = null,

    @field:Size(max = 100)
    @field:Schema(description = "문의 전화번호. 없으면 null", example = "02-767-2233")
    val phoneNumber: String? = null,

    @field:Size(max = 200)
    @field:Schema(description = "주최. 없으면 null", example = "현대백화점")
    val hostOrganization: String? = null,

    @field:Size(max = 200)
    @field:Schema(description = "주관. 없으면 null", example = "더현대 서울")
    val organizingInstitution: String? = null,
)
