package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * "현재 상태" 조회가 기준으로 삼을 산출일을 정한다.
 *
 * 저장된 가장 최근 산출일을 그대로 쓰면, 산출 잡이 멈춘 뒤에도 마지막으로 성공한 날의 상태가 계속
 * 현재 상태로 노출된다. 봄에 멈추면 가을까지 벚꽃 명소가 만개로 남는다. 그래서 산출일이
 * [BloomBaseDateProperties.maxAgeDays] 를 넘기면 null 을 돌려 "산출 없음"으로 취급한다.
 *
 * 호출부는 null 을 이미 "개화 정보 없음"으로 처리하고 있어, 철 지난 상태를 보여주는 대신 핀·뱃지·배너가
 * 빠진다. 틀린 상태를 내보내는 것보다 낫고, 잡이 멈춘 사실도 드러난다.
 */
@Component
class BloomBaseDateResolver(
    private val seasonalBloomEstimateRepository: SeasonalBloomEstimateRepository,
    private val properties: BloomBaseDateProperties,
    private val clock: Clock = Clock.system(KST),
) {

    /** 현재 상태로 인정할 수 있는 산출일. 산출이 없거나 유효 기간을 넘겼으면 null. */
    fun currentBaseDate(): LocalDate? {
        val latest = seasonalBloomEstimateRepository.findLatestBaseDate() ?: return null
        val ageDays = ChronoUnit.DAYS.between(latest, LocalDate.now(clock)).coerceAtLeast(0)
        return latest.takeIf { ageDays <= properties.maxAgeDays }
    }

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
