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

    fun resize(source: ByteArray, variants: List<ImageVariant>): List<ResizedImage> {
        require(variants.isNotEmpty()) { "variants must not be empty" }
        val original = decode(source)
        return variants.map { variant -> ResizedImage(variant, render(original, variant)) }
    }

    private fun decode(source: ByteArray): BufferedImage =
        try {
            ImageIO.read(ByteArrayInputStream(source))
                ?: throw ImageException(ErrorCode.INVALID_IMAGE_FORMAT)
        } catch (e: ImageException) {
            throw e
        } catch (e: Exception) {
            throw ImageException(ErrorCode.INVALID_IMAGE_FORMAT)
        }

    private fun render(original: BufferedImage, variant: ImageVariant): ByteArray {
        val output = ByteArrayOutputStream()
        try {
            val builder = Thumbnails.of(original)
                .size(variant.width, variant.height)
                .outputFormat(variant.format.extension)
                .outputQuality(variant.quality)
            if (variant.mode == ResizeMode.CROP) {
                builder.crop(Positions.CENTER)
            }
            builder.toOutputStream(output)
        } catch (e: Exception) {
            throw ImageException(ErrorCode.IMAGE_PROCESSING_FAILED)
        }
        return output.toByteArray()
    }
}
