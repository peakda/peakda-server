package com.peakda.server.domain.festival.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/** 축제 상세의 주요 볼거리 1건. */
@Entity
@Table(
    name = "festival_highlights",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_festival_highlights_editorial_sort",
            columnNames = ["festival_editorial_id", "sort_order"],
        ),
    ],
    indexes = [
        Index(name = "ix_festival_highlights_editorial_id", columnList = "festival_editorial_id"),
    ],
)
class FestivalHighlight(
    @Column(name = "festival_editorial_id", nullable = false)
    val festivalEditorialId: Long,

    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int,

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    val title: String,

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    val body: String,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
