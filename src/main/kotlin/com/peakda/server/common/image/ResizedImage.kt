package com.peakda.server.common.image

data class ResizedImage(
    val variant: ImageVariant,
    val bytes: ByteArray,
) {
    val size: Int get() = bytes.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResizedImage) return false
        return variant == other.variant && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = 31 * variant.hashCode() + bytes.contentHashCode()
}
