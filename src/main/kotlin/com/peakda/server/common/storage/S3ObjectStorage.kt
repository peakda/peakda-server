package com.peakda.server.common.storage

import com.peakda.server.common.exception.ErrorCode
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception

@Component
class S3ObjectStorage(
    private val s3Client: S3Client,
    private val properties: StorageProperties,
) : ObjectStorage {

    override fun upload(key: String, bytes: ByteArray, contentType: String): String {
        try {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(properties.bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(bytes.size.toLong())
                    .build(),
                RequestBody.fromBytes(bytes),
            )
        } catch (e: S3Exception) {
            throw StorageException(ErrorCode.STORAGE_UPLOAD_FAILED)
        }
        return publicUrlOf(key)
    }

    override fun delete(key: String) {
        try {
            s3Client.deleteObject(
                DeleteObjectRequest.builder()
                    .bucket(properties.bucket)
                    .key(key)
                    .build(),
            )
        } catch (e: S3Exception) {
            throw StorageException(ErrorCode.STORAGE_DELETE_FAILED)
        }
    }

    override fun publicUrlOf(key: String): String {
        val base = properties.publicBaseUrl.trimEnd('/')
        val normalizedKey = key.trimStart('/')
        return "$base/$normalizedKey"
    }
}
