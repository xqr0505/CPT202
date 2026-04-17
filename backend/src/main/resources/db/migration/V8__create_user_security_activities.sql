CREATE TABLE IF NOT EXISTS `user_security_activities` (
    `id` BIGINT AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `event_type` VARCHAR(50) NOT NULL,
    `summary` VARCHAR(255) NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_security_activities_user_id_created_at` (`user_id`, `created_at`),
    CONSTRAINT `fk_user_security_activities_user_id`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
            ON UPDATE NO ACTION ON DELETE CASCADE
) COMMENT='Recent account and security activity records';
