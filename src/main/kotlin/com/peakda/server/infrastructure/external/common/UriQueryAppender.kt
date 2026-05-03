package com.peakda.server.infrastructure.external.common

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object UriQueryAppender {
    fun appendRaw(uri: URI, name: String, rawValue: String): URI {
        if (containsQueryParam(uri, name)) {
            return uri
        }

        val separator = if (uri.rawQuery.isNullOrBlank()) "?" else "&"
        return URI.create(uri.toASCIIString() + separator + name + "=" + rawValue)
    }

    fun appendEncoded(uri: URI, name: String, value: String): URI {
        return appendRaw(uri, name, encode(value))
    }

    fun maskServiceKey(uri: URI): String {
        return uri.toASCIIString().replace(Regex("([?&]serviceKey=)[^&]*"), "$1****")
    }

    private fun containsQueryParam(uri: URI, name: String): Boolean {
        val rawQuery = uri.rawQuery ?: return false
        return rawQuery.split("&").any { it.substringBefore("=") == name }
    }

    private fun encode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
    }
}
