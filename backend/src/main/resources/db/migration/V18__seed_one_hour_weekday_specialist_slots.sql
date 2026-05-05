DROP TEMPORARY TABLE IF EXISTS `tmp_active_specialists`;
DROP TEMPORARY TABLE IF EXISTS `tmp_future_weekdays`;
DROP TEMPORARY TABLE IF EXISTS `tmp_one_hour_specialist_slots`;

CREATE TEMPORARY TABLE `tmp_active_specialists` AS
SELECT
    ranked.specialist_id,
    ranked.specialist_index,
    COUNT(*) OVER () AS specialist_count
FROM (
    SELECT
        sp.id AS specialist_id,
        ROW_NUMBER() OVER (ORDER BY sp.id) - 1 AS specialist_index
    FROM `specialist_profiles` sp
    JOIN `users` u
        ON u.id = sp.user_id
    WHERE u.role = 'SPECIALIST'
      AND u.deleted_at IS NULL
      AND sp.status = 'ACTIVE'
) ranked;

CREATE TEMPORARY TABLE `tmp_future_weekdays` AS
SELECT
    filtered.day_offset,
    filtered.slot_date,
    ROW_NUMBER() OVER (ORDER BY filtered.day_offset) - 1 AS weekday_index
FROM (
    SELECT
        days.day_offset,
        DATE_ADD(CURDATE(), INTERVAL days.day_offset DAY) AS slot_date
    FROM (
        SELECT 1 AS day_offset
        UNION ALL SELECT 2
        UNION ALL SELECT 3
        UNION ALL SELECT 4
        UNION ALL SELECT 5
        UNION ALL SELECT 6
        UNION ALL SELECT 7
        UNION ALL SELECT 8
        UNION ALL SELECT 9
        UNION ALL SELECT 10
        UNION ALL SELECT 11
        UNION ALL SELECT 12
        UNION ALL SELECT 13
        UNION ALL SELECT 14
        UNION ALL SELECT 15
        UNION ALL SELECT 16
        UNION ALL SELECT 17
        UNION ALL SELECT 18
        UNION ALL SELECT 19
        UNION ALL SELECT 20
        UNION ALL SELECT 21
        UNION ALL SELECT 22
        UNION ALL SELECT 23
        UNION ALL SELECT 24
        UNION ALL SELECT 25
        UNION ALL SELECT 26
        UNION ALL SELECT 27
        UNION ALL SELECT 28
        UNION ALL SELECT 29
        UNION ALL SELECT 30
        UNION ALL SELECT 31
        UNION ALL SELECT 32
        UNION ALL SELECT 33
        UNION ALL SELECT 34
        UNION ALL SELECT 35
        UNION ALL SELECT 36
        UNION ALL SELECT 37
        UNION ALL SELECT 38
        UNION ALL SELECT 39
        UNION ALL SELECT 40
        UNION ALL SELECT 41
        UNION ALL SELECT 42
        UNION ALL SELECT 43
        UNION ALL SELECT 44
        UNION ALL SELECT 45
        UNION ALL SELECT 46
        UNION ALL SELECT 47
        UNION ALL SELECT 48
    ) days
) filtered
WHERE WEEKDAY(filtered.slot_date) < 5;

CREATE TEMPORARY TABLE `tmp_one_hour_specialist_slots` (
    `specialist_id` BIGINT NOT NULL,
    `slot_date` DATE NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL,
    PRIMARY KEY (`specialist_id`, `slot_date`, `start_time`)
);

INSERT INTO `tmp_one_hour_specialist_slots` (
    `specialist_id`,
    `slot_date`,
    `start_time`,
    `end_time`
)
SELECT
    specialists.specialist_id,
    days.slot_date,
    slot_time.start_time,
    ADDTIME(slot_time.start_time, '01:00:00')
FROM `tmp_future_weekdays` days
CROSS JOIN (
    SELECT 0 AS slot_index
    UNION ALL SELECT 1
    UNION ALL SELECT 2
) daily_slots
JOIN `tmp_active_specialists` specialists
    ON specialists.specialist_index = MOD(days.weekday_index * 5 + daily_slots.slot_index * 7, specialists.specialist_count)
JOIN (
    SELECT 0 AS slot_index, 0 AS time_variant, '09:00:00' AS start_time
    UNION ALL SELECT 0, 1, '10:00:00'
    UNION ALL SELECT 0, 2, '11:00:00'
    UNION ALL SELECT 1, 0, '13:30:00'
    UNION ALL SELECT 1, 1, '14:30:00'
    UNION ALL SELECT 2, 0, '15:30:00'
    UNION ALL SELECT 2, 1, '16:00:00'
) slot_time
    ON slot_time.slot_index = daily_slots.slot_index
   AND slot_time.time_variant = MOD(
       days.weekday_index + daily_slots.slot_index,
       CASE daily_slots.slot_index WHEN 0 THEN 3 ELSE 2 END
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
    seed.specialist_id,
    NULL,
    seed.slot_date,
    seed.start_time,
    seed.end_time,
    'AVAILABLE'
FROM `tmp_one_hour_specialist_slots` seed
LEFT JOIN `time_slots` existing
    ON existing.specialist_id = seed.specialist_id
   AND existing.slot_date = seed.slot_date
   AND NOT (
       existing.end_time <= seed.start_time
       OR existing.start_time >= seed.end_time
   )
WHERE existing.id IS NULL;

INSERT INTO `time_slots` (
    `specialist_id`,
    `recurring_rule_id`,
    `slot_date`,
    `start_time`,
    `end_time`,
    `status`
)
SELECT
    fallback.specialist_id,
    NULL,
    fallback.slot_date,
    fallback.start_time,
    ADDTIME(fallback.start_time, '01:00:00'),
    'AVAILABLE'
FROM (
    SELECT
        days.slot_date,
        specialists.specialist_id,
        backup_time.start_time,
        ROW_NUMBER() OVER (
            PARTITION BY days.slot_date
            ORDER BY MOD(days.weekday_index * 17 + specialists.specialist_index * 13 + backup_time.time_index * 19, 997)
        ) AS candidate_rank
    FROM `tmp_future_weekdays` days
    CROSS JOIN `tmp_active_specialists` specialists
    CROSS JOIN (
        SELECT 0 AS time_index, '09:00:00' AS start_time
        UNION ALL SELECT 1, '10:00:00'
        UNION ALL SELECT 2, '11:00:00'
        UNION ALL SELECT 3, '13:30:00'
        UNION ALL SELECT 4, '14:30:00'
        UNION ALL SELECT 5, '15:30:00'
        UNION ALL SELECT 6, '16:00:00'
    ) backup_time
    LEFT JOIN `time_slots` existing
        ON existing.specialist_id = specialists.specialist_id
       AND existing.slot_date = days.slot_date
       AND NOT (
           existing.end_time <= backup_time.start_time
           OR existing.start_time >= ADDTIME(backup_time.start_time, '01:00:00')
       )
    WHERE existing.id IS NULL
      AND NOT EXISTS (
          SELECT 1
          FROM `time_slots` ts
          JOIN `specialist_profiles` sp
              ON sp.id = ts.specialist_id
          JOIN `users` u
              ON u.id = sp.user_id
          WHERE ts.slot_date = days.slot_date
            AND ts.status = 'AVAILABLE'
            AND u.role = 'SPECIALIST'
            AND u.deleted_at IS NULL
            AND sp.status = 'ACTIVE'
      )
) fallback
WHERE fallback.candidate_rank = 1;

DROP TEMPORARY TABLE IF EXISTS `tmp_one_hour_specialist_slots`;
DROP TEMPORARY TABLE IF EXISTS `tmp_future_weekdays`;
DROP TEMPORARY TABLE IF EXISTS `tmp_active_specialists`;
