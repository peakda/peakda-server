--liquibase formatted sql

--changeset peakda:20261101-001-create-apple-tokens
CREATE TABLE apple_tokens (
    apple_user_id TEXT        PRIMARY KEY,
    refresh_token TEXT        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL
);

COMMENT ON TABLE apple_tokens IS '애플 로그인 사용자의 애플 리프레시 토큰. 회원 탈퇴 때 애플 토큰 폐기(revoke) API 를 부르는 데만 쓴다(애플 요구사항)';
COMMENT ON COLUMN apple_tokens.apple_user_id IS '애플 사용자 식별자(identity token 의 sub). 애플 회원의 users.provider_id 와 같다';
COMMENT ON COLUMN apple_tokens.refresh_token IS '로그인 때 authorization code 를 교환해 받은 애플 리프레시 토큰. 애플 API 에 원문을 보내야 해서 해시하지 않는다';
COMMENT ON COLUMN apple_tokens.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN apple_tokens.updated_at IS '마지막으로 토큰을 받은 시각';
--rollback DROP TABLE apple_tokens;
