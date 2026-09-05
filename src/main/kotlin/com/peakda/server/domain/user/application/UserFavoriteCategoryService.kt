package com.peakda.server.domain.user.application

import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.user.entity.UserFavoriteCategory
import com.peakda.server.domain.user.entity.UserFavoriteCategoryId
import com.peakda.server.domain.user.repository.UserFavoriteCategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserFavoriteCategoryService(
    private val userFavoriteCategoryRepository: UserFavoriteCategoryRepository,
) {

    /**
     * 사용자의 관심 꽃 카테고리를 [categories] 로 전부 교체한다 (replace semantics).
     */
    @Transactional
    fun replace(userId: Long, categories: Set<BloomCategory>): Set<BloomCategory> {
        userFavoriteCategoryRepository.deleteByUserId(userId)
        userFavoriteCategoryRepository.flush()
        userFavoriteCategoryRepository.saveAll(
            categories.map { UserFavoriteCategory(UserFavoriteCategoryId(userId, it)) },
        )
        return categories
    }

    @Transactional(readOnly = true)
    fun findCategories(userId: Long): Set<BloomCategory> =
        userFavoriteCategoryRepository.findByIdUserId(userId)
            .map { it.category }
            .toSet()

    /** 사용자의 모든 관심 꽃을 삭제한다. 계정 탈퇴 시 사용. */
    @Transactional
    fun deleteAllByUser(userId: Long) {
        userFavoriteCategoryRepository.deleteByUserId(userId)
    }
}
