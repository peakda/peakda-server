package com.peakda.server.domain.location.application

import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.location.entity.LocationAccessChannel
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Instant

/**
 * [RecordLocationUsage] 가 붙은 메서드가 정상 응답하면 위치정보 이용·제공사실 확인자료를 남긴다.
 *
 * 사용자 식별과 취득경로는 컨트롤러 시그니처를 건드리지 않도록 보안 컨텍스트와 요청 헤더에서 직접 읽는다.
 */
@Aspect
@Component
class LocationUsageAspect(
    private val locationUsageRecorder: LocationUsageRecorder,
) {

    @AfterReturning("@annotation(recordLocationUsage)")
    fun record(joinPoint: JoinPoint, recordLocationUsage: RecordLocationUsage) {
        val userId = currentUserId() ?: return
        if (!hasCoordinates(joinPoint, recordLocationUsage.coordinateParams)) return

        locationUsageRecorder.record(
            RecordLocationUsageCommand(
                userId = userId,
                channel = LocationAccessChannel.from(currentUserAgent()),
                service = recordLocationUsage.service,
                usedAt = Instant.now(),
            ),
        )
    }

    private fun currentUserId(): Long? =
        (SecurityContextHolder.getContext().authentication?.principal as? PrincipalDetails)?.getUser()?.id

    private fun currentUserAgent(): String? =
        (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)
            ?.request
            ?.getHeader(HttpHeaders.USER_AGENT)

    /**
     * 좌표가 선택 파라미터인 메서드에서 이번 요청이 실제로 좌표를 실어 보냈는지 판별한다.
     *
     * 파라미터 이름을 읽지 못하면 좌표를 보낸 것으로 본다. 확인자료는 누락이 과기록보다 위험하다.
     */
    private fun hasCoordinates(joinPoint: JoinPoint, coordinateParams: Array<String>): Boolean {
        if (coordinateParams.isEmpty()) return true

        val parameterNames = (joinPoint.signature as? MethodSignature)?.parameterNames ?: return true
        return coordinateParams.all { name ->
            val index = parameterNames.indexOf(name)
            index < 0 || joinPoint.args.getOrNull(index) != null
        }
    }
}
