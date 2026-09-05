package com.peakda.server.domain.location.entity

/**
 * 개인위치정보를 이용한 제공서비스 종류. 확인자료의 "제공서비스" 컬럼에 대응한다.
 *
 * 좌표를 입력받는 엔드포인트를 추가하면 여기에 값을 추가하고 해당 메서드에
 * `@RecordLocationUsage` 를 붙인다.
 */
enum class LocationServiceType {
    /** 지도 영역(bbox) 개화 현황 조회 */
    BLOOM_MAP,

    /** 큐레이션 상세의 연결 스팟까지 거리 계산 */
    CURATION_DETAIL,

    /** 좌표 기반 스팟 매칭 */
    SPOT_MATCH,

    /** 지도 핀 클릭 프리뷰 */
    SPOT_PREVIEW,
}
