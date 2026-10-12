--liquibase formatted sql

--changeset peakda:20261012-011-add-spot-record-content-link
ALTER TABLE spot_records
    ADD COLUMN content_link_url      TEXT,
    ADD COLUMN content_link_platform TEXT,
    ADD CONSTRAINT ck_spot_records_content_link_platform CHECK (
        content_link_platform IS NULL
            OR content_link_platform IN ('INSTAGRAM', 'NAVER_BLOG', 'YOUTUBE', 'UNSUPPORTED')
    ),
    ADD CONSTRAINT ck_spot_records_content_link_pair CHECK (
        (content_link_url IS NULL) = (content_link_platform IS NULL)
    );

COMMENT ON COLUMN spot_records.content_link_url IS '기록에 연결한 관련 콘텐츠 링크(블로그·인스타그램·유튜브 등). 기록 1건당 1개, 없으면 NULL';
COMMENT ON COLUMN spot_records.content_link_platform IS 'content_link_url hostname 으로 서버가 분류한 플랫폼. 목록에 없는 도메인은 UNSUPPORTED';
--rollback ALTER TABLE spot_records DROP CONSTRAINT ck_spot_records_content_link_pair, DROP CONSTRAINT ck_spot_records_content_link_platform, DROP COLUMN content_link_platform, DROP COLUMN content_link_url;
