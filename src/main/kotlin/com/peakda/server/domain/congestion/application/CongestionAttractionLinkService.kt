package com.peakda.server.domain.congestion.application

import com.peakda.server.domain.attraction.application.AttractionEligibilityProperties
import com.peakda.server.domain.attraction.repository.AttractionNameCandidate
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.congestion.entity.CongestionAttractionLink
import com.peakda.server.domain.congestion.repository.CongestionAttractionKey
import com.peakda.server.domain.congestion.repository.CongestionAttractionLinkRepository
import com.peakda.server.domain.congestion.repository.CongestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 집중률 관광지를 명소에 연결하는 배치 서비스. 시군구 단위로 후보를 한 번씩만 읽는다. */
@Service
class CongestionAttractionLinkService(
    private val congestionRepository: CongestionRepository,
    private val linkRepository: CongestionAttractionLinkRepository,
    private val attractionRepository: AttractionRepository,
    private val eligibility: AttractionEligibilityProperties,
) {
    /** [fromBaseDate] 이후 예측이 있는 관광지를 (지역, 시군구) 단위로 묶는다. 배치 실행당 한 번 호출한다. */
    @Transactional(readOnly = true)
    fun findTargetsBySigungu(fromBaseDate: String): Map<Pair<String, String>, List<String>> =
        congestionRepository.findAttractionKeysFrom(fromBaseDate)
            .groupBy({ it.areaCode to it.sigunguCode }, CongestionAttractionKey::touristAttractionName)

    /**
     * 한 시군구의 관광지명들을 판정해 연결을 upsert 한다.
     * 확정·거절된 연결은 사람이 내렸거나 이미 신뢰하는 결정이라 건너뛴다.
     */
    /**
     * @param reformCandidateCache 개편 후 시도 범위 후보를 배치 실행 한 번 동안 재사용하는 캐시.
     *   같은 시도의 사라진 시군구(광주·전남 약 27개)마다 시도 전체를 다시 읽지 않도록 잡이 실행마다 하나 만들어 넘긴다.
     */
    @Transactional
    fun linkSigungu(
        areaCode: String,
        sigunguCode: String,
        names: List<String>,
        reformCandidateCache: MutableMap<Set<String>, List<AttractionNameCandidate>> = mutableMapOf(),
    ): CongestionLinkSummary {
        val existing = linkRepository.findByAreaCodeAndSigunguCode(areaCode, sigunguCode)
            .associateBy { it.touristAttractionName }
        val targets = names.distinct().filter { existing[it]?.status?.reevaluable ?: true }
        if (targets.isEmpty()) return CongestionLinkSummary(skipped = names.size)

        val contentTypes = eligibility.eligibleContentTypes
        val sameSigungu = attractionRepository.findNameCandidatesBySigungu(sigunguCode, contentTypes)
        val reformArea = if (sameSigungu.isEmpty()) {
            val reformAreaCodes = CongestionAttractionMatcher.reformAreaCodes(areaCode)
            reformCandidateCache.getOrPut(reformAreaCodes) {
                attractionRepository.findNameCandidatesByAreas(reformAreaCodes, contentTypes)
            }
        } else {
            emptyList()
        }

        val decided = targets.map { name ->
            val decision = CongestionAttractionMatcher.decide(name, sameSigungu, reformArea)
            val link = existing[name] ?: CongestionAttractionLink(
                areaCode = areaCode,
                sigunguCode = sigunguCode,
                touristAttractionName = name,
                status = decision.status,
            )
            link.apply(decision.status, decision.matchType, decision.attractionId, decision.candidateIds)
            link
        }
        linkRepository.saveAll(decided)

        return CongestionLinkSummary(
            evaluated = decided.size,
            skipped = names.size - decided.size,
            byStatus = decided.groupingBy { it.status }.eachCount(),
        )
    }
}
