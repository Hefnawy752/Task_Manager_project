ALTER TABLE tasks ADD COLUMN created_by_id BIGINT NOT NULL DEFAULT 1;
-- adjust default to an existing user id, then later drop default
ALTER TABLE tasks ADD CONSTRAINT fk_task_created_by FOREIGN KEY (created_by_id) REFERENCES users(id);