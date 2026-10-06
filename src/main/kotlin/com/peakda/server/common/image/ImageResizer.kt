package com.peakda.server.common.image

import com.peakda.server.common.exception.ErrorCode
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Component
class ImageResizer {

    init {
        // WebP 같은 플러그인 writer 는 classpath 의 SPI 로 등록된다. 실행 jar 에서는 ImageIO 가 처음 쓰이는
        // 스레드의 classloader 에 따라 플러그인을 못 찾을 수 있어, 기동 시 한 번 다시 스캔한다.
        ImageIO.scanForPlugins()
    }

    fun resize(source: ByteArray, variants: List<ImageVariant>): List<ResizedImage> {
        require(variants.isNotEmpty()) { "variants must not be empty" }
        val original = decode(source)
        return variants.map { variant -> ResizedImage(variant, render(original, variant)) }
    }

    /**
     * 헤더로 가로·세로를 먼저 읽고, 픽셀 수가 상한을 넘으면 디코드하지 않는다.
     *
     * PNG·WebP 는 수 KB 파일로도 수 GB 래스터를 만들 수 있다. 디코드 중 OutOfMemoryError 가 나면
     * 서버가 통째로 내려가므로(ExitOnOutOfMemoryError) 파일 크기 검사만으로는 막을 수 없다.
     */
    private fun decode(source: ByteArray): BufferedImage {
        try {
            ImageIO.createImageInputStream(ByteArrayInputStream(source)).use { input ->
                val reader = input?.let { ImageIO.getImageReaders(it) }?.takeIf { it.hasNext() }?.next()
                    ?: throw ImageException(ErrorCode.INVALID_IMAGE_FORMAT)
                try {
                    reader.setInput(input, true, true)
                    if (reader.getWidth(0).toLong() * reader.getHeight(0) > MAX_PIXELS) {
                        throw ImageException(ErrorCode.IMAGE_SIZE_EXCEEDED)
                    }
                    return reader.read(0)
                } finally {
                    reader.dispose()
                }
            }
        } catch (e: ImageException) {
            throw e
        } catch (e: Exception) {
            throw ImageException(ErrorCode.INVALID_IMAGE_FORMAT)
        }
    }

    private fun render(original: BufferedImage, variant: ImageVariant): ByteArray {
        val output = ByteArrayOutputStream()
        try {
            val builder = Thumbnails.of(original)
            if (keepsOriginalSize(original, variant)) {
                builder.scale(1.0)
            } else {
                builder.size(variant.width, variant.height)
            }
            builder.outputFormat(variant.format.extension)
                .outputQuality(variant.quality)
            variant.format.compressionType?.let(builder::outputFormatType)
            if (variant.mode == ResizeMode.CROP) {
                builder.crop(Positions.CENTER)
            }
            builder.toOutputStream(output)
        } catch (e: Exception) {
            throw ImageException(ErrorCode.IMAGE_PROCESSING_FAILED)
        }
        return output.toByteArray()
    }

    private fun keepsOriginalSize(original: BufferedImage, variant: ImageVariant): Boolean =
        !variant.upscale &&
            original.width <= variant.width &&
            original.height <= variant.height

    companion object {
        /** 디코드를 허용하는 최대 픽셀 수. 휴대폰 50MP 사진(8160×6120)은 받고, 래스터는 240MB 안쪽으로 묶는다. */
        const val MAX_PIXELS: Long = 60_000_000
    }
}
