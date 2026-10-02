package com.peakda.server.domain.attraction.application

import com.peakda.server.domain.attraction.repository.AttractionOperatingInfoRepository
import com.peakda.server.domain.attraction.repository.AttractionOperatingInfoTarget
import com.peakda.server.infrastructure.external.kto.korservice.response.DetailInfoItem
import com.peakda.server.infrastructure.external.kto.korservice.response.DetailIntroItem
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AttractionOperatingInfoSyncService(
    private val repository: AttractionOperatingInfoRepository,
    private val eligibilityProperties: AttractionEligibilityProperties,
) {
    /**
     * 이번에 받을 명소를 [limit] 건까지 고른다.
     *
     * 운영 정보가 아예 없는 명소를 먼저 채우고, 남는 몫으로 관광공사가 수정한 명소를 다시 받는다.
     */
    @Transactional(readOnly = true)
    fun findTargets(limit: Int): List<AttractionOperatingInfoTarget> {
        if (limit <= 0) return emptyList()
        val contentTypes = eligibilityProperties.eligibleContentTypes
        val missing = repository.findTargetsWithoutOperatingInfo(contentTypes, PageRequest.of(0, limit))
        val remaining = limit - missing.size
        if (remaining <= 0) return missing
        return missing + repository.findTargetsWithOutdatedOperatingInfo(contentTypes, PageRequest.of(0, remaining))
    }

    @Transactional
    fun save(target: AttractionOperatingInfoTarget, intro: DetailIntroItem?, infos: List<DetailInfoItem>): Int =
        repository.upsert(target.toOperatingInfoUpsertCommand(intro, infos))
}
