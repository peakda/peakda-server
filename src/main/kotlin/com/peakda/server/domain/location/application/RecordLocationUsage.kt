package com.peakda.server.domain.location.application

import com.peakda.server.domain.location.entity.LocationServiceType

/**
 * 개인위치정보를 이용하는 컨트롤러 메서드에 붙여 위치정보 이용·제공사실 확인자료를 남긴다.
 *
 * 기록은 [LocationUsageAspect] 가 응답이 정상 반환된 뒤에 수행한다. 요청이 실패하면 위치정보를
 * 이용한 것이 아니므로 남기지 않는다.
 *
 * @property service 확인자료의 "제공서비스" 로 표시할 종류
 * @property coordinateParams 좌표가 선택 파라미터인 메서드에서 좌표 전달 여부를 판별할 파라미터 이름.
 *   비워 두면 호출 자체를 위치정보 이용으로 본다. 지정하면 그 이름의 인자가 모두 non-null 일 때만 기록한다.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RecordLocationUsage(
    val service: LocationServiceType,
    val coordinateParams: Array<String> = [],
)
