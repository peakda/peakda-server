package com.peakda.server.domain.spot.application

/**
 * 찜 목록 배너가 어느 응답 경로에서도 같은 문구와 조사를 사용하도록 만드는 메시지 정본.
 */
object SpotFavoriteBannerMessage {

    fun currentPeak(spotName: String): String = "지금 $spotName${subjectMarker(spotName)} 절정이에요"

    fun imminent(spotName: String): String = "$spotName${subjectMarker(spotName)} 곧 만개해요"

    private fun subjectMarker(spotName: String): String {
        val last = spotName.lastOrNull() ?: return "가"
        if (last !in '\uAC00'..'\uD7A3') return "가"
        return if ((last.code - HANGUL_BASE) % JONGSEONG_COUNT == 0) "가" else "이"
    }

    private const val HANGUL_BASE = 0xAC00
    private const val JONGSEONG_COUNT = 28
}
