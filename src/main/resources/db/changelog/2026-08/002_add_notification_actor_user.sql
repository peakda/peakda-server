--liquibase formatted sql

--changeset peakda:20260815-002-add-notification-actor-user
ALTER TABLE notifications ADD COLUMN actor_user_id BIGINT;
CREATE INDEX ix_notifications_actor_user ON notifications (actor_user_id);

COMMENT ON COLUMN notifications.actor_user_id IS 'FOLLOW/REACTION 알림을 발생시킨 행위자 사용자 id. 기존 행은 NULL';
--rollback DROP INDEX ix_notifications_actor_user;
--rollback ALTER TABLE notifications DROP COLUMN actor_user_id;
