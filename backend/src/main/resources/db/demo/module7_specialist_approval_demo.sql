USE cpt202_consultancy;

SET @specialist_email := 'ryan.lin@medlink.local';
SET @specialist_id := (
    SELECT sp.id
    FROM specialist_profiles sp
    JOIN users u ON u.id = sp.user_id
    WHERE u.email = @specialist_email
    LIMIT 1
);

DROP TEMPORARY TABLE IF EXISTS temp_module7_demo_slots;
CREATE TEMPORARY TABLE temp_module7_demo_slots AS
SELECT b.slot_id
FROM bookings b
WHERE b.topic LIKE '[Module7 Demo]%'
  AND b.slot_id IS NOT NULL;

DELETE FROM refund_penalties
WHERE booking_id IN (
    SELECT id
    FROM bookings
    WHERE topic LIKE '[Module7 Demo]%'
);

DELETE FROM bookings
WHERE topic LIKE '[Module7 Demo]%';

DELETE FROM time_slots
WHERE id IN (SELECT slot_id FROM temp_module7_demo_slots);

DROP TEMPORARY TABLE temp_module7_demo_slots;

INSERT IGNORE INTO users (
    email,
    password_hash,
    role,
    status,
    full_name,
    phone_number,
    login_fail_count,
    lock_time,
    created_at,
    updated_at
)
VALUES
('module7.anna@example.com', 'demo', 'CUSTOMER', 'ACTIVE', 'Anna Demo', '10000000001', 0, NOW(), NOW(), NOW()),
('module7.brian@example.com', 'demo', 'CUSTOMER', 'ACTIVE', 'Brian Demo', '10000000002', 0, NOW(), NOW(), NOW()),
('module7.claire@example.com', 'demo', 'CUSTOMER', 'ACTIVE', 'Claire Demo', '10000000003', 0, NOW(), NOW(), NOW()),
('module7.david@example.com', 'demo', 'CUSTOMER', 'ACTIVE', 'David Demo', '10000000004', 0, NOW(), NOW(), NOW()),
('module7.ella@example.com', 'demo', 'CUSTOMER', 'ACTIVE', 'Ella Demo', '10000000005', 0, NOW(), NOW(), NOW()),
('module7.felix@example.com', 'demo', 'CUSTOMER', 'ACTIVE', 'Felix Demo', '10000000006', 0, NOW(), NOW(), NOW()),
('module7.grace@example.com', 'demo', 'CUSTOMER', 'ACTIVE', 'Grace Demo', '10000000007', 0, NOW(), NOW(), NOW()),
('module7.henry@example.com', 'demo', 'CUSTOMER', 'ACTIVE', 'Henry Demo', '10000000008', 0, NOW(), NOW(), NOW()),
('module7.iris@example.com', 'demo', 'CUSTOMER', 'ACTIVE', 'Iris Demo', '10000000009', 0, NOW(), NOW(), NOW()),
('module7.jack@example.com', 'demo', 'CUSTOMER', 'ACTIVE', 'Jack Demo', '10000000010', 0, NOW(), NOW(), NOW()),
('module7.kate@example.com', 'demo', 'CUSTOMER', 'ACTIVE', 'Kate Demo', '10000000011', 0, NOW(), NOW(), NOW()),
('module7.louis@example.com', 'demo', 'CUSTOMER', 'ACTIVE', 'Louis Demo', '10000000012', 0, NOW(), NOW(), NOW()),
('module7.mia@example.com', 'demo', 'CUSTOMER', 'ACTIVE', 'Mia Demo', '10000000013', 0, NOW(), NOW(), NOW()),
('module7.noah@example.com', 'demo', 'CUSTOMER', 'ACTIVE', 'Noah Demo', '10000000014', 0, NOW(), NOW(), NOW()),
('module7.olivia@example.com', 'demo', 'CUSTOMER', 'ACTIVE', 'Olivia Demo', '10000000015', 0, NOW(), NOW(), NOW()),
('module7.peter@example.com', 'demo', 'CUSTOMER', 'ACTIVE', 'Peter Demo', '10000000016', 0, NOW(), NOW(), NOW());

INSERT INTO time_slots (specialist_id, recurring_rule_id, slot_date, start_time, end_time, status, created_at, updated_at)
VALUES
(@specialist_id, NULL, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '09:00:00', '10:00:00', 'BOOKED', NOW(), NOW()),
(@specialist_id, NULL, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '10:00:00', '11:00:00', 'BOOKED', NOW(), NOW()),
(@specialist_id, NULL, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '11:00:00', '12:00:00', 'BOOKED', NOW(), NOW()),
(@specialist_id, NULL, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '14:00:00', '15:00:00', 'BOOKED', NOW(), NOW()),
(@specialist_id, NULL, DATE_ADD(CURDATE(), INTERVAL 3 DAY), '09:00:00', '10:00:00', 'BOOKED', NOW(), NOW()),
(@specialist_id, NULL, DATE_ADD(CURDATE(), INTERVAL 3 DAY), '10:00:00', '11:00:00', 'BOOKED', NOW(), NOW()),
(@specialist_id, NULL, DATE_ADD(CURDATE(), INTERVAL 3 DAY), '11:00:00', '12:00:00', 'BOOKED', NOW(), NOW()),
(@specialist_id, NULL, DATE_ADD(CURDATE(), INTERVAL 3 DAY), '14:00:00', '15:00:00', 'BOOKED', NOW(), NOW()),
(@specialist_id, NULL, DATE_ADD(CURDATE(), INTERVAL 4 DAY), '09:00:00', '10:00:00', 'BOOKED', NOW(), NOW()),
(@specialist_id, NULL, DATE_ADD(CURDATE(), INTERVAL 4 DAY), '10:00:00', '11:00:00', 'BOOKED', NOW(), NOW()),
(@specialist_id, NULL, DATE_ADD(CURDATE(), INTERVAL 4 DAY), '11:00:00', '12:00:00', 'BOOKED', NOW(), NOW()),
(@specialist_id, NULL, DATE_ADD(CURDATE(), INTERVAL 4 DAY), '14:00:00', '15:00:00', 'BOOKED', NOW(), NOW()),
(@specialist_id, NULL, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '09:00:00', '10:00:00', 'BOOKED', NOW(), NOW()),
(@specialist_id, NULL, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '10:00:00', '11:00:00', 'BOOKED', NOW(), NOW()),
(@specialist_id, NULL, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '11:00:00', '12:00:00', 'BOOKED', NOW(), NOW()),
(@specialist_id, NULL, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '14:00:00', '15:00:00', 'BOOKED', NOW(), NOW()),
(@specialist_id, NULL, DATE_ADD(CURDATE(), INTERVAL 6 DAY), '09:00:00', '10:00:00', 'BOOKED', NOW(), NOW()),
(@specialist_id, NULL, DATE_ADD(CURDATE(), INTERVAL 6 DAY), '10:00:00', '11:00:00', 'BOOKED', NOW(), NOW()),
(@specialist_id, NULL, DATE_ADD(CURDATE(), INTERVAL 6 DAY), '11:00:00', '12:00:00', 'BOOKED', NOW(), NOW());

SET @slot0 := LAST_INSERT_ID();

INSERT INTO bookings (
    customer_id,
    specialist_id,
    slot_id,
    status,
    price,
    topic,
    customer_notes,
    parent_booking_id,
    rejection_reason,
    decision_time,
    cancelled_by,
    cancel_reason,
    change_type,
    refund_status,
    created_at,
    updated_at
)
VALUES
((SELECT id FROM users WHERE email = 'module7.anna@example.com'), @specialist_id, @slot0 + 0, 'PENDING', 220.00, '[Module7 Demo] Urgent chest pain follow-up', 'Customer reports recurring discomfort and needs fast approval.', NULL, NULL, NULL, NULL, NULL, NULL, 'NONE', NOW() - INTERVAL 23 HOUR - INTERVAL 15 MINUTE, NOW()),
((SELECT id FROM users WHERE email = 'module7.brian@example.com'), @specialist_id, @slot0 + 1, 'PENDING', 180.00, '[Module7 Demo] Urgent blood pressure review', 'Uploaded latest blood pressure readings.', NULL, NULL, NULL, NULL, NULL, NULL, 'NONE', NOW() - INTERVAL 23 HOUR - INTERVAL 25 MINUTE, NOW()),
((SELECT id FROM users WHERE email = 'module7.claire@example.com'), @specialist_id, @slot0 + 2, 'PENDING', 260.00, '[Module7 Demo] Urgent medication side effects', 'Needs advice before taking next dose.', NULL, NULL, NULL, NULL, NULL, NULL, 'NONE', NOW() - INTERVAL 23 HOUR - INTERVAL 35 MINUTE, NOW()),
((SELECT id FROM users WHERE email = 'module7.david@example.com'), @specialist_id, @slot0 + 3, 'PENDING', 200.00, '[Module7 Demo] Urgent post-surgery symptom check', 'Concerned about swelling after procedure.', NULL, NULL, NULL, NULL, NULL, NULL, 'NONE', NOW() - INTERVAL 23 HOUR - INTERVAL 45 MINUTE, NOW()),
((SELECT id FROM users WHERE email = 'module7.ella@example.com'), @specialist_id, @slot0 + 4, 'PENDING', 220.00, '[Module7 Demo] Soon diabetes diet consultation', 'Would like nutrition advice before upcoming travel.', NULL, NULL, NULL, NULL, NULL, NULL, 'NONE', NOW() - INTERVAL 22 HOUR, NOW()),
((SELECT id FROM users WHERE email = 'module7.felix@example.com'), @specialist_id, @slot0 + 5, 'PENDING', 210.00, '[Module7 Demo] Soon sleep disorder assessment', 'Recent insomnia worsening over two weeks.', NULL, NULL, NULL, NULL, NULL, NULL, 'NONE', NOW() - INTERVAL 21 HOUR - INTERVAL 15 MINUTE, NOW()),
((SELECT id FROM users WHERE email = 'module7.grace@example.com'), @specialist_id, @slot0 + 6, 'PENDING', 190.00, '[Module7 Demo] Soon allergy treatment review', 'Requests review of seasonal allergy plan.', NULL, NULL, NULL, NULL, NULL, NULL, 'NONE', NOW() - INTERVAL 20 HOUR - INTERVAL 30 MINUTE, NOW()),
((SELECT id FROM users WHERE email = 'module7.henry@example.com'), @specialist_id, @slot0 + 7, 'PENDING', 230.00, '[Module7 Demo] Soon anxiety consultation', 'Needs specialist advice before work presentation.', NULL, NULL, NULL, NULL, NULL, NULL, 'NONE', NOW() - INTERVAL 19 HOUR - INTERVAL 20 MINUTE, NOW()),
((SELECT id FROM users WHERE email = 'module7.iris@example.com'), @specialist_id, @slot0 + 8, 'PENDING', 240.00, '[Module7 Demo] Normal long-term migraine review', 'Prepared symptom diary for discussion.', NULL, NULL, NULL, NULL, NULL, NULL, 'NONE', NOW() - INTERVAL 12 HOUR, NOW()),
((SELECT id FROM users WHERE email = 'module7.jack@example.com'), @specialist_id, @slot0 + 9, 'PENDING', 160.00, '[Module7 Demo] Normal skin rash assessment', 'Photos available in customer notes.', NULL, NULL, NULL, NULL, NULL, NULL, 'NONE', NOW() - INTERVAL 10 HOUR, NOW()),
((SELECT id FROM users WHERE email = 'module7.kate@example.com'), @specialist_id, @slot0 + 10, 'PENDING', 280.00, '[Module7 Demo] Normal thyroid report explanation', 'Wants help understanding lab results.', NULL, NULL, NULL, NULL, NULL, NULL, 'NONE', NOW() - INTERVAL 8 HOUR, NOW()),
((SELECT id FROM users WHERE email = 'module7.louis@example.com'), @specialist_id, @slot0 + 11, 'PENDING', 210.00, '[Module7 Demo] Normal sports injury recovery', 'Asks about returning to running.', NULL, NULL, NULL, NULL, NULL, NULL, 'NONE', NOW() - INTERVAL 6 HOUR, NOW()),
((SELECT id FROM users WHERE email = 'module7.mia@example.com'), @specialist_id, @slot0 + 12, 'PENDING', 220.00, '[Module7 Demo] Normal pregnancy nutrition advice', 'Looking for safe supplement guidance.', NULL, NULL, NULL, NULL, NULL, NULL, 'NONE', NOW() - INTERVAL 4 HOUR, NOW()),
((SELECT id FROM users WHERE email = 'module7.noah@example.com'), @specialist_id, @slot0 + 13, 'PENDING', 200.00, '[Module7 Demo] Normal vaccination question', 'Asks about timing between vaccines.', NULL, NULL, NULL, NULL, NULL, NULL, 'NONE', NOW() - INTERVAL 2 HOUR, NOW()),
((SELECT id FROM users WHERE email = 'module7.olivia@example.com'), @specialist_id, @slot0 + 14, 'CONFIRMED', 250.00, '[Module7 Demo] Approved digestive health review', 'Already approved for history demonstration.', NULL, NULL, NOW() - INTERVAL 1 HOUR, NULL, NULL, NULL, 'NONE', NOW() - INTERVAL 5 HOUR, NOW()),
((SELECT id FROM users WHERE email = 'module7.peter@example.com'), @specialist_id, @slot0 + 15, 'CANCELLED', 230.00, '[Module7 Demo] Specialist rejected headache second opinion', 'Used to demonstrate manual rejection and refund status.', NULL, 'Schedule conflict with emergency case.', NOW() - INTERVAL 40 MINUTE, 'SPECIALIST_MANUAL', 'Schedule conflict with emergency case.', 'REJECT', 'PENDING', NOW() - INTERVAL 3 HOUR, NOW()),
((SELECT id FROM users WHERE email = 'module7.anna@example.com'), @specialist_id, @slot0 + 16, 'CANCELLED', 205.00, '[Module7 Demo] Timeout rejected dermatology follow-up', 'Used to demonstrate system timeout rejection and refund status.', NULL, 'Request expired before specialist decision.', NOW() - INTERVAL 20 MINUTE, 'SYSTEM_TIMEOUT', 'Request expired before specialist decision.', 'SYSTEM_TIMEOUT_CANCEL', 'PENDING', NOW() - INTERVAL 24 HOUR - INTERVAL 10 MINUTE, NOW()),
((SELECT id FROM users WHERE email = 'module7.brian@example.com'), @specialist_id, @slot0 + 17, 'PENDING', 260.00, '[Module7 Demo] Normal lab report clarification', 'Wants a quick explanation of recent blood test results.', NULL, NULL, NULL, NULL, NULL, NULL, 'NONE', NOW() - INTERVAL 1 HOUR, NOW()),
((SELECT id FROM users WHERE email = 'module7.claire@example.com'), @specialist_id, @slot0 + 18, 'PENDING', 195.00, '[Module7 Demo] Normal allergy medicine adjustment', 'Asks whether current prescription should be adjusted.', NULL, NULL, NULL, NULL, NULL, NULL, 'NONE', NOW() - INTERVAL 20 MINUTE, NOW());

INSERT INTO refund_penalties (booking_id, refund_amount, penalty_amount, calculation_rule, status, processed_at, created_at)
SELECT b.id, b.price, 0.00, 'SPECIALIST_REJECT_FULL_REFUND', 'PENDING', NOW(), NOW()
FROM bookings b
WHERE b.topic = '[Module7 Demo] Specialist rejected headache second opinion';

INSERT INTO refund_penalties (booking_id, refund_amount, penalty_amount, calculation_rule, status, processed_at, created_at)
SELECT b.id, b.price, 0.00, 'SYSTEM_TIMEOUT_FULL_REFUND', 'PENDING', NOW(), NOW()
FROM bookings b
WHERE b.topic = '[Module7 Demo] Timeout rejected dermatology follow-up';

SELECT
    b.id,
    u.full_name AS customer_name,
    b.topic,
    b.status,
    b.refund_status,
    b.created_at,
    DATE_ADD(b.created_at, INTERVAL 1440 MINUTE) AS auto_reject_at
FROM bookings b
JOIN users u ON u.id = b.customer_id
WHERE b.specialist_id = @specialist_id
  AND b.topic LIKE '[Module7 Demo]%'
ORDER BY b.created_at ASC;
