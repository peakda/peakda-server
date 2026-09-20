package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.seasonal.entity.BloomStatus
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 절정 구간까지 남은 날로 [BloomStatus] 를 가르는 단일 산식.
 *
 * 추정기는 "아직 안 폈다"를 [BloomStatus.PREPARING] 하나로만 낸다. 그래서 절정이 코앞인 명소와
 * 반년 남은 명소가 같은 뱃지를 달았다. 개화전과 이르다를 가르는 기준을 여기 한 곳에 둔다.
 *
 * 기본값 기준 사다리 (D = 절정 시작일):
 * ```
 * D-15 이상   개화전 BEFORE_SEASON
 * D-14 ~ D-8  이르다 PREPARING   (earlyWindowDays)
 * D-7  ~ D-1  시작   STARTED     (startedWindowDays)
 * 절정 구간    절정   PEAK
 * 절정 종료 후 늦었다 ENDED
 * ```
 *
 * 산출 시점의 상태 보정([narrow])과 방문예정일 기준 재계산([statusOn])이 같은 경계를 쓰도록 공유한다.
 */
@Component
class BloomStatusWindowResolver(
    private val properties: BloomStatusWindowProperties,
) {
    /**
     * 절정 구간만으로 [date] 시점 상태를 판정한다 (방문예정일 재계산).
     * 절정 시작일을 모르면 판정 근거가 없으므로 null 을 돌려 호출부가 산출 상태를 유지하게 한다.
     */
    fun statusOn(date: LocalDate, peakStartDate: LocalDate?, peakEndDate: LocalDate?): BloomStatus? {
        val start = peakStartDate ?: return null
        val end = peakEndDate ?: start
        return when {
            date.isAfter(end) -> BloomStatus.ENDED
            !date.isBefore(start) -> BloomStatus.PEAK
            !date.isBefore(start.minusDays(properties.startedWindowDays)) -> BloomStatus.STARTED
            !date.isBefore(start.minusDays(properties.earlyWindowDays)) -> BloomStatus.PREPARING
            else -> BloomStatus.BEFORE_SEASON
        }
    }

    /**
     * 추정기가 낸 [status] 가 [BloomStatus.PREPARING] 일 때만 개화전/이르다로 다시 가른다.
     *
     * 나머지 상태는 기온·관측·축제 신호가 직접 판정한 것이라 날짜로 덮어쓰지 않는다. 절정 시작일을
     * 모르면 "1~2주 내 개화"라고 말할 근거가 없으므로 개화전으로 둔다.
     */
    fun narrow(status: BloomStatus, baseDate: LocalDate, peakStartDate: LocalDate?): BloomStatus {
        if (status != BloomStatus.PREPARING) return status
        val start = peakStartDate ?: return BloomStatus.BEFORE_SEASON
        return if (baseDate.isBefore(start.minusDays(properties.earlyWindowDays))) {
            BloomStatus.BEFORE_SEASON
        } else {
            BloomStatus.PREPARING
        }
    }
}
