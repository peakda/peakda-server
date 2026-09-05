package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.seasonal.repository.BloomObservationUpsertCommand
import com.peakda.server.infrastructure.external.kma.flower.response.FlowerDetail
import java.time.LocalDate
import java.time.format.DateTimeParseException

fun FlowerDetail.toUpsertCommand(): BloomObservationUpsertCommand? {
    val treeType = treeType?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
    val obsPlace = obsPlace?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
    val buddingOn = bfShotDate.toObservationDateOrNull()
    val floweringOn = cfShotDate.toObservationDateOrNull()
    val fullBloomOn = ffShotDate.toObservationDateOrNull()
    val obsYear = floweringOn?.year ?: buddingOn?.year ?: fullBloomOn?.year ?: return null

    return BloomObservationUpsertCommand(
        treeType = treeType,
        obsPlace = obsPlace,
        obsYear = obsYear,
        obsPlaceDetail = obsPlaceDetail?.trim()?.ifBlank { null },
        flowerStatus = flowerStatus?.trim()?.ifBlank { null },
        buddingOn = buddingOn,
        floweringOn = floweringOn,
        fullBloomOn = fullBloomOn,
        sourceModifiedAt = modDate?.trim()?.ifBlank { null },
    )
}

private fun String?.toObservationDateOrNull(): LocalDate? {
    val value = this?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
    return try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }
}
