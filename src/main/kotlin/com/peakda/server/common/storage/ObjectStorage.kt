package com.peakda.server.common.storage

interface ObjectStorage {
    fun upload(key: String, bytes: ByteArray, contentType: String): String
    fun delete(key: String)
    fun publicUrlOf(key: String): String
}
