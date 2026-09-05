package com.peakda.server.domain.notification.repository

import com.peakda.server.domain.notification.entity.Notice
import com.peakda.server.domain.notification.entity.NoticeStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface NoticeRepository : JpaRepository<Notice, Long> {

    fun findAllByOrderByIdDesc(pageable: Pageable): Page<Notice>

    fun findByStatusOrderByIdDesc(status: NoticeStatus, pageable: Pageable): Page<Notice>

    fun findFirstByStatusOrderByIdAsc(status: NoticeStatus): Notice?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM Notice n WHERE n.id = :id")
    fun findByIdForUpdate(@Param("id") id: Long): Notice?
}
