CREATE TABLE IF NOT EXISTS `user_security_activities` (
    `id` BIGINT AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `activity_type` VARCHAR(50) NOT NULL,
    `description` VARCHAR(255) NOT NULL,
    `changed_fields` TEXT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_security_activities_user_id_created_at` (`user_id`, `created_at`),
    CONSTRAINT `fk_user_security_activities_user_id`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
            ON UPDATE NO ACTION ON DELETE CASCADE
) COMMENT='Recent account and security activity records';

SET @current_schema = DATABASE();

SET @has_event_type = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @current_schema
      AND TABLE_NAME = 'user_security_activities'
      AND COLUMN_NAME = 'event_type'
);

SET @has_activity_type = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @current_schema
      AND TABLE_NAME = 'user_security_activities'
      AND COLUMN_NAME = 'activity_type'
);

SET @rename_event_type_sql = IF(
    @has_event_type > 0 AND @has_activity_type = 0,
    'ALTER TABLE `user_security_activities` CHANGE COLUMN `event_type` `activity_type` VARCHAR(50) NOT NULL',
    'SELECT 1'
);
PREPARE user_security_stmt FROM @rename_event_type_sql;
EXECUTE user_security_stmt;
DEALLOCATE PREPARE user_security_stmt;

SET @has_summary = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @current_schema
      AND TABLE_NAME = 'user_security_activities'
      AND COLUMN_NAME = 'summary'
);

SET @has_description = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @current_schema
      AND TABLE_NAME = 'user_security_activities'
      AND COLUMN_NAME = 'description'
);

SET @rename_summary_sql = IF(
    @has_summary > 0 AND @has_description = 0,
    'ALTER TABLE `user_security_activities` CHANGE COLUMN `summary` `description` VARCHAR(255) NOT NULL',
    'SELECT 1'
);
PREPARE user_security_stmt FROM @rename_summary_sql;
EXECUTE user_security_stmt;
DEALLOCATE PREPARE user_security_stmt;

SET @has_changed_fields = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @current_schema
      AND TABLE_NAME = 'user_security_activities'
      AND COLUMN_NAME = 'changed_fields'
);

SET @add_changed_fields_sql = IF(
    @has_changed_fields = 0,
    'ALTER TABLE `user_security_activities` ADD COLUMN `changed_fields` TEXT NULL AFTER `description`',
    'SELECT 1'
);
PREPARE user_security_stmt FROM @add_changed_fields_sql;
EXECUTE user_security_stmt;
DEALLOCATE PREPARE user_security_stmt;

ALTER TABLE `user_security_activities`
    MODIFY COLUMN `activity_type` VARCHAR(50) NOT NULL,
    MODIFY COLUMN `description` VARCHAR(255) NOT NULL,
    MODIFY COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
