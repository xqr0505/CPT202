INSERT INTO time_slots (id, specialist_id, slot_date, start_time, end_time, status, created_at, updated_at)
SELECT 910001, 201, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00', '09:45:00', 'AVAILABLE', NOW(), NOW()
FROM DUAL
WHERE EXISTS (SELECT 1 FROM specialist_profiles WHERE id = 201)
  AND NOT EXISTS (
    SELECT 1 FROM time_slots
    WHERE specialist_id = 201
      AND slot_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY)
      AND start_time = '09:00:00'
      AND end_time = '09:45:00'
  );

INSERT INTO time_slots (id, specialist_id, slot_date, start_time, end_time, status, created_at, updated_at)
SELECT 910002, 201, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '14:00:00', '14:45:00', 'AVAILABLE', NOW(), NOW()
FROM DUAL
WHERE EXISTS (SELECT 1 FROM specialist_profiles WHERE id = 201)
  AND NOT EXISTS (
    SELECT 1 FROM time_slots
    WHERE specialist_id = 201
      AND slot_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY)
      AND start_time = '14:00:00'
      AND end_time = '14:45:00'
  );

INSERT INTO time_slots (id, specialist_id, slot_date, start_time, end_time, status, created_at, updated_at)
SELECT 910003, 201, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '10:00:00', '10:45:00', 'AVAILABLE', NOW(), NOW()
FROM DUAL
WHERE EXISTS (SELECT 1 FROM specialist_profiles WHERE id = 201)
  AND NOT EXISTS (
    SELECT 1 FROM time_slots
    WHERE specialist_id = 201
      AND slot_date = DATE_ADD(CURDATE(), INTERVAL 2 DAY)
      AND start_time = '10:00:00'
      AND end_time = '10:45:00'
  );

INSERT INTO time_slots (id, specialist_id, slot_date, start_time, end_time, status, created_at, updated_at)
SELECT 910004, 201, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '16:00:00', '16:45:00', 'AVAILABLE', NOW(), NOW()
FROM DUAL
WHERE EXISTS (SELECT 1 FROM specialist_profiles WHERE id = 201)
  AND NOT EXISTS (
    SELECT 1 FROM time_slots
    WHERE specialist_id = 201
      AND slot_date = DATE_ADD(CURDATE(), INTERVAL 2 DAY)
      AND start_time = '16:00:00'
      AND end_time = '16:45:00'
  );

INSERT INTO time_slots (id, specialist_id, slot_date, start_time, end_time, status, created_at, updated_at)
SELECT 910005, 202, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00:00', '10:45:00', 'AVAILABLE', NOW(), NOW()
FROM DUAL
WHERE EXISTS (SELECT 1 FROM specialist_profiles WHERE id = 202)
  AND NOT EXISTS (
    SELECT 1 FROM time_slots
    WHERE specialist_id = 202
      AND slot_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY)
      AND start_time = '10:00:00'
      AND end_time = '10:45:00'
  );

INSERT INTO time_slots (id, specialist_id, slot_date, start_time, end_time, status, created_at, updated_at)
SELECT 910006, 202, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '16:00:00', '16:45:00', 'AVAILABLE', NOW(), NOW()
FROM DUAL
WHERE EXISTS (SELECT 1 FROM specialist_profiles WHERE id = 202)
  AND NOT EXISTS (
    SELECT 1 FROM time_slots
    WHERE specialist_id = 202
      AND slot_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY)
      AND start_time = '16:00:00'
      AND end_time = '16:45:00'
  );

INSERT INTO time_slots (id, specialist_id, slot_date, start_time, end_time, status, created_at, updated_at)
SELECT 910007, 202, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '11:00:00', '11:45:00', 'AVAILABLE', NOW(), NOW()
FROM DUAL
WHERE EXISTS (SELECT 1 FROM specialist_profiles WHERE id = 202)
  AND NOT EXISTS (
    SELECT 1 FROM time_slots
    WHERE specialist_id = 202
      AND slot_date = DATE_ADD(CURDATE(), INTERVAL 2 DAY)
      AND start_time = '11:00:00'
      AND end_time = '11:45:00'
  );

INSERT INTO time_slots (id, specialist_id, slot_date, start_time, end_time, status, created_at, updated_at)
SELECT 910008, 202, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '15:00:00', '15:45:00', 'AVAILABLE', NOW(), NOW()
FROM DUAL
WHERE EXISTS (SELECT 1 FROM specialist_profiles WHERE id = 202)
  AND NOT EXISTS (
    SELECT 1 FROM time_slots
    WHERE specialist_id = 202
      AND slot_date = DATE_ADD(CURDATE(), INTERVAL 2 DAY)
      AND start_time = '15:00:00'
      AND end_time = '15:45:00'
  );

INSERT INTO time_slots (id, specialist_id, slot_date, start_time, end_time, status, created_at, updated_at)
SELECT 910009, 203, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '11:00:00', '11:45:00', 'AVAILABLE', NOW(), NOW()
FROM DUAL
WHERE EXISTS (SELECT 1 FROM specialist_profiles WHERE id = 203)
  AND NOT EXISTS (
    SELECT 1 FROM time_slots
    WHERE specialist_id = 203
      AND slot_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY)
      AND start_time = '11:00:00'
      AND end_time = '11:45:00'
  );

INSERT INTO time_slots (id, specialist_id, slot_date, start_time, end_time, status, created_at, updated_at)
SELECT 910010, 203, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '15:00:00', '15:45:00', 'AVAILABLE', NOW(), NOW()
FROM DUAL
WHERE EXISTS (SELECT 1 FROM specialist_profiles WHERE id = 203)
  AND NOT EXISTS (
    SELECT 1 FROM time_slots
    WHERE specialist_id = 203
      AND slot_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY)
      AND start_time = '15:00:00'
      AND end_time = '15:45:00'
  );

INSERT INTO time_slots (id, specialist_id, slot_date, start_time, end_time, status, created_at, updated_at)
SELECT 910011, 203, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '09:30:00', '10:15:00', 'AVAILABLE', NOW(), NOW()
FROM DUAL
WHERE EXISTS (SELECT 1 FROM specialist_profiles WHERE id = 203)
  AND NOT EXISTS (
    SELECT 1 FROM time_slots
    WHERE specialist_id = 203
      AND slot_date = DATE_ADD(CURDATE(), INTERVAL 2 DAY)
      AND start_time = '09:30:00'
      AND end_time = '10:15:00'
  );

INSERT INTO time_slots (id, specialist_id, slot_date, start_time, end_time, status, created_at, updated_at)
SELECT 910012, 203, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '14:30:00', '15:15:00', 'AVAILABLE', NOW(), NOW()
FROM DUAL
WHERE EXISTS (SELECT 1 FROM specialist_profiles WHERE id = 203)
  AND NOT EXISTS (
    SELECT 1 FROM time_slots
    WHERE specialist_id = 203
      AND slot_date = DATE_ADD(CURDATE(), INTERVAL 2 DAY)
      AND start_time = '14:30:00'
      AND end_time = '15:15:00'
  );
