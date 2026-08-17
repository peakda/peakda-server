package com.peakda.server.domain.location.application

import com.peakda.server.domain.location.entity.LocationUsageLog
import com.peakda.server.domain.location.repository.LocationUsageLogRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * 위치정보 이용·제공사실 확인자료를 적재한다.
 *
 * 지도 조회 같은 고빈도 요청의 응답 시간에 INSERT 가 얹히지 않도록 비동기로 저장한다.
 * 비동기 실행은 예외를 호출부로 전달하지 않으므로 실패를 여기서 직접 남긴다.
 */
@Component
class LocationUsageRecorder(
    private val locationUsageLogRepository: LocationUsageLogRepository,
    private val locationUsageDebouncer: LocationUsageDebouncer,
) {

    @Async
    fun record(command: RecordLocationUsageCommand) {
        if (!locationUsageDebouncer.shouldRecord(command.userId, command.service)) return

        try {
            locationUsageLogRepository.save(
                LocationUsageLog(
                    userId = command.userId,
                    channel = command.channel,
                    service = command.service,
                    usedAt = command.usedAt,
                ),
            )
        } catch (e: Exception) {
            log.error(
                "위치정보 이용 기록 저장 실패. userId={}, service={}, usedAt={}",
                command.userId,
                command.service,
                command.usedAt,
                e,
            )
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(LocationUsageRecorder::class.java)
    }
}
