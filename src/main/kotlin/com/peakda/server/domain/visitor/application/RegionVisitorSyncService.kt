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
        var saved = 0
        for (item in items) {
            if (item.baseYmd.isBlank() || item.areaCd.isBlank() || item.touDivCd.isBlank()) continue
            val existing = repository.findByBaseYmdAndAreaCdAndTouDivCd(item.baseYmd, item.areaCd, item.touDivCd)
            if (existing == null) repository.save(item.toRegionVisitor()) else existing.applyUpdate(item)
            saved++
        }
        return saved
    }
}
