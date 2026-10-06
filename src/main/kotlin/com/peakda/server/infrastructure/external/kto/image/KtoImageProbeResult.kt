package com.peakda.server.infrastructure.external.kto.image

/** 관광공사 이미지 URL 을 열어 본 결과. */
enum class KtoImageProbeResult {
    /** 이미지가 있다. */
    AVAILABLE,

    /** 서버가 404/410 으로 파일이 없다고 답했다. */
    MISSING,

    /** 타임아웃·5xx 등으로 판단할 수 없다. 다음에 다시 확인한다. */
    UNKNOWN,

    /** 관광공사 이미지 서버 주소가 아니어서 요청하지 않았다. 다시 확인해도 결과가 같다. */
    UNSUPPORTED,
}
