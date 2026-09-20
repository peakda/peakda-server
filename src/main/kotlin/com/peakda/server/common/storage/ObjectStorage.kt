package com.peakda.server.common.storage

/**
 * 오브젝트 스토리지 추상화. 모든 메서드는 우리 버킷에 저장되는 **key** 단위로 동작한다.
 *
 * 조회용 URL 은 [ObjectKeyUrlResolver] 가 만든다. 버킷 앞에 CDN 이 붙어 있으면 만료 없는 공개 URL 을,
 * 아니면 [presignedGetUrl] 로 매번 새로 발급한 URL 을 쓴다. 어느 쪽이든 URL 은 DB 에 저장하지 않고
 * API 응답 직전에만 key 로부터 만든다.
 */
interface ObjectStorage {
    fun upload(key: String, bytes: ByteArray, contentType: String): String
    fun copy(sourceKey: String, destinationKey: String): String
    fun download(key: String): ByteArray
    fun delete(key: String)
    fun presignedGetUrl(key: String): String
}
