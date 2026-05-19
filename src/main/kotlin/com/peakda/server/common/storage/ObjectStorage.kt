package com.peakda.server.common.storage

/**
 * 오브젝트 스토리지 추상화. 모든 메서드는 우리 버킷에 저장되는 **key** 단위로 동작한다.
 *
 * 외부 공개용 URL 은 [presignedGetUrl] 로 매번 새로 발급한다.
 * 만료가 있는 URL 이므로 DB 에 저장하지 않고, API 응답 직전에만 변환해서 내려준다.
 */
interface ObjectStorage {
    fun upload(key: String, bytes: ByteArray, contentType: String): String
    fun copy(sourceKey: String, destinationKey: String): String
    fun delete(key: String)
    fun presignedGetUrl(key: String): String
}
