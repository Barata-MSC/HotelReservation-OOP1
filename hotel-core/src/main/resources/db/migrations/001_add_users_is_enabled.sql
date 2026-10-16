SET @users_have_is_enabled = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND COLUMN_NAME = 'is_enabled'
);

SET @add_users_is_enabled = IF(
  @users_have_is_enabled = 0,
  'ALTER TABLE users ADD COLUMN is_enabled BOOLEAN NOT NULL DEFAULT TRUE',
  'SELECT 1'
);

PREPARE add_users_is_enabled_statement FROM @add_users_is_enabled;
EXECUTE add_users_is_enabled_statement;
DEALLOCATE PREPARE add_users_is_enabled_statement;
