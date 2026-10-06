package com.peakda.server.domain.curation.application

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.image.ImageException
import com.peakda.server.common.image.ImageResizer
import com.peakda.server.common.storage.ObjectKeyUrlResolver
import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.common.storage.StorageProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class CurationImageUploaderTest {

    private val objectStorage = FakeObjectStorage()
    private val uploader = CurationImageUploader(
        objectStorage,
        ImageResizer(),
        ObjectKeyUrlResolver(objectStorage, StorageProperties(bucket = "media", publicBaseUrl = "https://cdn.peakda.com")),
    )

    @Test
    fun `사진은 가로 860px WebP 한 벌만 저장한다`() {
        val uploaded = uploader.upload(png(4096, 2731), CurationImageUsage.PHOTO)

        assertThat(uploaded.objectKey).matches("curations/\\d{4}-\\d{2}/[0-9a-f-]{36}/main\\.webp")
        assertThat(uploaded.previewUrl).isEqualTo("https://cdn.peakda.com/${uploaded.objectKey}")
        assertThat(objectStorage.uploads.keys).containsExactly(uploaded.objectKey)
        assertThat(objectStorage.contentTypes[uploaded.objectKey]).isEqualTo("image/webp")
        assertThat(storedImage(uploaded.objectKey).width).isEqualTo(860)
    }

    @Test
    fun `히어로는 원본 해상도를 유지한다`() {
        val uploaded = uploader.upload(png(1280, 720), CurationImageUsage.HERO)

        val stored = storedImage(uploaded.objectKey)
        assertThat(stored.width).isEqualTo(1280)
        assertThat(stored.height).isEqualTo(720)
    }

    @Test
    fun `히어로도 상한보다 크면 1600x900 안으로 줄인다`() {
        val uploaded = uploader.upload(png(4096, 2304), CurationImageUsage.HERO)

        val stored = storedImage(uploaded.objectKey)
        assertThat(stored.width).isEqualTo(1600)
        assertThat(stored.height).isEqualTo(900)
    }

    @Test
    fun `30MB 를 넘는 파일은 거부한다`() {
        val oversized = MockMultipartFile("file", "big.png", "image/png", ByteArray((30 * 1024 * 1024) + 1))

        assertThatThrownBy { uploader.upload(oversized, CurationImageUsage.PHOTO) }
            .isInstanceOf(ImageException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.IMAGE_SIZE_EXCEEDED)
    }

    private fun storedImage(key: String): BufferedImage =
        ImageIO.read(ByteArrayInputStream(objectStorage.uploads.getValue(key)))

    private fun png(width: Int, height: Int): MockMultipartFile {
        val output = ByteArrayOutputStream()
        ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", output)
        return MockMultipartFile("file", "image.png", "image/png", output.toByteArray())
    }

    private class FakeObjectStorage : ObjectStorage {
        val uploads = linkedMapOf<String, ByteArray>()
        val contentTypes = mutableMapOf<String, String>()

        override fun upload(key: String, bytes: ByteArray, contentType: String): String {
            uploads[key] = bytes
            contentTypes[key] = contentType
            return key
        }

        override fun copy(sourceKey: String, destinationKey: String): String = destinationKey

        override fun download(key: String): ByteArray = uploads.getValue(key)

        override fun delete(key: String) {
            uploads -= key
        }

        override fun presignedGetUrl(key: String): String = "https://s3/$key"
    }
}
