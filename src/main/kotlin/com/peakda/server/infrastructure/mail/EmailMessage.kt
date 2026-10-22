package com.peakda.server.infrastructure.mail

/** 한 수신자에게 보내는 메일. 본문은 HTML 을 못 여는 클라이언트를 위해 텍스트도 함께 담는다. */
data class EmailMessage(
    val to: String,
    val subject: String,
    val text: String,
    val html: String,
) {
    /** 수신 주소는 로그에 남기지 않는다. */
    override fun toString(): String = "EmailMessage(subject=$subject)"
}
