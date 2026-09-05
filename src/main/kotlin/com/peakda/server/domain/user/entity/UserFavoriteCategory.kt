package com.peakda.server.domain.user.entity

import com.peakda.server.domain.seasonal.entity.BloomCategory
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.EnumType
import jakarta.persistence.Entity
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.io.Serializable

/**
 * 사용자가 선택한 관심 꽃 카테고리. (user_id, category) 복합키 조인 엔티티.
 *
 * 분류 정본은 [BloomCategory] 이며, 지도 꽃 필터·피드 관심 식물 필터와 동일 축을 사용한다.
 */
@Entity
@Table(name = "user_favorite_categories")
class UserFavoriteCategory(
    @EmbeddedId
    val id: UserFavoriteCategoryId,
) {
    val userId: Long get() = id.userId
    val category: BloomCategory get() = id.category
}

@Embeddable
data class UserFavoriteCategoryId(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, columnDefinition = "TEXT")
    val category: BloomCategory,
) : Serializable
