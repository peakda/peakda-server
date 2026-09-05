package com.peakda.server.common.storage

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

@Configuration
@EnableConfigurationProperties(StorageProperties::class)
class StorageConfig {

    @Bean
    fun s3Client(properties: StorageProperties): S3Client =
        S3Client.builder()
            .endpointOverride(URI.create(properties.endpoint))
            .region(Region.of(properties.region))
            .credentialsProvider(credentialsProvider(properties))
            .serviceConfiguration(s3ServiceConfiguration(properties))
            .build()

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
