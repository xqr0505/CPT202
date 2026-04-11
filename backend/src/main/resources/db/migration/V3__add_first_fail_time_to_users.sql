ALTER TABLE `users` 
ADD COLUMN `first_fail_time` DATETIME NULL DEFAULT NULL COMMENT 'First failure time (for a 3-minute window)' AFTER `lock_time`;