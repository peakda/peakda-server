package com.peakda.server.domain.spot.repository

import com.peakda.server.domain.spot.entity.SpotRecordPhotoUpload
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SpotRecordPhotoUploadRepository : JpaRepository<SpotRecordPhotoUpload, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from SpotRecordPhotoUpload u where u.objectKey = :key")
    fun findByKeyForUpdate(key: String): SpotRecordPhotoUpload?

    @Query(
        value = """
            SELECT * FROM spot_record_photo_uploads WHERE object_key IN (:keys)
            ORDER BY object_key COLLATE "C" FOR UPDATE
        """,
        nativeQuery = true,
    )
    fun findByKeysForUpdate(@Param("keys") keys: Collection<String>): List<SpotRecordPhotoUpload>

    @Modifying
    @Query(
        value = """
            INSERT INTO spot_record_photo_uploads (object_key, owner_id, variant_names, evidence, available)
            VALUES (:key, :ownerId, :variantNames, 'LEGACY_SERVER_KEY_AND_STORAGE', true)
            ON CONFLICT (object_key) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun registerLegacyIfAbsent(key: String, ownerId: Long, variantNames: String): Int
}
