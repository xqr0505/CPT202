CREATE TABLE IF NOT EXISTS `specialist_fee_change_records` (
    `id` BIGINT AUTO_INCREMENT,
    `specialist_id` BIGINT NOT NULL,
    `old_fee` DECIMAL(10, 2) NOT NULL,
    `new_fee` DECIMAL(10, 2) NOT NULL,
    `level` VARCHAR(50) NOT NULL,
    `range_min` DECIMAL(10, 2) NOT NULL,
    `range_max` DECIMAL(10, 2) NOT NULL,
    `out_of_range` TINYINT(1) NOT NULL DEFAULT 0,
    `changed_by_user_id` BIGINT NULL,
    `changed_by_name` VARCHAR(100) NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_fee_change_specialist_id` (`specialist_id`),
    CONSTRAINT `fk_fee_change_specialist_id`
        FOREIGN KEY (`specialist_id`) REFERENCES `specialist_profiles` (`id`)
) COMMENT='Specialist fee change records';
