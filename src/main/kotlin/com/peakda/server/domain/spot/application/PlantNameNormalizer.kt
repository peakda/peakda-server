package com.peakda.server.domain.spot.application

object PlantNameNormalizer {
    private val whitespaceRegex = Regex("\\s+")

    fun normalize(value: String): String = value.trim().replace(whitespaceRegex, " ")
}
