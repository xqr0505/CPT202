CREATE TABLE IF NOT EXISTS `specialist_consultation_windows` (
    `id` BIGINT AUTO_INCREMENT,
    `specialist_id` BIGINT NOT NULL,
    `day_of_week` TINYINT NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_specialist_consultation_window` (`specialist_id`, `day_of_week`, `start_time`, `end_time`)
) COMMENT='Weekly consultation windows that constrain specialist-managed slots';

ALTER TABLE `specialist_consultation_windows`
    ADD CONSTRAINT `fk_scw_specialist_id`
        FOREIGN KEY (`specialist_id`) REFERENCES `specialist_profiles` (`id`)
        ON UPDATE NO ACTION ON DELETE CASCADE;

CREATE TEMPORARY TABLE `tmp_specialist_schedule_seed` (
    `full_name` VARCHAR(100) NOT NULL,
    `category_name` VARCHAR(100) NOT NULL,
    `level` VARCHAR(50) NOT NULL,
    `day_of_week` TINYINT NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL
);

INSERT INTO `tmp_specialist_schedule_seed` (`full_name`, `category_name`, `level`, `day_of_week`, `start_time`, `end_time`)
VALUES
    ('Dr. Emily Chen', 'Pediatrics', 'CHIEF', 2, '09:00:00', '12:00:00'),
    ('Dr. Emily Chen', 'Pediatrics', 'CHIEF', 4, '09:00:00', '12:00:00'),
    ('Dr. Ryan Lin', 'Pediatrics', 'SENIOR', 1, '13:30:00', '17:00:00'),
    ('Dr. Ryan Lin', 'Pediatrics', 'SENIOR', 3, '13:30:00', '17:00:00'),
    ('Dr. Ryan Lin', 'Pediatrics', 'SENIOR', 5, '13:30:00', '17:00:00'),
    ('Dr. Sophie Zhao', 'Pediatrics', 'INTERMEDIATE', 1, '09:00:00', '12:00:00'),
    ('Dr. Sophie Zhao', 'Pediatrics', 'INTERMEDIATE', 2, '13:30:00', '17:00:00'),
    ('Dr. Sophie Zhao', 'Pediatrics', 'INTERMEDIATE', 4, '13:30:00', '17:00:00'),
    ('Dr. Sophie Zhao', 'Pediatrics', 'INTERMEDIATE', 6, '09:00:00', '12:00:00'),
    ('Dr. Jason Xu', 'Pediatrics', 'JUNIOR', 3, '09:00:00', '12:00:00'),
    ('Dr. Jason Xu', 'Pediatrics', 'JUNIOR', 7, '13:30:00', '17:00:00'),
    ('Dr. Olivia Wang', 'Dermatology', 'CHIEF', 1, '09:00:00', '12:00:00'),
    ('Dr. Olivia Wang', 'Dermatology', 'CHIEF', 5, '09:00:00', '12:00:00'),
    ('Dr. Daniel Sun', 'Dermatology', 'SENIOR', 2, '13:30:00', '17:00:00'),
    ('Dr. Daniel Sun', 'Dermatology', 'SENIOR', 4, '13:30:00', '17:00:00'),
    ('Dr. Daniel Sun', 'Dermatology', 'SENIOR', 6, '09:00:00', '12:00:00'),
    ('Dr. Grace Liu', 'Dermatology', 'INTERMEDIATE', 1, '13:30:00', '17:00:00'),
    ('Dr. Grace Liu', 'Dermatology', 'INTERMEDIATE', 3, '13:30:00', '17:00:00'),
    ('Dr. Leo He', 'Dermatology', 'JUNIOR', 3, '09:00:00', '12:00:00'),
    ('Dr. Leo He', 'Dermatology', 'JUNIOR', 5, '13:30:00', '17:00:00'),
    ('Dr. Leo He', 'Dermatology', 'JUNIOR', 7, '09:00:00', '12:00:00'),
    ('Dr. Michael Zhang', 'Orthopedics', 'CHIEF', 2, '09:00:00', '12:00:00'),
    ('Dr. Michael Zhang', 'Orthopedics', 'CHIEF', 6, '09:00:00', '12:00:00'),
    ('Dr. Hannah Wu', 'Orthopedics', 'SENIOR', 1, '13:30:00', '17:00:00'),
    ('Dr. Hannah Wu', 'Orthopedics', 'SENIOR', 3, '09:00:00', '12:00:00'),
    ('Dr. Hannah Wu', 'Orthopedics', 'SENIOR', 5, '09:00:00', '12:00:00'),
    ('Dr. Kevin Ma', 'Orthopedics', 'INTERMEDIATE', 1, '09:00:00', '12:00:00'),
    ('Dr. Kevin Ma', 'Orthopedics', 'INTERMEDIATE', 4, '09:00:00', '12:00:00'),
    ('Dr. Kevin Ma', 'Orthopedics', 'INTERMEDIATE', 2, '13:30:00', '17:00:00'),
    ('Dr. Kevin Ma', 'Orthopedics', 'INTERMEDIATE', 6, '13:30:00', '17:00:00'),
    ('Dr. Ruby Gao', 'Orthopedics', 'JUNIOR', 3, '13:30:00', '17:00:00'),
    ('Dr. Ruby Gao', 'Orthopedics', 'JUNIOR', 7, '09:00:00', '12:00:00'),
    ('Dr. Benjamin Li', 'Cardiology', 'CHIEF', 3, '13:30:00', '17:00:00'),
    ('Dr. Benjamin Li', 'Cardiology', 'CHIEF', 5, '09:00:00', '12:00:00'),
    ('Dr. Chloe Deng', 'Cardiology', 'SENIOR', 2, '09:00:00', '12:00:00'),
    ('Dr. Chloe Deng', 'Cardiology', 'SENIOR', 4, '13:30:00', '17:00:00'),
    ('Dr. Ethan Qiu', 'Cardiology', 'INTERMEDIATE', 1, '09:00:00', '12:00:00'),
    ('Dr. Ethan Qiu', 'Cardiology', 'INTERMEDIATE', 2, '09:00:00', '12:00:00'),
    ('Dr. Ethan Qiu', 'Cardiology', 'INTERMEDIATE', 6, '09:00:00', '12:00:00'),
    ('Dr. Ethan Qiu', 'Cardiology', 'INTERMEDIATE', 4, '13:30:00', '17:00:00'),
    ('Dr. Mia Tang', 'Cardiology', 'JUNIOR', 2, '13:30:00', '17:00:00'),
    ('Dr. Mia Tang', 'Cardiology', 'JUNIOR', 4, '09:00:00', '12:00:00'),
    ('Dr. Mia Tang', 'Cardiology', 'JUNIOR', 7, '13:30:00', '17:00:00'),
    ('Dr. Isabella Zhou', 'Psychiatry', 'CHIEF', 1, '13:30:00', '17:00:00'),
    ('Dr. Isabella Zhou', 'Psychiatry', 'CHIEF', 4, '13:30:00', '17:00:00'),
    ('Dr. Noah Yu', 'Psychiatry', 'SENIOR', 2, '09:00:00', '12:00:00'),
    ('Dr. Noah Yu', 'Psychiatry', 'SENIOR', 3, '13:30:00', '17:00:00'),
    ('Dr. Noah Yu', 'Psychiatry', 'SENIOR', 6, '13:30:00', '17:00:00'),
    ('Dr. Vivian Feng', 'Psychiatry', 'INTERMEDIATE', 1, '09:00:00', '12:00:00'),
    ('Dr. Vivian Feng', 'Psychiatry', 'INTERMEDIATE', 3, '09:00:00', '12:00:00'),
    ('Dr. Vivian Feng', 'Psychiatry', 'INTERMEDIATE', 5, '13:30:00', '17:00:00'),
    ('Dr. Vivian Feng', 'Psychiatry', 'INTERMEDIATE', 7, '09:00:00', '12:00:00'),
    ('Dr. Lucas Ren', 'Psychiatry', 'JUNIOR', 2, '13:30:00', '17:00:00'),
    ('Dr. Lucas Ren', 'Psychiatry', 'JUNIOR', 5, '09:00:00', '12:00:00'),
    ('Dr. Victoria Jiang', 'Gynecology', 'CHIEF', 2, '13:30:00', '17:00:00'),
    ('Dr. Victoria Jiang', 'Gynecology', 'CHIEF', 5, '13:30:00', '17:00:00'),
    ('Dr. Eric Luo', 'Gynecology', 'SENIOR', 1, '09:00:00', '12:00:00'),
    ('Dr. Eric Luo', 'Gynecology', 'SENIOR', 4, '09:00:00', '12:00:00'),
    ('Dr. Eric Luo', 'Gynecology', 'SENIOR', 6, '09:00:00', '12:00:00'),
    ('Dr. Ava Shen', 'Gynecology', 'INTERMEDIATE', 3, '09:00:00', '12:00:00'),
    ('Dr. Ava Shen', 'Gynecology', 'INTERMEDIATE', 7, '13:30:00', '17:00:00'),
    ('Dr. Nicole Xie', 'Gynecology', 'JUNIOR', 3, '13:30:00', '17:00:00'),
    ('Dr. Nicole Xie', 'Gynecology', 'JUNIOR', 5, '09:00:00', '12:00:00'),
    ('Dr. Nicole Xie', 'Gynecology', 'JUNIOR', 7, '09:00:00', '12:00:00');

CREATE TEMPORARY TABLE `tmp_target_specialists` AS
SELECT DISTINCT sp.id AS specialist_id
FROM `tmp_specialist_schedule_seed` seed
JOIN `users` u
    ON u.full_name = seed.full_name
JOIN `specialist_profiles` sp
    ON sp.user_id = u.id
   AND sp.level = seed.level
JOIN `expertise_categories` c
    ON c.id = sp.category_id
   AND c.category_name = seed.category_name;

UPDATE `bookings` b
JOIN `tmp_target_specialists` ts
    ON ts.specialist_id = b.specialist_id
SET b.parent_booking_id = NULL;

DELETE rp
FROM `refund_penalties` rp
JOIN `bookings` b
    ON b.id = rp.booking_id
JOIN `tmp_target_specialists` ts
    ON ts.specialist_id = b.specialist_id;

DELETE b
FROM `bookings` b
JOIN `tmp_target_specialists` ts
    ON ts.specialist_id = b.specialist_id;

DELETE scw
FROM `specialist_consultation_windows` scw
JOIN `tmp_target_specialists` ts
    ON ts.specialist_id = scw.specialist_id;

DELETE ts
FROM `time_slots` ts
JOIN `tmp_target_specialists` target
    ON target.specialist_id = ts.specialist_id;

DELETE arr
FROM `availability_recurring_rules` arr
JOIN `tmp_target_specialists` target
    ON target.specialist_id = arr.specialist_id;

INSERT INTO `specialist_consultation_windows` (`specialist_id`, `day_of_week`, `start_time`, `end_time`)
SELECT
    sp.id,
    seed.day_of_week,
    seed.start_time,
    seed.end_time
FROM `tmp_specialist_schedule_seed` seed
JOIN `users` u
    ON u.full_name = seed.full_name
JOIN `specialist_profiles` sp
    ON sp.user_id = u.id
   AND sp.level = seed.level
JOIN `expertise_categories` c
    ON c.id = sp.category_id
   AND c.category_name = seed.category_name
LEFT JOIN `specialist_consultation_windows` existing
    ON existing.specialist_id = sp.id
   AND existing.day_of_week = seed.day_of_week
   AND existing.start_time = seed.start_time
   AND existing.end_time = seed.end_time
WHERE existing.id IS NULL;

DROP TEMPORARY TABLE IF EXISTS `tmp_target_specialists`;
DROP TEMPORARY TABLE IF EXISTS `tmp_specialist_schedule_seed`;
