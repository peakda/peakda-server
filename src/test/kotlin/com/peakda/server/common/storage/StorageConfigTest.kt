package com.peakda.server.common.storage

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider

class StorageConfigTest {

    private val credentialsProvider = StorageConfig::class.java
        .getDeclaredMethod("credentialsProvider", StorageProperties::class.java)
        .apply { isAccessible = true }

    @Test
    fun `credentials are resolved from the AWS default chain when explicit keys are absent`() {
        val properties = StorageProperties(bucket = "bucket")

        val provider = credentialsProvider.invoke(StorageConfig(), properties)

        assertThat(provider).isInstanceOf(DefaultCredentialsProvider::class.java)
    }

    @Test
    fun `explicit credentials remain supported for local and dev storage`() {
        val properties = StorageProperties(bucket = "bucket", accessKey = "access", secretKey = "secret")

        val provider = credentialsProvider.invoke(StorageConfig(), properties)

        assertThat(provider).isInstanceOf(StaticCredentialsProvider::class.java)
    }
}
