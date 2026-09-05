package com.peakda.server.domain.user.repository

import com.peakda.server.domain.user.entity.UserFavoriteCategory
import com.peakda.server.domain.user.entity.UserFavoriteCategoryId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface UserFavoriteCategoryRepository : JpaRepository<UserFavoriteCategory, UserFavoriteCategoryId> {

    fun findByIdUserId(userId: Long): List<UserFavoriteCategory>

    @Modifying
    @Query("delete from UserFavoriteCategory c where c.id.userId = :userId")
    fun deleteByUserId(userId: Long)
}
