package com.peakda.server.domain.curation.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.time.LocalDate

/**
 * 주차 단위 에디토리얼 큐레이션(SCR-026).
 *
 * [weekLabel]·[weekStartDate]·[weekEndDate]는 "8월 1주차 · 8/1~8/7" 뱃지용이다.
 * 월 기준 주차 계산 규칙은 화면 문구에 종속된다.
 * 따라서 서버가 파생하지 않고 에디터 입력을 그대로 보존한다.
 */
@Entity
@Table(
    name = "curations",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_curations_week_start_date", columnNames = ["week_start_date"]),
    ],
    indexes = [
        Index(name = "ix_curations_status_week_start_date", columnList = "status,week_start_date"),
    ],
)
class Curation(
    @Column(name = "week_start_date", nullable = false)
    val weekStartDate: LocalDate,

    @Column(name = "week_end_date", nullable = false)
    var weekEndDate: LocalDate,

    @Column(name = "week_label", nullable = false, columnDefinition = "TEXT")
    var weekLabel: String,

    @Column(name = "hero_image_url", columnDefinition = "TEXT")
    var heroImageUrl: String? = null,

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    var title: String,

    @Column(name = "subtitle", columnDefinition = "TEXT")
    var subtitle: String? = null,

    @Column(name = "intro", columnDefinition = "TEXT")
    var intro: String? = null,

    @Column(name = "next_teaser_overline", columnDefinition = "TEXT")
    var nextTeaserOverline: String? = null,

    @Column(name = "next_teaser_body", columnDefinition = "TEXT")
    var nextTeaserBody: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    var status: CurationStatus,

    @Column(name = "published_at")
    var publishedAt: Instant? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
