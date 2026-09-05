--liquibase formatted sql

--changeset peakda:20260620-005-create-user-favorite-categories
CREATE TABLE user_favorite_categories (
    user_id  BIGINT NOT NULL,
    category TEXT   NOT NULL,
    CONSTRAINT pk_user_favorite_categories PRIMARY KEY (user_id, category)
);

CREATE INDEX ix_user_favorite_categories_user_id ON user_favorite_categories (user_id);
--rollback DROP TABLE user_favorite_categories;

--changeset peakda:20260620-005-comment-user-favorite-categories
COMMENT ON TABLE user_favorite_categories IS '사용자가 선택한 관심 꽃 카테고리 (BloomCategory)';
COMMENT ON COLUMN user_favorite_categories.user_id IS '사용자 id';
COMMENT ON COLUMN user_favorite_categories.category IS '관심 꽃 카테고리 (BloomCategory enum 값)';
--rollback COMMENT ON TABLE user_favorite_categories IS NULL;
