package com.peakda.server.domain.attraction.application

import com.peakda.server.domain.attraction.repository.AttractionOperatingInfoTarget
import com.peakda.server.domain.attraction.repository.AttractionOperatingInfoUpsertCommand
import com.peakda.server.infrastructure.external.kto.korservice.response.DetailInfoItem
import com.peakda.server.infrastructure.external.kto.korservice.response.DetailIntroItem
import org.springframework.web.util.HtmlUtils

/** 반복정보에서 입장료로 읽는 제목. 원문은 "입 장 료"처럼 글자 사이에 공백이 섞여 있어 공백을 지우고 비교한다. */
private val ADMISSION_FEE_INFO_NAMES = setOf("입장료", "이용요금", "관람료")
private val LINE_BREAK = Regex("(?i)<br\\s*/?>")
private val HTML_TAG = Regex("<[^>]+>")
private val INLINE_SPACES = Regex("[ \\t\\u00A0]+")
private val WHITESPACE = Regex("\\s+")

/**
 * 관광지 소개정보에는 이용시간·쉬는날·주차만 있고 입장료는 반복정보에 따로 있어 두 응답을 합친다.
 * 소개정보가 없으면(빈 응답) 해당 항목을 null 로 둔다.
 */
fun AttractionOperatingInfoTarget.toOperatingInfoUpsertCommand(
    intro: DetailIntroItem?,
    infos: List<DetailInfoItem>,
) = AttractionOperatingInfoUpsertCommand(
    attractionId = attractionId,
    operatingHours = cleanTourText(intro?.usetime),
    closedDays = cleanTourText(intro?.restdate),
    admissionFee = infos
        .filter { it.infoname.replace(WHITESPACE, "") in ADMISSION_FEE_INFO_NAMES }
        .mapNotNull { cleanTourText(it.infotext) }
        .distinct()
        .joinToString("\n")
        .ifEmpty { null },
    parking = cleanTourText(intro?.parking),
    sourceModifiedAt = externalModifiedAt,
)

/** 관광공사 본문의 `<br>` 을 줄바꿈으로 바꾸고 나머지 태그·HTML 엔티티·겹친 공백을 걷어낸다. 남는 글자가 없으면 null. */
internal fun cleanTourText(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val text = HtmlUtils.htmlUnescape(raw.replace(LINE_BREAK, "\n").replace(HTML_TAG, ""))
    return text.lines()
        .map { it.replace(INLINE_SPACES, " ").trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n")
        .ifEmpty { null }
}
