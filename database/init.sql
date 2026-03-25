-- 1. 用户 

CREATE TABLE IF NOT EXISTS `users` (
	`id` BIGINT AUTO_INCREMENT,
	`email` VARCHAR(255) NOT NULL UNIQUE,
	`password_hash` VARCHAR(255) NOT NULL,
	`role` VARCHAR(50) NOT NULL,
	`status` VARCHAR(50) DEFAULT 'ACTIVE',
	`full_name` VARCHAR(100),
	`phone_number` VARCHAR(50),
	`login_fail_count` INT DEFAULT 0,
	`lock_time` DATETIME NULL,
	`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
	`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	`deleted_at` DATETIME NULL,
	PRIMARY KEY(`id`)
) COMMENT='用户表';


-- 2. 验证码表
CREATE TABLE IF NOT EXISTS `verification_codes` (
	`id` BIGINT AUTO_INCREMENT,
	`email` VARCHAR(255) NOT NULL COMMENT '接收邮箱',
	`code` VARCHAR(10) NOT NULL COMMENT '6位验证码',
	`type` VARCHAR(50) NOT NULL COMMENT '类型: REGISTRATION(注册), PASSWORD_RESET(重置密码)',
	`is_used` TINYINT DEFAULT 0 COMMENT '是否已使用: 0=否, 1=是',
	`expires_at` DATETIME NOT NULL COMMENT '过期时间',
	`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
	PRIMARY KEY(`id`)
) COMMENT='验证码表';

-- 3. 刷新令牌表
CREATE TABLE IF NOT EXISTS `refresh_tokens` (
	`id` BIGINT AUTO_INCREMENT,
	`user_id` BIGINT NOT NULL COMMENT '关联用户ID',
	`token` VARCHAR(255) NOT NULL UNIQUE COMMENT 'Token字符串',
	`expires_at` DATETIME NOT NULL COMMENT '过期时间',
	`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
	PRIMARY KEY(`id`)
) COMMENT='登录会话刷新令牌表';

-- 4. 专业领域分类表
CREATE TABLE IF NOT EXISTS `expertise_categories` (
	`id` BIGINT AUTO_INCREMENT,
	`category_name` VARCHAR(100) NOT NULL UNIQUE COMMENT '分类名称',
	`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
	`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY(`id`)
) COMMENT='专业领域分类表';

-- 5. 专家资料表
CREATE TABLE IF NOT EXISTS `specialist_profiles` (
	`id` BIGINT AUTO_INCREMENT,
	`user_id` BIGINT NOT NULL UNIQUE COMMENT '关联用户ID(唯一)',
	`category_id` BIGINT NOT NULL COMMENT '所属专业分类',
	`level` VARCHAR(50) COMMENT '专家等级: JUNIOR, SENIOR, EXPERT',
	`consultation_fee` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '咨询费用',
	`avatar_url` VARCHAR(500) COMMENT '头像OSS链接',
	`bio` TEXT COMMENT '简介',
	`status` VARCHAR(50) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, INACTIVE',
	`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
	`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY(`id`)
) COMMENT='专家资料表';

-- 6. 专家循环排班规则表
CREATE TABLE IF NOT EXISTS `availability_recurring_rules` (
	`id` BIGINT AUTO_INCREMENT,
	`specialist_id` BIGINT NOT NULL COMMENT '关联专家资料ID',
	`day_of_week` TINYINT NOT NULL COMMENT '周几: 1-7',
	`start_time` TIME NOT NULL,
	`end_time` TIME NOT NULL,
	`effective_end_date` DATE NOT NULL,
	`is_active` TINYINT DEFAULT 1 COMMENT '是否启用',
	`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY(`id`)
) COMMENT='专家循环排班规则表';

-- 7. 专家时间槽表 (修正：加入 recurring_rule_id)
CREATE TABLE IF NOT EXISTS `time_slots` (
	`id` BIGINT AUTO_INCREMENT,
	`specialist_id` BIGINT NOT NULL COMMENT '关联专家资料ID',
	`recurring_rule_id` BIGINT NULL COMMENT '关联的循环规则ID(手动创建则为空)',
	`slot_date` DATE NOT NULL COMMENT '排班日期',
	`start_time` TIME NOT NULL,
	`end_time` TIME NOT NULL,
	`status` VARCHAR(50) DEFAULT 'AVAILABLE' COMMENT '状态: AVAILABLE, LOCKED, BOOKED, DISABLED',
	`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
	`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY(`id`)
	INDEX `idx_specialist_schedule` (`specialist_id`, `slot_date`, `start_time`),
	FOREIGN KEY (`specialist_id`) REFERENCES `specialist_profiles`(`id`) ON DELETE CASCADE,
	FOREIGN KEY (`recurring_rule_id`) REFERENCES `availability_recurring_rules`(`id`) ON DELETE SET NULL
) COMMENT='专家时间槽表';



-- 8. 预约记录
CREATE TABLE IF NOT EXISTS `bookings` (
	`id` BIGINT AUTO_INCREMENT,
	`customer_id` BIGINT NOT NULL COMMENT '客户ID',
	`specialist_id` BIGINT NOT NULL COMMENT '专家ID',
	`slot_id` BIGINT NOT NULL UNIQUE COMMENT '时间槽ID',
	`status` VARCHAR(50) DEFAULT 'PENDING' COMMENT '状态: PENDING, CONFIRMED, REJECTED, CANCELLED, COMPLETED',
	`price` DECIMAL(10,2) NOT NULL,
	`topic` VARCHAR(255),
	`customer_notes` TEXT,
	`rejection_reason` TEXT,
	`decision_time` DATETIME NULL COMMENT '审批时间',
	`cancel_reason` TEXT,
	`refund_status` VARCHAR(50) DEFAULT 'NONE',
	`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
	`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY(`id`)
) COMMENT='预约记录核心表';

-- 9. 记住我令牌表
CREATE TABLE IF NOT EXISTS `remember_me_tokens` (
	`id` BIGINT AUTO_INCREMENT,
	`user_id` BIGINT NOT NULL,
	`token` VARCHAR(255) NOT NULL UNIQUE,
	`expires_at` DATETIME NOT NULL,
	`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY(`id`)
) COMMENT='记住我令牌表';

-- 10. 退款与罚金记录表
CREATE TABLE IF NOT EXISTS `refund_penalties` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `booking_id` BIGINT NOT NULL COMMENT '关联预约ID',
  `refund_amount` DECIMAL(10,2) NOT NULL COMMENT '实际退款金额',
  `penalty_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '罚金金额',
  `calculation_rule` VARCHAR(255) NOT NULL COMMENT '触发规则（如 >24h/ <24h/专家取消）',
  `status` VARCHAR(50) NOT NULL COMMENT '退款状态（pending/processed/failed）',
  `processed_at` DATETIME NULL COMMENT '处理完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`booking_id`) REFERENCES `bookings`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款与罚金记录表';

-- ==========================================
-- 外键约束
-- ==========================================
ALTER TABLE `refresh_tokens` ADD FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE;
ALTER TABLE `specialist_profiles` ADD FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE;
ALTER TABLE `specialist_profiles` ADD FOREIGN KEY(`category_id`) REFERENCES `expertise_categories`(`id`);
ALTER TABLE `availability_recurring_rules` ADD FOREIGN KEY(`specialist_id`) REFERENCES `specialist_profiles`(`id`) ON DELETE CASCADE;
ALTER TABLE `time_slots` ADD FOREIGN KEY(`specialist_id`) REFERENCES `specialist_profiles`(`id`) ON DELETE CASCADE;
ALTER TABLE `time_slots` ADD FOREIGN KEY(`recurring_rule_id`) REFERENCES `availability_recurring_rules`(`id`) ON DELETE SET NULL;
ALTER TABLE `bookings` ADD FOREIGN KEY(`customer_id`) REFERENCES `users`(`id`);
ALTER TABLE `bookings` ADD FOREIGN KEY(`specialist_id`) REFERENCES `specialist_profiles`(`id`);
ALTER TABLE `bookings` ADD FOREIGN KEY(`slot_id`) REFERENCES `time_slots`(`id`);