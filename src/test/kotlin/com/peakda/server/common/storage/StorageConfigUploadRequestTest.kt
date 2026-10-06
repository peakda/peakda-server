package com.peakda.server.common.storage

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import software.amazon.awssdk.core.interceptor.Context
import software.amazon.awssdk.core.interceptor.ExecutionAttributes
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.http.SdkHttpRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

/**
 * OCI Object Storage 의 S3 호환 API 는 aws-chunked 전송과 trailer 체크섬을 받지 않는다.
 * AWS SDK 2.30 부터 PutObject 에 기본으로 붙는 이 형식이 다시 켜지면 운영 업로드가 전부 실패한다.
 * 서명 방식이 엔드포인트 scheme 에 따라 달라지므로 운영과 같은 HTTPS 로 서명된 요청을 전송 직전에 확인한다.
 */
class StorageConfigUploadRequestTest {

    private class CapturedRequest : RuntimeException()

    @Test
    fun `put object is signed as a plain body without aws-chunked trailer checksums`() {
        val properties = StorageProperties(
            bucket = "peakda-prod-media",
            endpoint = "https://namespace.compat.objectstorage.ap-osaka-1.oraclecloud.com",
            region = "ap-osaka-1",
            accessKey = "access",
            secretKey = "secret",
            pathStyleAccess = true,
        )
        var captured: SdkHttpRequest? = null
        val client = StorageConfig().s3ClientBuilder(properties)
            .overrideConfiguration { config ->
                config.addExecutionInterceptor(object : ExecutionInterceptor {
                    override fun beforeTransmission(
                        context: Context.BeforeTransmission,
                        executionAttributes: ExecutionAttributes,
                    ) {
                        captured = context.httpRequest()
                        throw CapturedRequest()
                    }
                })
            }
            .build()

        runCatching {
            client.putObject(
                PutObjectRequest.builder()
                    .bucket(properties.bucket)
                    .key("spots/1/photo.webp")
                    .contentType("image/webp")
                    .build(),
                RequestBody.fromBytes("image-bytes".toByteArray()),
            )
        }

        assertThat(captured).isNotNull
        val headers = captured!!.headers().mapKeys { it.key.lowercase() }
        assertThat(headers["content-encoding"].orEmpty()).noneMatch { it.contains("aws-chunked") }
        assertThat(headers).doesNotContainKey("x-amz-trailer")
        assertThat(headers.keys).noneMatch { it.startsWith("x-amz-checksum-") }
        assertThat(headers["x-amz-content-sha256"].orEmpty()).noneMatch { it.startsWith("STREAMING-") }
    }
}
