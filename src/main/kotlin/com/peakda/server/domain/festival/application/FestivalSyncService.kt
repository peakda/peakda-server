package com.peakda.server.domain.festival.application

import com.peakda.server.domain.festival.repository.FestivalRepository
import com.peakda.server.infrastructure.external.pubdata.festival.response.FestivalItem
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FestivalSyncService(
    private val repository: FestivalRepository,
) {
    /**
     * 페이지의 유효 항목을 upsert 하고 처리한 항목 수를 돌려준다.
     * 값이 그대로인 행은 쓰이지 않으므로 영향 행 수가 아니라 처리 건수를 센다 — 페이징 종료 조건이 이 값을 누적한다.
     */
    @Transactional
    fun upsertPage(items: List<FestivalItem>): Int {
        val validItems = items
            .filter { it.fstvlNm.isNotBlank() && it.opar.isNotBlank() && it.fstvlStartDate.isNotBlank() }
        validItems.forEach { repository.upsert(it.toUpsertCommand()) }
        return validItems.size
    }
}
