package com.peakda.server.common.storage

import com.peakda.server.common.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.presigner.S3Presigner

class S3ObjectStorageTest {
    private val client = mock(S3Client::class.java)
    private val storage = S3ObjectStorage(
        client, mock(S3Presigner::class.java), StorageProperties(bucket = "test-media"),
    )
    private val key = "spot-records/7/2026/11/photo/main.jpg"

    @Test
    fun `존재 확인은 본문 다운로드 없이 정확한 bucket과 key로 HEAD한다`() {
        `when`(client.headObject(any(HeadObjectRequest::class.java))).thenReturn(HeadObjectResponse.builder().build())

        assertThat(storage.exists(key)).isTrue()

        val request = ArgumentCaptor.forClass(HeadObjectRequest::class.java)
        verify(client).headObject(request.capture())
        assertThat(request.value.bucket()).isEqualTo("test-media")
        assertThat(request.value.key()).isEqualTo(key)
        verifyNoMoreInteractions(client)
    }

    @Test
    fun `HEAD의 확인된 객체 404만 없음으로 반환한다`() {
        doThrow(S3Exception.builder().statusCode(404).build()).`when`(client)
            .headObject(any(HeadObjectRequest::class.java))

        assertThat(storage.exists(key)).isFalse()
    }

    @Test
    fun `권한 제한과 일시 서비스 오류는 없음이 아닌 기존 조회 오류다`() {
        listOf(403, 429, 500, 503).forEach { status ->
            doThrow(S3Exception.builder().statusCode(status).build()).`when`(client)
                .headObject(any(HeadObjectRequest::class.java))

            assertReadFailure()
        }
    }

    @Test
    fun `오류 이름만 NoSuchKey여도 HTTP 404가 아니면 없음으로 취급하지 않는다`() {
        doThrow(
            S3Exception.builder().statusCode(403)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchKey").build()).build(),
        ).`when`(client).headObject(any(HeadObjectRequest::class.java))

        assertReadFailure()
    }

    @Test
    fun `버킷 자체가 없다는 404는 개별 variant 없음으로 오인하지 않는다`() {
        doThrow(
            S3Exception.builder().statusCode(404)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchBucket").build()).build(),
        ).`when`(client).headObject(any(HeadObjectRequest::class.java))

        assertReadFailure()
    }

    @Test
    fun `통신 실패는 없음으로 취급하지 않는다`() {
        doThrow(SdkClientException.create("connection timed out")).`when`(client)
            .headObject(any(HeadObjectRequest::class.java))

        assertReadFailure()
    }

    private fun assertReadFailure() {
        assertThatThrownBy { storage.exists(key) }.isInstanceOfSatisfying(StorageException::class.java) {
            assertThat(it.errorCode).isEqualTo(ErrorCode.STORAGE_DOWNLOAD_FAILED)
        }
    }
}
