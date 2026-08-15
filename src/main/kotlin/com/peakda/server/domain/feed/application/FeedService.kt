package com.peakda.server.domain.feed.application

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.page.toPageResponse
import com.peakda.server.domain.feed.entity.FeedFilter
import com.peakda.server.domain.spot.application.SpotRecordResponseAssembler
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.exception.SpotRecordNotFoundException
import com.peakda.server.domain.spot.presentation.response.SpotRecordResponse
import com.peakda.server.domain.spot.presentation.response.SpotRecordSummaryResponse
import com.peakda.server.domain.spot.repository.PlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordPlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import com.peakda.server.domain.user.repository.BlockRepository
import com.peakda.server.domain.user.repository.FollowRepository
import com.peakda.server.domain.user.repository.UserFavoriteCategoryRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 피드 조회(SCR-023/023b) — 게시된 스팟 기록을 전체 / 관심 식물(결정 B) / 팔로잉 축으로 필터링한다.
 *
 * `listBySpot`(스팟별 기록, SCR-025b)은 [com.peakda.server.domain.spot.application.SpotRecordService] 를 그대로 재활용한다.
 */
@Service
class FeedService(
    private val spotRecordRepository: SpotRecordRepository,
    private val spotRecordPlantRepository: SpotRecordPlantRepository,
    private val plantRepository: PlantRepository,
    private val followRepository: FollowRepository,
    private val blockRepository: BlockRepository,
    private val userFavoriteCategoryRepository: UserFavoriteCategoryRepository,
    private val responseAssembler: SpotRecordResponseAssembler,
) {

    @Transactional(readOnly = true)
    fun list(userId: Long, filter: FeedFilter, pageRequest: PageRequest): PageResponse<SpotRecordSummaryResponse> {
        val pageable = pageRequest.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
        val blockedIds = blockRepository.findBlockedIdsByBlockerId(userId).toSet()
        val page = when (filter) {
            FeedFilter.ALL -> allPage(blockedIds, pageable)
            FeedFilter.FOLLOWING -> followingPage(userId, blockedIds, pageable)
            FeedFilter.INTEREST -> interestPage(userId, blockedIds, pageable)
        }
        val summariesById = responseAssembler.assembleSummaries(page.content, userId).associateBy { it.id }
        return page.map { record -> summariesById.getValue(requireNotNull(record.id)) }.toPageResponse()
    }

    /** 게시된 기록만 노출한다 — DRAFT id 를 추측해 남의 비공개 기록을 보는 것을 막는다. */
    @Transactional(readOnly = true)
    fun detail(recordId: Long, userId: Long): SpotRecordResponse {
        val record = spotRecordRepository.findById(recordId).orElseThrow { SpotRecordNotFoundException() }
        if (record.status != SpotRecordStatus.PUBLISHED) throw SpotRecordNotFoundException()
        return responseAssembler.assemble(record, userId)
    }

    private fun allPage(blockedIds: Set<Long>, pageable: Pageable): Page<SpotRecord> {
        if (blockedIds.isEmpty()) return spotRecordRepository.findByStatus(SpotRecordStatus.PUBLISHED, pageable)
        return spotRecordRepository.findByStatusAndUserIdNotIn(SpotRecordStatus.PUBLISHED, blockedIds, pageable)
    }

    private fun followingPage(userId: Long, blockedIds: Set<Long>, pageable: Pageable): Page<SpotRecord> {
        val followingIds = followRepository.findFollowingIds(userId) - blockedIds
        if (followingIds.isEmpty()) return Page.empty(pageable)
        return spotRecordRepository.findByUserIdInAndStatus(followingIds, SpotRecordStatus.PUBLISHED, pageable)
    }

    /** 사용자 관심 [BloomCategory] 집합 → 매핑되는 Plant 들 → 그 Plant 가 태깅된 기록으로 확장한다. */
    private fun interestPage(userId: Long, blockedIds: Set<Long>, pageable: Pageable): Page<SpotRecord> {
        val categories = userFavoriteCategoryRepository.findByIdUserId(userId).map { it.category }.toSet()
        if (categories.isEmpty()) return Page.empty(pageable)
        val plantIds = plantRepository.findByBloomCategoryIn(categories).mapNotNull { it.id }
        if (plantIds.isEmpty()) return Page.empty(pageable)
        val recordIds = spotRecordPlantRepository.findByIdPlantIdIn(plantIds).map { it.spotRecordId }.distinct()
        if (recordIds.isEmpty()) return Page.empty(pageable)
        val allowedIds = if (blockedIds.isEmpty()) {
            recordIds
        } else {
            spotRecordRepository.findAllById(recordIds).filter { it.userId !in blockedIds }.mapNotNull { it.id }
        }
        if (allowedIds.isEmpty()) return Page.empty(pageable)
        return spotRecordRepository.findByIdInAndStatus(allowedIds, SpotRecordStatus.PUBLISHED, pageable)
    }
}
