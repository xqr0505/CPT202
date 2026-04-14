ALTER TABLE `users`
    ADD COLUMN `password_changed_at` DATETIME NULL AFTER `first_fail_time`;
