INSERT INTO `availability_recurring_rules` (
    `specialist_id`,
    `day_of_week`,
    `start_time`,
    `end_time`,
    `effective_end_date`,
    `is_active`
)
SELECT
    sp.id,
    seed.day_of_week,
    seed.start_time,
    seed.end_time,
    '2026-05-17',
    1
FROM (
    SELECT 'emily.chen@medlink.local' AS email, 2 AS day_of_week, '09:00:00' AS start_time, '12:00:00' AS end_time
    UNION ALL SELECT 'emily.chen@medlink.local', 4, '09:00:00', '12:00:00'
    UNION ALL SELECT 'ryan.lin@medlink.local', 1, '13:30:00', '17:00:00'
    UNION ALL SELECT 'ryan.lin@medlink.local', 3, '13:30:00', '17:00:00'
    UNION ALL SELECT 'ryan.lin@medlink.local', 5, '13:30:00', '17:00:00'
    UNION ALL SELECT 'sophie.zhao@medlink.local', 1, '09:00:00', '12:00:00'
    UNION ALL SELECT 'sophie.zhao@medlink.local', 2, '13:30:00', '17:00:00'
    UNION ALL SELECT 'sophie.zhao@medlink.local', 4, '13:30:00', '17:00:00'
    UNION ALL SELECT 'sophie.zhao@medlink.local', 6, '09:00:00', '12:00:00'
    UNION ALL SELECT 'jason.xu@medlink.local', 3, '09:00:00', '12:00:00'
    UNION ALL SELECT 'jason.xu@medlink.local', 7, '13:30:00', '17:00:00'
    UNION ALL SELECT 'olivia.wang@medlink.local', 1, '09:00:00', '12:00:00'
    UNION ALL SELECT 'olivia.wang@medlink.local', 5, '09:00:00', '12:00:00'
    UNION ALL SELECT 'daniel.sun@medlink.local', 2, '13:30:00', '17:00:00'
    UNION ALL SELECT 'daniel.sun@medlink.local', 4, '13:30:00', '17:00:00'
    UNION ALL SELECT 'daniel.sun@medlink.local', 6, '09:00:00', '12:00:00'
    UNION ALL SELECT 'grace.liu@medlink.local', 1, '13:30:00', '17:00:00'
    UNION ALL SELECT 'grace.liu@medlink.local', 3, '13:30:00', '17:00:00'
    UNION ALL SELECT 'leo.he@medlink.local', 3, '09:00:00', '12:00:00'
    UNION ALL SELECT 'leo.he@medlink.local', 5, '13:30:00', '17:00:00'
    UNION ALL SELECT 'leo.he@medlink.local', 7, '09:00:00', '12:00:00'
    UNION ALL SELECT 'michael.zhang@medlink.local', 2, '09:00:00', '12:00:00'
    UNION ALL SELECT 'michael.zhang@medlink.local', 6, '09:00:00', '12:00:00'
    UNION ALL SELECT 'hannah.wu@medlink.local', 1, '13:30:00', '17:00:00'
    UNION ALL SELECT 'hannah.wu@medlink.local', 3, '09:00:00', '12:00:00'
    UNION ALL SELECT 'hannah.wu@medlink.local', 5, '09:00:00', '12:00:00'
    UNION ALL SELECT 'kevin.ma@medlink.local', 1, '09:00:00', '12:00:00'
    UNION ALL SELECT 'kevin.ma@medlink.local', 2, '13:30:00', '17:00:00'
    UNION ALL SELECT 'kevin.ma@medlink.local', 4, '09:00:00', '12:00:00'
    UNION ALL SELECT 'kevin.ma@medlink.local', 6, '13:30:00', '17:00:00'
    UNION ALL SELECT 'ruby.gao@medlink.local', 3, '13:30:00', '17:00:00'
    UNION ALL SELECT 'ruby.gao@medlink.local', 7, '09:00:00', '12:00:00'
    UNION ALL SELECT 'benjamin.li@medlink.local', 3, '13:30:00', '17:00:00'
    UNION ALL SELECT 'benjamin.li@medlink.local', 5, '09:00:00', '12:00:00'
    UNION ALL SELECT 'chloe.deng@medlink.local', 2, '09:00:00', '12:00:00'
    UNION ALL SELECT 'chloe.deng@medlink.local', 4, '13:30:00', '17:00:00'
    UNION ALL SELECT 'ethan.qiu@medlink.local', 1, '09:00:00', '12:00:00'
    UNION ALL SELECT 'ethan.qiu@medlink.local', 2, '09:00:00', '12:00:00'
    UNION ALL SELECT 'ethan.qiu@medlink.local', 4, '13:30:00', '17:00:00'
    UNION ALL SELECT 'ethan.qiu@medlink.local', 6, '09:00:00', '12:00:00'
    UNION ALL SELECT 'mia.tang@medlink.local', 2, '13:30:00', '17:00:00'
    UNION ALL SELECT 'mia.tang@medlink.local', 4, '09:00:00', '12:00:00'
    UNION ALL SELECT 'mia.tang@medlink.local', 7, '13:30:00', '17:00:00'
    UNION ALL SELECT 'isabella.zhou@medlink.local', 1, '13:30:00', '17:00:00'
    UNION ALL SELECT 'isabella.zhou@medlink.local', 4, '13:30:00', '17:00:00'
    UNION ALL SELECT 'noah.yu@medlink.local', 2, '09:00:00', '12:00:00'
    UNION ALL SELECT 'noah.yu@medlink.local', 3, '13:30:00', '17:00:00'
    UNION ALL SELECT 'noah.yu@medlink.local', 6, '13:30:00', '17:00:00'
    UNION ALL SELECT 'vivian.feng@medlink.local', 1, '09:00:00', '12:00:00'
    UNION ALL SELECT 'vivian.feng@medlink.local', 3, '09:00:00', '12:00:00'
    UNION ALL SELECT 'vivian.feng@medlink.local', 5, '13:30:00', '17:00:00'
    UNION ALL SELECT 'vivian.feng@medlink.local', 7, '09:00:00', '12:00:00'
    UNION ALL SELECT 'lucas.ren@medlink.local', 2, '13:30:00', '17:00:00'
    UNION ALL SELECT 'lucas.ren@medlink.local', 5, '09:00:00', '12:00:00'
    UNION ALL SELECT 'victoria.jiang@medlink.local', 2, '13:30:00', '17:00:00'
    UNION ALL SELECT 'victoria.jiang@medlink.local', 5, '13:30:00', '17:00:00'
    UNION ALL SELECT 'eric.luo@medlink.local', 1, '09:00:00', '12:00:00'
    UNION ALL SELECT 'eric.luo@medlink.local', 4, '09:00:00', '12:00:00'
    UNION ALL SELECT 'eric.luo@medlink.local', 6, '09:00:00', '12:00:00'
    UNION ALL SELECT 'ava.shen@medlink.local', 3, '09:00:00', '12:00:00'
    UNION ALL SELECT 'ava.shen@medlink.local', 7, '13:30:00', '17:00:00'
    UNION ALL SELECT 'nicole.xie@medlink.local', 3, '13:30:00', '17:00:00'
    UNION ALL SELECT 'nicole.xie@medlink.local', 5, '09:00:00', '12:00:00'
    UNION ALL SELECT 'nicole.xie@medlink.local', 7, '09:00:00', '12:00:00'
) seed
JOIN `users` u
    ON u.email = seed.email
JOIN `specialist_profiles` sp
    ON sp.user_id = u.id
LEFT JOIN `availability_recurring_rules` existing
    ON existing.specialist_id = sp.id
   AND existing.day_of_week = seed.day_of_week
   AND existing.start_time = seed.start_time
   AND existing.end_time = seed.end_time
   AND existing.effective_end_date = '2026-05-17'
   AND existing.is_active = 1
WHERE existing.id IS NULL;

DELETE ts
FROM `time_slots` ts
JOIN `specialist_profiles` sp
    ON sp.id = ts.specialist_id
JOIN `users` u
    ON u.id = sp.user_id
LEFT JOIN `bookings` b
    ON b.slot_id = ts.id
WHERE u.email LIKE '%@medlink.local'
  AND ts.recurring_rule_id IS NULL
  AND ts.status = 'AVAILABLE'
  AND b.id IS NULL;

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
    rl.rule_id,
    sd.slot_date,
    ADDTIME(seed.start_time, SEC_TO_TIME(so.offset_index * 1800)),
    ADDTIME(seed.start_time, SEC_TO_TIME((so.offset_index + 1) * 1800)),
    'AVAILABLE'
FROM (
    SELECT 'emily.chen@medlink.local' AS email, 2 AS day_of_week, '09:00:00' AS start_time, '12:00:00' AS end_time
    UNION ALL SELECT 'emily.chen@medlink.local', 4, '09:00:00', '12:00:00'
    UNION ALL SELECT 'ryan.lin@medlink.local', 1, '13:30:00', '17:00:00'
    UNION ALL SELECT 'ryan.lin@medlink.local', 3, '13:30:00', '17:00:00'
    UNION ALL SELECT 'ryan.lin@medlink.local', 5, '13:30:00', '17:00:00'
    UNION ALL SELECT 'sophie.zhao@medlink.local', 1, '09:00:00', '12:00:00'
    UNION ALL SELECT 'sophie.zhao@medlink.local', 2, '13:30:00', '17:00:00'
    UNION ALL SELECT 'sophie.zhao@medlink.local', 4, '13:30:00', '17:00:00'
    UNION ALL SELECT 'sophie.zhao@medlink.local', 6, '09:00:00', '12:00:00'
    UNION ALL SELECT 'jason.xu@medlink.local', 3, '09:00:00', '12:00:00'
    UNION ALL SELECT 'jason.xu@medlink.local', 7, '13:30:00', '17:00:00'
    UNION ALL SELECT 'olivia.wang@medlink.local', 1, '09:00:00', '12:00:00'
    UNION ALL SELECT 'olivia.wang@medlink.local', 5, '09:00:00', '12:00:00'
    UNION ALL SELECT 'daniel.sun@medlink.local', 2, '13:30:00', '17:00:00'
    UNION ALL SELECT 'daniel.sun@medlink.local', 4, '13:30:00', '17:00:00'
    UNION ALL SELECT 'daniel.sun@medlink.local', 6, '09:00:00', '12:00:00'
    UNION ALL SELECT 'grace.liu@medlink.local', 1, '13:30:00', '17:00:00'
    UNION ALL SELECT 'grace.liu@medlink.local', 3, '13:30:00', '17:00:00'
    UNION ALL SELECT 'leo.he@medlink.local', 3, '09:00:00', '12:00:00'
    UNION ALL SELECT 'leo.he@medlink.local', 5, '13:30:00', '17:00:00'
    UNION ALL SELECT 'leo.he@medlink.local', 7, '09:00:00', '12:00:00'
    UNION ALL SELECT 'michael.zhang@medlink.local', 2, '09:00:00', '12:00:00'
    UNION ALL SELECT 'michael.zhang@medlink.local', 6, '09:00:00', '12:00:00'
    UNION ALL SELECT 'hannah.wu@medlink.local', 1, '13:30:00', '17:00:00'
    UNION ALL SELECT 'hannah.wu@medlink.local', 3, '09:00:00', '12:00:00'
    UNION ALL SELECT 'hannah.wu@medlink.local', 5, '09:00:00', '12:00:00'
    UNION ALL SELECT 'kevin.ma@medlink.local', 1, '09:00:00', '12:00:00'
    UNION ALL SELECT 'kevin.ma@medlink.local', 2, '13:30:00', '17:00:00'
    UNION ALL SELECT 'kevin.ma@medlink.local', 4, '09:00:00', '12:00:00'
    UNION ALL SELECT 'kevin.ma@medlink.local', 6, '13:30:00', '17:00:00'
    UNION ALL SELECT 'ruby.gao@medlink.local', 3, '13:30:00', '17:00:00'
    UNION ALL SELECT 'ruby.gao@medlink.local', 7, '09:00:00', '12:00:00'
    UNION ALL SELECT 'benjamin.li@medlink.local', 3, '13:30:00', '17:00:00'
    UNION ALL SELECT 'benjamin.li@medlink.local', 5, '09:00:00', '12:00:00'
    UNION ALL SELECT 'chloe.deng@medlink.local', 2, '09:00:00', '12:00:00'
    UNION ALL SELECT 'chloe.deng@medlink.local', 4, '13:30:00', '17:00:00'
    UNION ALL SELECT 'ethan.qiu@medlink.local', 1, '09:00:00', '12:00:00'
    UNION ALL SELECT 'ethan.qiu@medlink.local', 2, '09:00:00', '12:00:00'
    UNION ALL SELECT 'ethan.qiu@medlink.local', 4, '13:30:00', '17:00:00'
    UNION ALL SELECT 'ethan.qiu@medlink.local', 6, '09:00:00', '12:00:00'
    UNION ALL SELECT 'mia.tang@medlink.local', 2, '13:30:00', '17:00:00'
    UNION ALL SELECT 'mia.tang@medlink.local', 4, '09:00:00', '12:00:00'
    UNION ALL SELECT 'mia.tang@medlink.local', 7, '13:30:00', '17:00:00'
    UNION ALL SELECT 'isabella.zhou@medlink.local', 1, '13:30:00', '17:00:00'
    UNION ALL SELECT 'isabella.zhou@medlink.local', 4, '13:30:00', '17:00:00'
    UNION ALL SELECT 'noah.yu@medlink.local', 2, '09:00:00', '12:00:00'
    UNION ALL SELECT 'noah.yu@medlink.local', 3, '13:30:00', '17:00:00'
    UNION ALL SELECT 'noah.yu@medlink.local', 6, '13:30:00', '17:00:00'
    UNION ALL SELECT 'vivian.feng@medlink.local', 1, '09:00:00', '12:00:00'
    UNION ALL SELECT 'vivian.feng@medlink.local', 3, '09:00:00', '12:00:00'
    UNION ALL SELECT 'vivian.feng@medlink.local', 5, '13:30:00', '17:00:00'
    UNION ALL SELECT 'vivian.feng@medlink.local', 7, '09:00:00', '12:00:00'
    UNION ALL SELECT 'lucas.ren@medlink.local', 2, '13:30:00', '17:00:00'
    UNION ALL SELECT 'lucas.ren@medlink.local', 5, '09:00:00', '12:00:00'
    UNION ALL SELECT 'victoria.jiang@medlink.local', 2, '13:30:00', '17:00:00'
    UNION ALL SELECT 'victoria.jiang@medlink.local', 5, '13:30:00', '17:00:00'
    UNION ALL SELECT 'eric.luo@medlink.local', 1, '09:00:00', '12:00:00'
    UNION ALL SELECT 'eric.luo@medlink.local', 4, '09:00:00', '12:00:00'
    UNION ALL SELECT 'eric.luo@medlink.local', 6, '09:00:00', '12:00:00'
    UNION ALL SELECT 'ava.shen@medlink.local', 3, '09:00:00', '12:00:00'
    UNION ALL SELECT 'ava.shen@medlink.local', 7, '13:30:00', '17:00:00'
    UNION ALL SELECT 'nicole.xie@medlink.local', 3, '13:30:00', '17:00:00'
    UNION ALL SELECT 'nicole.xie@medlink.local', 5, '09:00:00', '12:00:00'
    UNION ALL SELECT 'nicole.xie@medlink.local', 7, '09:00:00', '12:00:00'
) seed
JOIN `users` u
    ON u.email = seed.email
JOIN `specialist_profiles` sp
    ON sp.user_id = u.id
JOIN (
    SELECT
        MIN(arr.id) AS rule_id,
        arr.specialist_id,
        arr.day_of_week,
        arr.start_time,
        arr.end_time,
        arr.effective_end_date
    FROM `availability_recurring_rules` arr
    WHERE arr.effective_end_date = '2026-05-17'
      AND arr.is_active = 1
    GROUP BY
        arr.specialist_id,
        arr.day_of_week,
        arr.start_time,
        arr.end_time,
        arr.effective_end_date
) rl
    ON rl.specialist_id = sp.id
   AND rl.day_of_week = seed.day_of_week
   AND rl.start_time = seed.start_time
   AND rl.end_time = seed.end_time
   AND rl.effective_end_date = '2026-05-17'
JOIN (
    SELECT DATE('2026-04-20') AS slot_date
    UNION ALL SELECT DATE('2026-04-21')
    UNION ALL SELECT DATE('2026-04-22')
    UNION ALL SELECT DATE('2026-04-23')
    UNION ALL SELECT DATE('2026-04-24')
    UNION ALL SELECT DATE('2026-04-25')
    UNION ALL SELECT DATE('2026-04-26')
    UNION ALL SELECT DATE('2026-04-27')
    UNION ALL SELECT DATE('2026-04-28')
    UNION ALL SELECT DATE('2026-04-29')
    UNION ALL SELECT DATE('2026-04-30')
    UNION ALL SELECT DATE('2026-05-01')
    UNION ALL SELECT DATE('2026-05-02')
    UNION ALL SELECT DATE('2026-05-03')
    UNION ALL SELECT DATE('2026-05-04')
    UNION ALL SELECT DATE('2026-05-05')
    UNION ALL SELECT DATE('2026-05-06')
    UNION ALL SELECT DATE('2026-05-07')
    UNION ALL SELECT DATE('2026-05-08')
    UNION ALL SELECT DATE('2026-05-09')
    UNION ALL SELECT DATE('2026-05-10')
    UNION ALL SELECT DATE('2026-05-11')
    UNION ALL SELECT DATE('2026-05-12')
    UNION ALL SELECT DATE('2026-05-13')
    UNION ALL SELECT DATE('2026-05-14')
    UNION ALL SELECT DATE('2026-05-15')
    UNION ALL SELECT DATE('2026-05-16')
    UNION ALL SELECT DATE('2026-05-17')
) sd
    ON WEEKDAY(sd.slot_date) + 1 = seed.day_of_week
JOIN (
    SELECT 0 AS offset_index
    UNION ALL SELECT 1
    UNION ALL SELECT 2
    UNION ALL SELECT 3
    UNION ALL SELECT 4
    UNION ALL SELECT 5
    UNION ALL SELECT 6
) so
    ON ADDTIME(seed.start_time, SEC_TO_TIME((so.offset_index + 1) * 1800)) <= seed.end_time
LEFT JOIN `time_slots` existing
    ON existing.specialist_id = sp.id
   AND existing.slot_date = sd.slot_date
   AND existing.start_time = ADDTIME(seed.start_time, SEC_TO_TIME(so.offset_index * 1800))
   AND existing.end_time = ADDTIME(seed.start_time, SEC_TO_TIME((so.offset_index + 1) * 1800))
WHERE existing.id IS NULL;
