-- Keep the article summary aligned with create_db.sql and the JPA mapping.
-- This statement is safe to run again when the column is already TEXT.
ALTER TABLE articles
  MODIFY COLUMN sapo TEXT NOT NULL;
