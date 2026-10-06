package com.peakda.server.domain.attraction.application

import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.attraction.repository.AttractionThumbnailCheckTarget
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant

/** 명소 썸네일 URL 이 실제로 열리는지 확인한 결과를 고르고 저장한다. 판정은 [com.peakda.server.domain.attraction.entity.Attraction.cardImageUrl] 이 쓴다. */
@Service
class AttractionThumbnailCheckService(
    private val repository: AttractionRepository,
    private val eligibilityProperties: AttractionEligibilityProperties,
    private val clock: Clock = Clock.systemUTC(),
) {
    /**
     * 이번에 확인할 명소를 [limit] 건까지 고른다.
     *
     * 현재 썸네일 URL 을 아직 확인하지 않은 명소를 먼저 고르고, 남는 몫으로 [recheckAfter] 가 지난 명소를 다시 확인한다.
     * 관광공사가 나중에 썸네일을 만들어 넣거나 지울 수 있어서다.
     */
    @Transactional(readOnly = true)
    fun findTargets(limit: Int, recheckAfter: Duration): List<AttractionThumbnailCheckTarget> {
        if (limit <= 0) return emptyList()
        val contentTypes = eligibilityProperties.eligibleContentTypes
        val unchecked = repository.findThumbnailUncheckedTargets(contentTypes, PageRequest.of(0, limit))
        val remaining = limit - unchecked.size
        if (remaining <= 0) return unchecked
        val checkedBefore = Instant.now(clock).minus(recheckAfter)
        return unchecked + repository.findThumbnailCheckedBefore(contentTypes, checkedBefore, PageRequest.of(0, remaining))
    }

    @Transactional
    fun save(target: AttractionThumbnailCheckTarget, missing: Boolean) {
        repository.updateThumbnailCheck(target.attractionId, target.thumbnailImageUrl, missing, Instant.now(clock))
    }
}
