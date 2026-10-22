package com.peakda.server.infrastructure.mail

/**
 * 메일 발송 실패.
 *
 * 원인 예외는 붙이지 않는다. SMTP 응답·주소 파싱 오류 메시지에 수신 주소가 들어 있어, 로그에 원인이 찍히면
 * 파기해야 할 이메일이 로그 저장소에 남는다. 대신 원인 예외 클래스 이름만 [reason] 에 담는다.
 *
 * @property permanent 이 주소로는 다시 보내도 실패할 오류(주소 형식, 수신 거부)인지. SMTP 장애 판단에서 뺀다.
 */
class EmailSendException(
    val reason: String,
    val permanent: Boolean,
) : RuntimeException("메일 발송 실패: $reason")
