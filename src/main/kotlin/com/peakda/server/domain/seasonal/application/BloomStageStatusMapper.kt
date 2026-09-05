package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.spot.entity.BloomStage

/**
 * 결정 D — 기록축 [BloomStage](사용자 SpotRecord 4단계)를 예측축 [BloomStatus] 로 환산하는 단일 변환표.
 *
 * | 기록 BloomStage | 예측 BloomStatus | 지도 핀 |
 * | --- | --- | --- |
 * | EARLY (개화 전)   | PREPARING | Before |
 * | STARTING (피기 시작) | STARTED  | Start |
 * | PEAK (절정)       | PEAK     | Peak |
 * | LATE (지는 중)     | ENDED    | 미노출(내부 전이) |
 *
 * 동네형 Spot 핀 산출과 명소형 융합의 사용자 기록 신호 환산 양쪽에서 공유한다.
 */
object BloomStageStatusMapper {
    fun toStatus(stage: BloomStage): BloomStatus = when (stage) {
        BloomStage.EARLY -> BloomStatus.PREPARING
        BloomStage.STARTING -> BloomStatus.STARTED
        BloomStage.PEAK -> BloomStatus.PEAK
        BloomStage.LATE -> BloomStatus.ENDED
    }
}
