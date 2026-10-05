package com.peakda.server.common.persistence

import java.time.Instant

/** [BaseTimeEntity] 를 상속한 엔티티의 id 와 마지막 수정 시각만 읽는 projection. */
interface LastModifiedRow {
    val id: Long
    val updatedAt: Instant
}
