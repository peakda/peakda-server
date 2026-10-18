--liquibase formatted sql

--changeset peakda:20261018-012-create-auth-sessions
CREATE TABLE auth_sessions (
    id                          UUID        PRIMARY KEY,
    user_id                     BIGINT      NOT NULL,
    device_id                   TEXT        NOT NULL,
    device_name                 TEXT,
    client_type                 TEXT        NOT NULL,
    platform                    TEXT,
    refresh_token_hash          TEXT        NOT NULL,
    previous_refresh_token_hash TEXT,
    rotated_at                  TIMESTAMPTZ,
    last_used_at                TIMESTAMPTZ NOT NULL,
    expires_at                  TIMESTAMPTZ NOT NULL,
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_auth_sessions_user_device UNIQUE (user_id, device_id),
    CONSTRAINT ck_auth_sessions_client_type CHECK (client_type IN ('APP', 'WEB')),
    CONSTRAINT ck_auth_sessions_platform CHECK (platform IS NULL OR platform IN ('ANDROID', 'IOS'))
);

CREATE INDEX ix_auth_sessions_user_id_last_used_at ON auth_sessions (user_id, last_used_at);

COMMENT ON TABLE auth_sessions IS '로그인 세션(기기). 로그인 1회가 한 행이며 사용자당 최대 개수를 넘으면 가장 오래 안 쓴 세션부터 지운다';
COMMENT ON COLUMN auth_sessions.id IS '세션 id. 액세스·리프레시 토큰의 sid 클레임';
COMMENT ON COLUMN auth_sessions.user_id IS '세션 소유자 사용자 id';
COMMENT ON COLUMN auth_sessions.device_id IS '클라이언트가 만든 설치(브라우저) 식별자. 같은 기기 재로그인은 기존 행을 교체한다. 보내지 않으면 서버가 만든다';
COMMENT ON COLUMN auth_sessions.device_name IS '기기 표시 이름 (앱: 기기 모델, 웹: 브라우저 (OS))';
COMMENT ON COLUMN auth_sessions.client_type IS '로그인 경로 (APP, WEB). 리프레시 토큰 유효기간이 달라진다';
COMMENT ON COLUMN auth_sessions.platform IS '앱 플랫폼 (ANDROID, IOS). 웹이거나 앱이 보내지 않았으면 NULL';
COMMENT ON COLUMN auth_sessions.refresh_token_hash IS '현재 리프레시 토큰의 SHA-256';
COMMENT ON COLUMN auth_sessions.previous_refresh_token_hash IS '직전 리프레시 토큰의 SHA-256. 회전 직후 잠깐 같은 결과를 돌려주는 데 쓴다';
COMMENT ON COLUMN auth_sessions.rotated_at IS '마지막 회전 시각';
COMMENT ON COLUMN auth_sessions.last_used_at IS '마지막 로그인·재발급 시각. 기기 수 초과 시 가장 오래된 세션부터 해제한다';
COMMENT ON COLUMN auth_sessions.expires_at IS '리프레시 토큰 만료 시각';
COMMENT ON COLUMN auth_sessions.created_at IS '세션 생성 시각 (기기 등록일)';
COMMENT ON COLUMN auth_sessions.updated_at IS '레코드 최종 수정 시각';
--rollback DROP TABLE auth_sessions;
