package com.peakda.server.domain.feed.application

import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.common.test.IntegrationTestSupport
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.feed.presentation.FeedController
import com.peakda.server.domain.feed.presentation.v2.FeedV2Controller
import com.peakda.server.domain.spot.entity.ReactionType
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.repository.SpotRecordReactionRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

@Transactional
class FeedReactionCoexistenceIntegrationTest : IntegrationTestSupport() {
    @Autowired lateinit var v1: FeedController
    @Autowired lateinit var v2: FeedV2Controller
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var records: SpotRecordRepository
    @Autowired lateinit var reactions: SpotRecordReactionRepository

    @Test
    fun `v1 and v2 coexist and share legacy reactions while v2 keeps additional emoji`() {
        val user = users.saveAndFlush(User(OAuth2LoginType.KAKAO, "reaction-coexistence-171", "반응공존"))
        val userId = requireNotNull(user.id)
        val principal = PrincipalDetails(user)
        val record = records.saveAndFlush(SpotRecord(spotId = 171L, userId = userId, status = SpotRecordStatus.PUBLISHED))
        val id = requireNotNull(record.id)

        val fire = requireNotNull(v2.addReaction(principal, id, "u:1f525").body?.data)
        assertThat(fire.key).isEqualTo("u:1f525")
        assertThat(fire.count).isEqualTo(1L)

        val legacy = requireNotNull(v1.addReaction(principal, id, ReactionType.HEART).body?.data)
        assertThat(legacy.counts.map { it.reactionType }).containsExactly(ReactionType.HEART)
        assertThat(legacy.myReactions).containsExactly(ReactionType.HEART)

        val sameHeart = requireNotNull(v2.addReaction(principal, id, "u:2764-fe0f").body?.data)
        assertThat(sameHeart.count).isEqualTo(1L)
        assertThat(sameHeart.reactedByMe).isTrue()
        assertThat(reactions.countByRecordAndStorageKey(id, "HEART")).isEqualTo(1L)

        val all = requireNotNull(v2.reactions(principal, id, null, 50, false).body?.data)
        assertThat(all.items.map { it.key }).containsExactly("u:1f525", "u:2764-fe0f")
        assertThat(all.items).allMatch { it.reactedByMe }

        val removed = requireNotNull(v1.removeReaction(principal, id, ReactionType.HEART).body?.data)
        assertThat(removed.counts).isEmpty()
        assertThat(removed.myReactions).isEmpty()
        val remaining = requireNotNull(v2.reactions(principal, id, null, 50, true).body?.data)
        assertThat(remaining.items.map { it.key }).containsExactly("u:1f525")

        v1.addReaction(principal, id, ReactionType.SMILE)
        val removedSmile = requireNotNull(v2.removeReaction(principal, id, "u:1f600").body?.data)
        assertThat(removedSmile.count).isZero()
        assertThat(reactions.countByRecordAndStorageKey(id, "SMILE")).isZero()
        assertThat(reactions.countByRecordAndStorageKey(id, "u:1f525")).isEqualTo(1L)
    }
}
