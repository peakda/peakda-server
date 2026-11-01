package com.peakda.server.domain.festival.presentation.response

import com.peakda.server.domain.festival.application.FestivalPhase
import com.peakda.server.domain.festival.entity.FestivalKind
import com.peakda.server.domain.festival.entity.FestivalSource
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "관리자 축제 기본 정보. 직접 등록(MANUAL) 축제만 수정할 수 있다")
data class FestivalAdminDetailResponse(
    @field:Schema(description = "축제 id", example = "9001")
    val id: Long,

    @field:Schema(description = "데이터 출처. PUBLIC_DATA(공공데이터)·MANUAL(직접 등록)", example = "MANUAL")
    val source: FestivalSource,

    @field:Schema(description = "축제 종류. FESTIVAL(축제·행사)·TREE(트리 명소)", example = "TREE")
    val kind: FestivalKind,

    @field:Schema(description = "축제·트리 명소 이름", example = "더현대 서울 크리스마스 트리")
    val name: String,

    @field:Schema(description = "장소명", example = "더현대 서울 5층 사운즈 포레스트")
    val venue: String,

    @field:Schema(description = "시작일. 공공데이터 원천을 파싱할 수 없으면 null", example = "2026-11-14")
    val startsOn: LocalDate?,

    @field:Schema(description = "종료일. 없거나 파싱할 수 없으면 null", example = "2027-01-04")
    val endsOn: LocalDate?,

    @field:Schema(description = "도로명 주소. 없으면 null", example = "서울 영등포구 여의대로 108")
    val roadAddress: String?,

    @field:Schema(description = "위도. 없으면 null", example = "37.5259")
    val latitude: Double?,

    @field:Schema(description = "경도. 없으면 null", example = "126.9284")
    val longitude: Double?,

    @field:Schema(description = "홈페이지 URL. 없으면 null", example = "https://www.ehyundai.com")
    val homepageUrl: String?,

    @field:Schema(description = "문의 전화번호. 없으면 null", example = "02-767-2233")
    val phoneNumber: String?,

    @field:Schema(description = "주최. 없으면 null", example = "현대백화점")
    val hostOrganization: String?,

    @field:Schema(description = "주관. 없으면 null", example = "더현대 서울")
    val organizingInstitution: String?,

    @field:Schema(
        description = "오늘 기준 단계. UPCOMING(시작 전)·ONGOING(진행 중)·ENDING_SOON(종료 임박)·ENDED(종료). 시작일이 없으면 null",
        example = "UPCOMING",
    )
    val phase: FestivalPhase?,
)
