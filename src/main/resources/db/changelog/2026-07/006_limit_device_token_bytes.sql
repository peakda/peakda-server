--liquibase formatted sql

--changeset peakda:20260718-006-limit-device-token-bytes
ALTER TABLE device_tokens
    ADD CONSTRAINT ck_device_tokens_token_bytes
        CHECK (octet_length(token) BETWEEN 1 AND 1024) NOT VALID;
--rollback ALTER TABLE device_tokens DROP CONSTRAINT ck_device_tokens_token_bytes;
