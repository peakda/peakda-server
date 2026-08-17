package com.peakda.server.domain.location.application

import com.peakda.server.domain.location.entity.LocationServiceType
import com.peakda.server.domain.location.presentation.response.LocationUsageLogResponse
import com.peakda.server.domain.location.repository.LocationUsageLogRepository
import com.peakda.server.domain.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import org.springframework.data.domain.PageRequest as SpringPageRequest

@Service
class LocationUsageQueryService(
    private val locationUsageLogRepository: LocationUsageLogRepository,
    private val userRepository: UserRepository,
) {

    /**
     * 위치정보 이용·제공사실 확인자료를 최신순으로 조회한다.
     *
     * [email] 검색어가 있으면 먼저 대상 사용자를 찾아 id 로 좁힌다. 확인자료와 users 를 한 번에 조인하지 않고
     * 두 번 나눠 조회한다 — 확인자료는 사용자 도메인의 수명과 무관하게 남는 기록이다.
     */
    @Transactional(readOnly = true)
    fun list(
        email: String?,
        service: LocationServiceType?,
        from: Instant?,
        to: Instant?,
        pageable: Pageable,
    ): Page<LocationUsageLogResponse> {
        val searchKeyword = email?.trim()?.takeIf { it.isNotEmpty() }
        val usageLogs = if (searchKeyword == null) {
            locationUsageLogRepository.search(service, from, to, pageable)
        } else {
            val userIds = userRepository.findIdsByEmailPattern(
                emailPattern = "%${escapeLikePattern(searchKeyword)}%",
                pageable = SpringPageRequest.of(0, MAX_SEARCHED_USERS),
            )
            if (userIds.isEmpty()) {
                return PageImpl(emptyList(), pageable, 0)
            }
            locationUsageLogRepository.searchByUserIds(userIds, service, from, to, pageable)
        }

        val users = userRepository.findAllById(usageLogs.content.map { it.userId }.distinct())
            .associateBy { requireNotNull(it.id) }

        return usageLogs.map { usageLog ->
            val user = users[usageLog.userId]
            LocationUsageLogResponse.from(usageLog, user?.email, user?.nickname)
        }
    }

    /** LIKE 와일드카드를 검색어 그대로 비교하도록 이스케이프한다. 쿼리에서 `ESCAPE '\'` 로 받는다. */
    private fun escapeLikePattern(keyword: String): String =
        keyword.replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

    companion object {
        /** 이메일 검색으로 좁힐 대상자 상한. IN 절이 과도하게 길어지지 않게 한다. */
        private const val MAX_SEARCHED_USERS = 500
    }
}
