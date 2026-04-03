INSERT INTO time_slots (specialist_id, slot_date, start_time, end_time, status, created_at, updated_at)
SELECT
    sp.id,
    DATE_ADD(CURDATE(), INTERVAL d.day_offset DAY),
    t.start_time,
    t.end_time,
    'AVAILABLE',
    NOW(),
    NOW()
FROM specialist_profiles sp
CROSS JOIN (
    SELECT 0 AS day_offset
    UNION ALL SELECT 1
    UNION ALL SELECT 2
    UNION ALL SELECT 3
    UNION ALL SELECT 4
) d
CROSS JOIN (
    SELECT '09:00:00' AS start_time, '09:45:00' AS end_time
    UNION ALL SELECT '14:00:00', '14:45:00'
    UNION ALL SELECT '16:00:00', '16:45:00'
) t
WHERE sp.status = 'ACTIVE'
  AND NOT EXISTS (
    SELECT 1
    FROM time_slots ts
    WHERE ts.specialist_id = sp.id
      AND ts.slot_date = DATE_ADD(CURDATE(), INTERVAL d.day_offset DAY)
      AND ts.start_time = t.start_time
      AND ts.end_time = t.end_time
  );
