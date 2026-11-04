package com.peakda.server.domain.spot.application

import com.peakda.server.domain.spot.entity.BloomStage
import com.peakda.server.domain.spot.entity.ContentLinkPlatform
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import java.time.Instant
import java.time.LocalDate

data class SpotRecordView(
    val id: Long,
    val spot: SpotView,
    val user: UserView,
    val visitedDate: LocalDate?,
    val bloomStage: BloomStage?,
    val memo: String?,
    val contentLink: ContentLinkView?,
    val plants: List<PlantView>,
    val photos: List<PhotoView>,
    val status: SpotRecordStatus,
    val publishedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val legacyReactions: LegacyReactionOverview,
)

data class SpotView(
    val id: Long,
    val type: SpotType,
    val name: String,
    val address: String?,
    val attractionId: Long?,
)

data class UserView(val id: Long, val nickname: String, val profileImageUrl: String?)
data class PlantView(val id: Long, val name: String)
data class ContentLinkView(val url: String, val platform: ContentLinkPlatform)
data class PhotoView(val objectKey: String, val url: String, val sortOrder: Int, val variants: Map<String, String>)

data class LegacyReactionOverview(
    val counts: List<LegacyReactionCount>,
    val myReactions: Set<String>,
)

data class LegacyReactionCount(val reactionType: String, val count: Long)
