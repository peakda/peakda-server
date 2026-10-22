package com.peakda.server.infrastructure.mail

interface EmailSender {
    /** 실제로 메일이 나가는 구현인지. 꺼져 있으면 대량 발송을 시작하지 않는다. */
    val available: Boolean

    /** @throws EmailSendException 보내지 못했을 때 */
    fun send(message: EmailMessage)
}
