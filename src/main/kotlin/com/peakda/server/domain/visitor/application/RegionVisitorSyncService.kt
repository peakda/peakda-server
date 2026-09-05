package com.peakda.server.domain.visitor.application

import com.peakda.server.domain.visitor.repository.RegionVisitorRepository
import com.peakda.server.infrastructure.external.kto.datalab.response.MetcoVisitrItem
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegionVisitorSyncService(
    private val repository: RegionVisitorRepository,
) {
    @Transactional
    fun upsertPage(items: List<MetcoVisitrItem>): Int {
        return items
            .filter { it.baseYmd.isNotBlank() && it.areaCd.isNotBlank() && it.touDivCd.isNotBlank() }
            .sumOf { repository.upsert(it.toUpsertCommand()) }
    }
}
