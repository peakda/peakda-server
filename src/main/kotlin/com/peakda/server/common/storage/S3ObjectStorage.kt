package com.peakda.server.common.storage

import com.peakda.server.common.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CopyObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.time.Duration

@Component
class S3ObjectStorage(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val properties: StorageProperties,
) : ObjectStorage {

    private val log = LoggerFactory.getLogger(javaClass)

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
            logFailure("upload", key, e)
            throw StorageException(ErrorCode.STORAGE_UPLOAD_FAILED)
        }
        return key
    }

    override fun copy(sourceKey: String, destinationKey: String): String {
        try {
            s3Client.copyObject(
                CopyObjectRequest.builder()
                    .sourceBucket(properties.bucket)
                    .sourceKey(sourceKey)
                    .destinationBucket(properties.bucket)
                    .destinationKey(destinationKey)
                    .build(),
            )
        } catch (e: S3Exception) {
            logFailure("copy", sourceKey, e)
            throw StorageException(ErrorCode.STORAGE_UPLOAD_FAILED)
        }
        return destinationKey
    }

    override fun download(key: String): ByteArray {
        try {
            return s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                    .bucket(properties.bucket)
                    .key(key)
                    .build(),
            ).asByteArray()
        } catch (e: S3Exception) {
            logFailure("download", key, e)
            throw StorageException(ErrorCode.STORAGE_DOWNLOAD_FAILED)
        }
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
            logFailure("delete", key, e)
            throw StorageException(ErrorCode.STORAGE_DELETE_FAILED)
        }
    }

    override fun presignedGetUrl(key: String): String {
        val getRequest = GetObjectRequest.builder()
            .bucket(properties.bucket)
            .key(key)
            .build()
        val presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(properties.presignedUrlTtlSeconds))
            .getObjectRequest(getRequest)
            .build()
        return s3Presigner.presignGetObject(presignRequest).url().toString()
    }

    // 응답에는 ErrorCode 만 남으므로 원인(상태 코드·S3 에러 코드·요청 ID)은 여기서 남긴다.
    private fun logFailure(operation: String, key: String, e: S3Exception) {
        log.error(
            "스토리지 {} 실패 - bucket={}, key={}, status={}, errorCode={}, requestId={}, message={}",
            operation,
            properties.bucket,
            key,
            e.statusCode(),
            e.awsErrorDetails()?.errorCode(),
            e.requestId(),
            e.awsErrorDetails()?.errorMessage() ?: e.message,
            e,
        )
    }
}
