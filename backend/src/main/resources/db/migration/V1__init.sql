CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT AUTO_INCREMENT,
    `email` VARCHAR(255) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `role` VARCHAR(50) NOT NULL,
    `status` VARCHAR(50) DEFAULT 'ACTIVE',
    `full_name` VARCHAR(100),
    `phone_number` VARCHAR(50),
    `login_fail_count` INTEGER DEFAULT 0,
    `lock_time` DATETIME NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `deleted_at` DATETIME DEFAULT NULL,
    PRIMARY KEY(`id`)
    ) COMMENT='User table';


CREATE TABLE IF NOT EXISTS `verification_codes` (
    `id` BIGINT AUTO_INCREMENT,
    `email` VARCHAR(255) NOT NULL COMMENT 'Recipient email',
    `code` VARCHAR(10) NOT NULL COMMENT '6-digit verification code',
    `type` VARCHAR(50) NOT NULL COMMENT 'Type: REGISTRATION, PASSWORD_RESET',
    `is_used` TINYINT DEFAULT 0 COMMENT 'Used or not',
    `expires_at` DATETIME NOT NULL COMMENT 'Expiration time',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(`id`)
    ) COMMENT='Verification code table';


CREATE TABLE IF NOT EXISTS `refresh_tokens` (
    `id` BIGINT AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `token` VARCHAR(255) NOT NULL UNIQUE,
    `expires_at` DATETIME NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(`id`)
    ) COMMENT='Login session refresh token table';


CREATE TABLE IF NOT EXISTS `expertise_categories` (
    `id` BIGINT AUTO_INCREMENT,
    `category_name` VARCHAR(100) NOT NULL UNIQUE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(`id`)
    ) COMMENT='Expertise category table';


CREATE TABLE IF NOT EXISTS `specialist_profiles` (
    `id` BIGINT AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL UNIQUE,
    `category_id` BIGINT NOT NULL,
    `level` VARCHAR(50),
    `consultation_fee` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `avatar_url` VARCHAR(500),
    `bio` TEXT,
    `status` VARCHAR(50) DEFAULT 'ACTIVE',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(`id`)
    ) COMMENT='Specialist profile table';


CREATE TABLE IF NOT EXISTS `availability_recurring_rules` (
    `id` BIGINT AUTO_INCREMENT,
    `specialist_id` BIGINT NOT NULL,
    `day_of_week` TINYINT NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL,
    `effective_end_date` DATE NOT NULL,
    `is_active` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(`id`)
    ) COMMENT='Specialist recurring schedule rule table';


CREATE TABLE IF NOT EXISTS `time_slots` (
    `id` BIGINT AUTO_INCREMENT,
    `specialist_id` BIGINT NOT NULL,
    `recurring_rule_id` BIGINT NULL,
    `slot_date` DATE NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL,
    `status` VARCHAR(50) DEFAULT 'AVAILABLE',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(`id`)
    ) COMMENT='Specialist time slot table';


CREATE TABLE IF NOT EXISTS `bookings` (
    `id` BIGINT AUTO_INCREMENT,
    `customer_id` BIGINT NOT NULL,
    `specialist_id` BIGINT NOT NULL,
    `slot_id` BIGINT NOT NULL UNIQUE,
    `status` VARCHAR(50) DEFAULT 'PENDING',
    `price` DECIMAL(10,2) NOT NULL,
    `topic` VARCHAR(255),
    `customer_notes` TEXT,
    `parent_booking_id` BIGINT NOT NULL COMMENT 'Original booking ID for reschedule',
    `rejection_reason` TEXT,
    `decision_time` DATETIME NOT NULL,
    `cancelled_by` VARCHAR(50) NOT NULL COMMENT 'Cancelled by',
    `cancel_reason` TEXT,
    `change_type` VARCHAR(50) NOT NULL COMMENT 'CANCEL/RESCHEDULE',
    `refund_status` VARCHAR(50) DEFAULT 'NONE',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(`id`)
    ) COMMENT='Booking record core table';


CREATE TABLE IF NOT EXISTS `remember_me_tokens` (
    `id` BIGINT AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `token` VARCHAR(255) NOT NULL UNIQUE,
    `expires_at` DATETIME NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(`id`)
    ) COMMENT='Remember me token table';


CREATE TABLE IF NOT EXISTS `refund_penalties` (
    `id` BIGINT AUTO_INCREMENT,
    `booking_id` BIGINT NOT NULL,
    `refund_amount` DECIMAL(10,2) NOT NULL,
    `penalty_amount` DECIMAL(10,2) NOT NULL DEFAULT 0,
    `calculation_rule` VARCHAR(255) NOT NULL,
    `status` VARCHAR(50) NOT NULL,
    `processed_at` DATETIME NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(`id`)
    ) COMMENT='Refund and penalty record table';


ALTER TABLE `refresh_tokens`
    ADD FOREIGN KEY(`user_id`) REFERENCES `users`(`id`)
        ON UPDATE NO ACTION ON DELETE CASCADE;
ALTER TABLE `specialist_profiles`
    ADD FOREIGN KEY(`user_id`) REFERENCES `users`(`id`)
        ON UPDATE NO ACTION ON DELETE CASCADE;
ALTER TABLE `specialist_profiles`
    ADD FOREIGN KEY(`category_id`) REFERENCES `expertise_categories`(`id`)
        ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE `availability_recurring_rules`
    ADD FOREIGN KEY(`specialist_id`) REFERENCES `specialist_profiles`(`id`)
        ON UPDATE NO ACTION ON DELETE CASCADE;
ALTER TABLE `time_slots`
    ADD FOREIGN KEY(`specialist_id`) REFERENCES `specialist_profiles`(`id`)
        ON UPDATE NO ACTION ON DELETE CASCADE;
ALTER TABLE `time_slots`
    ADD FOREIGN KEY(`recurring_rule_id`) REFERENCES `availability_recurring_rules`(`id`)
        ON UPDATE NO ACTION ON DELETE SET NULL;
ALTER TABLE `bookings`
    ADD FOREIGN KEY(`customer_id`) REFERENCES `users`(`id`)
        ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE `bookings`
    ADD FOREIGN KEY(`specialist_id`) REFERENCES `specialist_profiles`(`id`)
        ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE `bookings`
    ADD FOREIGN KEY(`slot_id`) REFERENCES `time_slots`(`id`)
        ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE `bookings`
    ADD FOREIGN KEY(`parent_booking_id`) REFERENCES `bookings`(`id`)
        ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE `refund_penalties`
    ADD FOREIGN KEY(`booking_id`) REFERENCES `bookings`(`id`)
        ON UPDATE NO ACTION ON DELETE CASCADE;