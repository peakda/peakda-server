package com.peakda.server.domain.auth.session.application

import java.security.MessageDigest
import java.util.HexFormat

/** 리프레시 토큰은 원문 대신 SHA-256 으로만 DB 에 남긴다. */
object RefreshTokenHash {

    fun of(refreshToken: String): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(refreshToken.toByteArray()))
}
