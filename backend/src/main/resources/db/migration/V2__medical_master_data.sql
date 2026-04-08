CREATE TABLE IF NOT EXISTS `booking_topics` (
    `id` BIGINT AUTO_INCREMENT,
    `topic_code` VARCHAR(64) NOT NULL UNIQUE,
    `topic_name` VARCHAR(100) NOT NULL UNIQUE,
    `sort_order` INT NOT NULL DEFAULT 0,
    `is_active` TINYINT NOT NULL DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) COMMENT='Booking topic master data';

ALTER TABLE `bookings`
    MODIFY `parent_booking_id` BIGINT NULL COMMENT 'Original booking ID for reschedule';

ALTER TABLE `bookings`
    MODIFY `decision_time` DATETIME NULL;

ALTER TABLE `bookings`
    MODIFY `cancelled_by` VARCHAR(50) NULL COMMENT 'Cancelled by';

ALTER TABLE `bookings`
    MODIFY `change_type` VARCHAR(50) NULL COMMENT 'CANCEL/RESCHEDULE';

INSERT INTO `expertise_categories` (`category_name`)
SELECT seed.category_name
FROM (
    SELECT 'Pediatrics' AS category_name
    UNION ALL SELECT 'Dermatology'
    UNION ALL SELECT 'Orthopedics'
    UNION ALL SELECT 'Cardiology'
    UNION ALL SELECT 'Psychiatry'
    UNION ALL SELECT 'Gynecology'
) seed
LEFT JOIN `expertise_categories` existing
    ON existing.category_name = seed.category_name
WHERE existing.id IS NULL;

INSERT INTO `booking_topics` (`topic_code`, `topic_name`, `sort_order`, `is_active`)
SELECT seed.topic_code, seed.topic_name, seed.sort_order, 1
FROM (
    SELECT 'INITIAL_CONSULTATION' AS topic_code, 'Initial Consultation' AS topic_name, 1 AS sort_order
    UNION ALL SELECT 'SYMPTOM_ASSESSMENT', 'Symptom Assessment', 2
    UNION ALL SELECT 'TEST_RESULTS_REVIEW', 'Test Results Review', 3
    UNION ALL SELECT 'TREATMENT_PLANNING', 'Treatment Planning', 4
    UNION ALL SELECT 'MEDICATION_REVIEW', 'Medication Review', 5
    UNION ALL SELECT 'FOLLOW_UP_REVIEW', 'Follow-up Review', 6
) seed
LEFT JOIN `booking_topics` existing
    ON existing.topic_code = seed.topic_code
WHERE existing.id IS NULL;

INSERT INTO `users` (
    `email`,
    `password_hash`,
    `role`,
    `status`,
    `full_name`,
    `phone_number`,
    `login_fail_count`,
    `lock_time`
)
SELECT
    seed.email,
    '$2a$10$9Vok.PTkoXIZkfdbFJnfa.dMSPrCUUNuEGpDTcDdxuU.XSd3hnOGu',
    'SPECIALIST',
    'ACTIVE',
    seed.full_name,
    seed.phone_number,
    0,
    NOW()
FROM (
    SELECT 'emily.chen@medlink.local' AS email, 'Dr. Emily Chen' AS full_name, '13800000001' AS phone_number
    UNION ALL SELECT 'ryan.lin@medlink.local', 'Dr. Ryan Lin', '13800000002'
    UNION ALL SELECT 'sophie.zhao@medlink.local', 'Dr. Sophie Zhao', '13800000003'
    UNION ALL SELECT 'jason.xu@medlink.local', 'Dr. Jason Xu', '13800000004'
    UNION ALL SELECT 'olivia.wang@medlink.local', 'Dr. Olivia Wang', '13800000005'
    UNION ALL SELECT 'daniel.sun@medlink.local', 'Dr. Daniel Sun', '13800000006'
    UNION ALL SELECT 'grace.liu@medlink.local', 'Dr. Grace Liu', '13800000007'
    UNION ALL SELECT 'leo.he@medlink.local', 'Dr. Leo He', '13800000008'
    UNION ALL SELECT 'michael.zhang@medlink.local', 'Dr. Michael Zhang', '13800000009'
    UNION ALL SELECT 'hannah.wu@medlink.local', 'Dr. Hannah Wu', '13800000010'
    UNION ALL SELECT 'kevin.ma@medlink.local', 'Dr. Kevin Ma', '13800000011'
    UNION ALL SELECT 'ruby.gao@medlink.local', 'Dr. Ruby Gao', '13800000012'
    UNION ALL SELECT 'benjamin.li@medlink.local', 'Dr. Benjamin Li', '13800000013'
    UNION ALL SELECT 'chloe.deng@medlink.local', 'Dr. Chloe Deng', '13800000014'
    UNION ALL SELECT 'ethan.qiu@medlink.local', 'Dr. Ethan Qiu', '13800000015'
    UNION ALL SELECT 'mia.tang@medlink.local', 'Dr. Mia Tang', '13800000016'
    UNION ALL SELECT 'isabella.zhou@medlink.local', 'Dr. Isabella Zhou', '13800000017'
    UNION ALL SELECT 'noah.yu@medlink.local', 'Dr. Noah Yu', '13800000018'
    UNION ALL SELECT 'vivian.feng@medlink.local', 'Dr. Vivian Feng', '13800000019'
    UNION ALL SELECT 'lucas.ren@medlink.local', 'Dr. Lucas Ren', '13800000020'
    UNION ALL SELECT 'victoria.jiang@medlink.local', 'Dr. Victoria Jiang', '13800000021'
    UNION ALL SELECT 'eric.luo@medlink.local', 'Dr. Eric Luo', '13800000022'
    UNION ALL SELECT 'ava.shen@medlink.local', 'Dr. Ava Shen', '13800000023'
    UNION ALL SELECT 'nicole.xie@medlink.local', 'Dr. Nicole Xie', '13800000024'
) seed
LEFT JOIN `users` existing
    ON existing.email = seed.email
WHERE existing.id IS NULL;

INSERT INTO `specialist_profiles` (
    `user_id`,
    `category_id`,
    `level`,
    `consultation_fee`,
    `avatar_url`,
    `bio`,
    `status`
)
SELECT
    u.id,
    c.id,
    seed.level,
    seed.consultation_fee,
    NULL,
    seed.bio,
    seed.status
FROM (
    SELECT 'emily.chen@medlink.local' AS email, 'Pediatrics' AS category_name, 'CHIEF' AS level, 260.00 AS consultation_fee, 'ACTIVE' AS status, 'Chief pediatric specialist focusing on growth monitoring and common pediatric conditions.' AS bio
    UNION ALL SELECT 'ryan.lin@medlink.local', 'Pediatrics', 'SENIOR', 220.00, 'ACTIVE', 'Senior pediatric doctor supporting vaccination plans and child respiratory care.'
    UNION ALL SELECT 'sophie.zhao@medlink.local', 'Pediatrics', 'INTERMEDIATE', 170.00, 'ACTIVE', 'Pediatric doctor focusing on nutrition advice and adolescent health follow-up.'
    UNION ALL SELECT 'jason.xu@medlink.local', 'Pediatrics', 'JUNIOR', 120.00, 'INACTIVE', 'General pediatric doctor for routine consultations and recovery monitoring.'
    UNION ALL SELECT 'olivia.wang@medlink.local', 'Dermatology', 'CHIEF', 255.00, 'ACTIVE', 'Chief dermatology specialist for chronic skin disease and long-term treatment planning.'
    UNION ALL SELECT 'daniel.sun@medlink.local', 'Dermatology', 'SENIOR', 215.00, 'ACTIVE', 'Senior dermatologist for acne, eczema, and recurring skin irritation.'
    UNION ALL SELECT 'grace.liu@medlink.local', 'Dermatology', 'INTERMEDIATE', 165.00, 'INACTIVE', 'Dermatology doctor handling allergy-related skin complaints and follow-up checks.'
    UNION ALL SELECT 'leo.he@medlink.local', 'Dermatology', 'JUNIOR', 115.00, 'ACTIVE', 'General dermatology doctor for common outpatient skin assessments.'
    UNION ALL SELECT 'michael.zhang@medlink.local', 'Orthopedics', 'CHIEF', 275.00, 'ACTIVE', 'Chief orthopedic specialist for joint disorders and fracture recovery.'
    UNION ALL SELECT 'hannah.wu@medlink.local', 'Orthopedics', 'SENIOR', 225.00, 'ACTIVE', 'Senior orthopedic doctor with focus on sports injury and spinal pain management.'
    UNION ALL SELECT 'kevin.ma@medlink.local', 'Orthopedics', 'INTERMEDIATE', 175.00, 'ACTIVE', 'Orthopedic doctor providing muscle, bone, and mobility assessments.'
    UNION ALL SELECT 'ruby.gao@medlink.local', 'Orthopedics', 'JUNIOR', 125.00, 'INACTIVE', 'General orthopedic doctor for routine follow-up and rehabilitation review.'
    UNION ALL SELECT 'benjamin.li@medlink.local', 'Cardiology', 'CHIEF', 290.00, 'ACTIVE', 'Chief cardiology specialist for hypertension and cardiac risk management.'
    UNION ALL SELECT 'chloe.deng@medlink.local', 'Cardiology', 'SENIOR', 235.00, 'INACTIVE', 'Senior cardiology doctor for arrhythmia review and chest discomfort follow-up.'
    UNION ALL SELECT 'ethan.qiu@medlink.local', 'Cardiology', 'INTERMEDIATE', 185.00, 'ACTIVE', 'Cardiology doctor supporting ECG review and blood pressure monitoring.'
    UNION ALL SELECT 'mia.tang@medlink.local', 'Cardiology', 'JUNIOR', 130.00, 'ACTIVE', 'General cardiovascular doctor for first-line outpatient consultations.'
    UNION ALL SELECT 'isabella.zhou@medlink.local', 'Psychiatry', 'CHIEF', 280.00, 'ACTIVE', 'Chief psychiatry specialist for anxiety, depression, and sleep disorders.'
    UNION ALL SELECT 'noah.yu@medlink.local', 'Psychiatry', 'SENIOR', 230.00, 'ACTIVE', 'Senior psychiatry doctor for stress, mood symptoms, and medication review.'
    UNION ALL SELECT 'vivian.feng@medlink.local', 'Psychiatry', 'INTERMEDIATE', 180.00, 'ACTIVE', 'Psychiatry doctor supporting emotional assessment and structured follow-up.'
    UNION ALL SELECT 'lucas.ren@medlink.local', 'Psychiatry', 'JUNIOR', 128.00, 'INACTIVE', 'General mental health doctor for routine psychiatric consultations.'
    UNION ALL SELECT 'victoria.jiang@medlink.local', 'Gynecology', 'CHIEF', 285.00, 'ACTIVE', 'Chief gynecology specialist for reproductive health and menstrual disorders.'
    UNION ALL SELECT 'eric.luo@medlink.local', 'Gynecology', 'SENIOR', 240.00, 'ACTIVE', 'Senior gynecology doctor for routine women''s health consultation and review.'
    UNION ALL SELECT 'ava.shen@medlink.local', 'Gynecology', 'INTERMEDIATE', 190.00, 'INACTIVE', 'Gynecology doctor focusing on hormonal issues and follow-up care.'
    UNION ALL SELECT 'nicole.xie@medlink.local', 'Gynecology', 'JUNIOR', 135.00, 'ACTIVE', 'General women''s health doctor for outpatient consultation and follow-up.'
) seed
JOIN `users` u
    ON u.email = seed.email
JOIN `expertise_categories` c
    ON c.category_name = seed.category_name
LEFT JOIN `specialist_profiles` sp
    ON sp.user_id = u.id
WHERE sp.id IS NULL;

INSERT INTO `time_slots` (
    `specialist_id`,
    `recurring_rule_id`,
    `slot_date`,
    `start_time`,
    `end_time`,
    `status`
)
SELECT
    sp.id,
    NULL,
    DATE_ADD(CURDATE(), INTERVAL CASE MOD(sp.id, 3) WHEN 0 THEN 1 WHEN 1 THEN 2 ELSE 3 END DAY),
    CASE sp.level
        WHEN 'CHIEF' THEN '09:00:00'
        WHEN 'SENIOR' THEN '10:00:00'
        WHEN 'INTERMEDIATE' THEN '14:00:00'
        ELSE '15:00:00'
    END,
    CASE sp.level
        WHEN 'CHIEF' THEN '09:30:00'
        WHEN 'SENIOR' THEN '10:30:00'
        WHEN 'INTERMEDIATE' THEN '14:30:00'
        ELSE '15:30:00'
    END,
    'AVAILABLE'
FROM `specialist_profiles` sp
JOIN `users` u
    ON u.id = sp.user_id
WHERE sp.status = 'ACTIVE'
  AND u.email LIKE '%@medlink.local'
  AND NOT EXISTS (
    SELECT 1
    FROM `time_slots` ts
    WHERE ts.specialist_id = sp.id
      AND ts.slot_date = DATE_ADD(CURDATE(), INTERVAL CASE MOD(sp.id, 3) WHEN 0 THEN 1 WHEN 1 THEN 2 ELSE 3 END DAY)
  );

INSERT INTO `time_slots` (
    `specialist_id`,
    `recurring_rule_id`,
    `slot_date`,
    `start_time`,
    `end_time`,
    `status`
)
SELECT
    sp.id,
    NULL,
    DATE_ADD(CURDATE(), INTERVAL CASE MOD(sp.id, 4) WHEN 0 THEN 5 WHEN 1 THEN 6 WHEN 2 THEN 7 ELSE 8 END DAY),
    CASE sp.level
        WHEN 'CHIEF' THEN '16:00:00'
        WHEN 'SENIOR' THEN '11:00:00'
        WHEN 'INTERMEDIATE' THEN '15:00:00'
        ELSE '16:30:00'
    END,
    CASE sp.level
        WHEN 'CHIEF' THEN '16:30:00'
        WHEN 'SENIOR' THEN '11:30:00'
        WHEN 'INTERMEDIATE' THEN '15:30:00'
        ELSE '17:00:00'
    END,
    'AVAILABLE'
FROM `specialist_profiles` sp
JOIN `users` u
    ON u.id = sp.user_id
WHERE sp.status = 'ACTIVE'
  AND u.email LIKE '%@medlink.local'
  AND NOT EXISTS (
    SELECT 1
    FROM `time_slots` ts
    WHERE ts.specialist_id = sp.id
      AND ts.slot_date = DATE_ADD(CURDATE(), INTERVAL CASE MOD(sp.id, 4) WHEN 0 THEN 5 WHEN 1 THEN 6 WHEN 2 THEN 7 ELSE 8 END DAY)
  );
