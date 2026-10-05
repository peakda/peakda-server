package com.peakda.server.common.persistence

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseTimeEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: Instant
        protected set

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: Instant
        protected set

    /**
     * 자기 컬럼은 그대로이고 자식 행만 바뀐 수정도 수정 시각에 남긴다.
     * 값을 바꿔 dirty 로 만들면 flush 시 auditing 이 현재 시각으로 다시 채운다.
     */
    fun markModified() {
        updatedAt = Instant.now()
    }
}
