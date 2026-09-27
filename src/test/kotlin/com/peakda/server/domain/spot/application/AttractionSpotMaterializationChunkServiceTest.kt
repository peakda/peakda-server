package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.application.AttractionEligibilityProperties
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import com.peakda.server.domain.spot.repository.SpotVisibilityRow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails

class AttractionSpotMaterializationChunkServiceTest {
    private val attractionRepository = mock(AttractionRepository::class.java) { invocation ->
        if (invocation.method.name == "findVisibleIdsByIdInAndContentTypes") listOf(1L, 3L) else null
    }
    private val spotRepository = mock(SpotRepository::class.java) { invocation ->
        if (invocation.method.name == "updateVisibleByIdIn") (invocation.arguments[0] as Collection<*>).size else null
    }
    private val service = AttractionSpotMaterializationChunkService(
        mock(SpotService::class.java),
        spotRepository,
        attractionRepository,
        AttractionEligibilityProperties(setOf("12")),
    )

    @Test
    fun `대상 명소의 Spot 은 공개하고 대상이 아닌 명소의 Spot 은 숨긴다`() {
        val result = service.syncVisibility(
            listOf(
                row(id = 10L, attractionId = 1L, visible = true),
                row(id = 20L, attractionId = 2L, visible = true),
                row(id = 30L, attractionId = 3L, visible = false),
            ),
        )

        assertThat(result).isEqualTo(AttractionSpotVisibilitySyncResult(hidden = 1, shown = 1))
        val lookup = mockingDetails(attractionRepository).invocations.single()
        assertThat(lookup.arguments[0] as Collection<*>).containsExactly(1L, 2L, 3L)
        assertThat(lookup.arguments[1]).isEqualTo(setOf("12"))
        val updates = mockingDetails(spotRepository).invocations.associate { it.arguments[1] to it.arguments[0] }
        assertThat(updates[false] as Collection<*>).containsExactly(20L)
        assertThat(updates[true] as Collection<*>).containsExactly(30L)
    }

    @Test
    fun `이미 상태가 맞으면 갱신 쿼리를 실행하지 않는다`() {
        val result = service.syncVisibility(
            listOf(row(id = 10L, attractionId = 1L, visible = true), row(id = 20L, attractionId = 2L, visible = false)),
        )

        assertThat(result).isEqualTo(AttractionSpotVisibilitySyncResult(hidden = 0, shown = 0))
        assertThat(mockingDetails(spotRepository).invocations).isEmpty()
    }

    private fun row(id: Long, attractionId: Long?, visible: Boolean): SpotVisibilityRow = object : SpotVisibilityRow {
        override val id = id
        override val attractionId = attractionId
        override val visible = visible
    }
}
