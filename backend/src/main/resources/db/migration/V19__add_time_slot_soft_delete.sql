ALTER TABLE `time_slots`
    ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Soft delete flag: 0 active, 1 deleted' AFTER `status`;

CREATE INDEX `idx_time_slots_specialist_date_deleted`
    ON `time_slots` (`specialist_id`, `slot_date`, `is_deleted`);
