package com.peakda.server.common.image

import com.peakda.server.common.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class ImageResizerTest {

    private val resizer = ImageResizer()

    @Test
    fun `WebP variant 는 WebP 로 저장한다`() {
        val result = resizer.resize(png(1200, 675), listOf(webp(width = 860, height = 1720))).single()

        assertThat(result.bytes.copyOfRange(0, 4).decodeToString()).isEqualTo("RIFF")
        assertThat(result.bytes.copyOfRange(8, 12).decodeToString()).isEqualTo("WEBP")
        // VP8 = 손실 압축, VP8L = 무손실. 품질 지정이 무시되고 무손실로 저장되면 용량이 줄지 않는다.
        assertThat(result.bytes.copyOfRange(12, 16).decodeToString()).isEqualTo("VP8 ")
    }

    @Test
    fun `upscale 을 끄면 기준보다 큰 원본만 줄인다`() {
        val large = decode(resizer.resize(png(4096, 2731), listOf(webp(width = 860, height = 1720))).single())
        val small = decode(resizer.resize(png(640, 360), listOf(webp(width = 860, height = 1720))).single())

        assertThat(large.width).isEqualTo(860)
        assertThat(large.height).isEqualTo(573)
        assertThat(small.width).isEqualTo(640)
        assertThat(small.height).isEqualTo(360)
    }

    @Test
    fun `upscale 을 켜 두면 기존처럼 기준 크기까지 키운다`() {
        val variant = ImageVariant(name = "main", width = 1600, height = 900, mode = ResizeMode.FIT)

        val resized = decode(resizer.resize(png(640, 360), listOf(variant)).single())

        assertThat(resized.width).isEqualTo(1600)
        assertThat(resized.height).isEqualTo(900)
    }

    @Test
    fun `WebP 원본도 읽을 수 있다`() {
        val webpSource = resizer.resize(png(800, 450), listOf(webp(width = 800, height = 450))).single().bytes

        val result = decode(resizer.resize(webpSource, listOf(webp(width = 400, height = 400))).single())

        assertThat(result.width).isEqualTo(400)
    }

    @Test
    fun `투명 PNG 도 손실 압축 WebP 로 저장한다`() {
        val image = BufferedImage(1200, 675, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.color = Color(120, 180, 90, 128)
        graphics.fillRect(0, 0, 600, 675)
        graphics.dispose()
        val source = ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()

        val result = resizer.resize(source, listOf(webp(width = 860, height = 1720))).single()

        val chunks = result.bytes.decodeToString(throwOnInvalidSequence = false)
        assertThat(chunks).contains("VP8 ").doesNotContain("VP8L")
    }

    @Test
    fun `픽셀 수가 상한을 넘으면 디코드하지 않고 거부한다`() {
        // 8000×8000 = 64MP. 단색 1비트 PNG 라 파일은 작지만 ARGB 로 펼치면 256MB 가 된다.
        val image = BufferedImage(8000, 8000, BufferedImage.TYPE_BYTE_BINARY)
        val source = ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()

        assertThatThrownBy { resizer.resize(source, listOf(webp(width = 860, height = 1720))) }
            .isInstanceOf(ImageException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.IMAGE_SIZE_EXCEEDED)
    }

    @Test
    fun `CROP 에서는 upscale 을 끌 수 없다`() {
        assertThatThrownBy {
            ImageVariant(name = "thumbnail", width = 256, height = 256, mode = ResizeMode.CROP, upscale = false)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun webp(width: Int, height: Int) =
        ImageVariant(
            name = "main",
            width = width,
            height = height,
            mode = ResizeMode.FIT,
            format = ImageFormat.WEBP,
            quality = 0.8,
            upscale = false,
        )

    private fun png(width: Int, height: Int): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = Color(120, 180, 90)
        graphics.fillRect(0, 0, width, height / 2)
        graphics.dispose()
        val output = ByteArrayOutputStream()
        ImageIO.write(image, "png", output)
        return output.toByteArray()
    }

    private fun decode(result: ResizedImage): BufferedImage = ImageIO.read(ByteArrayInputStream(result.bytes))
}
