ALTER TABLE `availability_recurring_rules`
    ADD COLUMN `effective_start_date` DATE NULL AFTER `specialist_id`;

UPDATE `availability_recurring_rules`
SET `effective_start_date` = COALESCE(DATE(`created_at`), CURDATE())
WHERE `effective_start_date` IS NULL;

ALTER TABLE `availability_recurring_rules`
    MODIFY COLUMN `effective_start_date` DATE NOT NULL AFTER `specialist_id`;

CREATE INDEX `idx_arr_effective_start_date`
    ON `availability_recurring_rules` (`effective_start_date`);
