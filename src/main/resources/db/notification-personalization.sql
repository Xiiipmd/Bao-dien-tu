-- Optional manual migration for environments that keep TMDT_DDL_AUTO=validate.
-- MySQL does not support ADD COLUMN IF NOT EXISTS consistently across Azure versions.
SET @push_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'users'
    AND column_name = 'push_notifications_enabled'
);
SET @add_push_column_sql = IF(
  @push_column_exists = 0,
  'ALTER TABLE users ADD COLUMN push_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE',
  'SELECT 1'
);
PREPARE add_push_column_statement FROM @add_push_column_sql;
EXECUTE add_push_column_statement;
DEALLOCATE PREPARE add_push_column_statement;

SET @published_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'articles'
    AND column_name = 'published_at'
);
SET @add_published_column_sql = IF(
  @published_column_exists = 0,
  'ALTER TABLE articles ADD COLUMN published_at TIMESTAMP(6) NULL',
  'SELECT 1'
);
PREPARE add_published_column_statement FROM @add_published_column_sql;
EXECUTE add_published_column_statement;
DEALLOCATE PREPARE add_published_column_statement;

UPDATE articles
SET published_at = created_at
WHERE status = 'PUBLISHED'
  AND published_at IS NULL;

SET @published_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'articles'
    AND index_name = 'idx_articles_publish_recency'
);
SET @add_published_index_sql = IF(
  @published_index_exists = 0,
  'CREATE INDEX idx_articles_publish_recency ON articles(status, published_at, created_at)',
  'SELECT 1'
);
PREPARE add_published_index_statement FROM @add_published_index_sql;
EXECUTE add_published_index_statement;
DEALLOCATE PREPARE add_published_index_statement;

CREATE TABLE IF NOT EXISTS user_topic_preferences (
  user_id INT NOT NULL,
  category_id INT NOT NULL,
  PRIMARY KEY (user_id, category_id),
  CONSTRAINT fk_topic_preference_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_topic_preference_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS news_notifications (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id INT NOT NULL,
  article_id INT NOT NULL,
  type VARCHAR(30) NOT NULL,
  title VARCHAR(255) NOT NULL,
  message VARCHAR(500) NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uk_notification_user_article_type UNIQUE (user_id, article_id, type),
  CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_notification_article FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE
);
