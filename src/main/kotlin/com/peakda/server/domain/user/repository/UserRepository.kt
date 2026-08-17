package com.peakda.server.domain.user.repository

import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.entity.UserRole
import com.peakda.server.domain.user.entity.UserStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<User, Long> {
    fun findByProviderAndProviderId(provider: OAuth2LoginType, providerId: String): User?
    fun existsByNickname(nickname: String): Boolean
    fun findTop500ByStatusAndIdGreaterThanOrderByIdAsc(status: UserStatus, id: Long): List<User>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    fun findByIdForUpdate(@Param("userId") userId: Long): User?

    /** 닉네임 부분일치(대소문자 무시) 사용자 검색. 탈퇴(익명화) 계정은 [status] 로 걸러 제외한다. */
    fun findByStatusAndNicknameContainingIgnoreCase(status: UserStatus, nickname: String, pageable: Pageable): Page<User>

    /**
     * 백오피스 사용자 검색.
     *
     * PostgreSQL 은 파라미터가 `LOWER(...)` 나 `IS NULL` 에만 등장하면 타입을 정하지 못한다.
     * null 을 `LOWER()` 에 넣으면 `bytea` 로 바인딩되어 `function lower(bytea) does not exist` 가 나고,
     * `(:param IS NULL OR ...)` 는 `could not determine data type of parameter $N` (SQLState 42P18) 이 난다.
     *
     * 그래서 [nicknamePattern] 은 절대 null 이 아니어야 하며(검색어가 없으면 `%`),
     * 나머지 선택 조건은 NOT NULL 컬럼과 함께 `COALESCE` 에 넣어 컬럼에서 타입을 가져오게 한다.
     * `status`·`role` 은 모두 NOT NULL 이므로 값이 없으면 조건이 항상 참이 된다.
     */
    @Query(
        """
            SELECT u
            FROM User u
            WHERE LOWER(u.nickname) LIKE LOWER(:nicknamePattern) ESCAPE '\'
              AND u.status = COALESCE(:status, u.status)
              AND u.role = COALESCE(:role, u.role)
            ORDER BY u.id DESC
        """,
    )
    fun findAdminUsers(
        @Param("nicknamePattern") nicknamePattern: String,
        @Param("status") status: UserStatus?,
        @Param("role") role: UserRole?,
        pageable: Pageable,
    ): Page<User>

    /**
     * 이메일 부분일치(대소문자 무시) 사용자 id 조회. 백오피스 위치정보 확인자료를 대상자로 좁힐 때 쓴다.
     *
     * [findAdminUsers] 와 같은 이유로 [emailPattern] 은 절대 null 이 아니어야 한다.
     * 이메일이 없는 계정(탈퇴로 익명화된 계정 포함)은 LIKE 비교에서 자연히 빠진다.
     */
    @Query(
        """
            SELECT u.id
            FROM User u
            WHERE LOWER(u.email) LIKE LOWER(:emailPattern) ESCAPE '\'
            ORDER BY u.id DESC
        """,
    )
    fun findIdsByEmailPattern(
        @Param("emailPattern") emailPattern: String,
        pageable: Pageable,
    ): List<Long>
}
