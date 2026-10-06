package com.peakda.server.common.storage

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3ClientBuilder
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

@Configuration
@EnableConfigurationProperties(StorageProperties::class)
class StorageConfig {

    @Bean
    fun s3Client(properties: StorageProperties): S3Client = s3ClientBuilder(properties).build()

    internal fun s3ClientBuilder(properties: StorageProperties): S3ClientBuilder =
        S3Client.builder()
            .endpointOverride(URI.create(properties.endpoint))
            .region(Region.of(properties.region))
            .credentialsProvider(credentialsProvider(properties))
            .serviceConfiguration(s3ServiceConfiguration(properties))
            // SDK 2.30 부터 기본값(WHEN_SUPPORTED)은 업로드를 aws-chunked + trailer 체크섬으로 보낸다.
            // OCI Object Storage 의 S3 호환 API 는 이 형식을 받지 않아 PutObject 가 전부 실패하므로,
            // 체크섬은 API 가 요구할 때만 붙인다. backup.sh 의 AWS CLI 설정과 같은 값이다.
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)

    @Bean
    fun s3Presigner(properties: StorageProperties): S3Presigner =
        S3Presigner.builder()
            .endpointOverride(URI.create(properties.endpoint))
            .region(Region.of(properties.region))
            .credentialsProvider(credentialsProvider(properties))
            .serviceConfiguration(s3ServiceConfiguration(properties))
            .build()

    private fun credentialsProvider(properties: StorageProperties): AwsCredentialsProvider =
        if (properties.accessKey.isNotBlank() && properties.secretKey.isNotBlank()) {
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKey, properties.secretKey),
            )
        } else {
            // ECS task role, IRSA, EC2 instance profile, or the local AWS profile.
            DefaultCredentialsProvider.create()
        }

    private fun s3ServiceConfiguration(properties: StorageProperties) =
        S3Configuration.builder()
            .pathStyleAccessEnabled(properties.pathStyleAccess)
            .build()
}
