START TRANSACTION;


-- password:Test12345
INSERT INTO `users` (`email`, `password_hash`, `role`, `status`, `full_name`, `created_at`) 
VALUES ('test.user@expertlink.com', '$2a$10$1HK2O2.daVRPTmEe1DbIdOT76IkQAk52Hfqxn8.uGunRWFijRv/1q', 'CUSTOMER', 'ACTIVE', 'Test User', NOW());

SET @cust_id = LAST_INSERT_ID();


-- --- 2025.12  ---
INSERT INTO `time_slots` (`specialist_id`, `slot_date`, `start_time`, `end_time`, `status`) VALUES (4, '2025-12-15', '10:00:00', '11:00:00', 'BOOKED');
INSERT INTO `bookings` (`customer_id`, `specialist_id`, `slot_id`, `status`, `price`, `topic`) 
VALUES (@cust_id, 4, LAST_INSERT_ID(), 'COMPLETED', 130.00, 'Historical Follow-up');

-- --- 2026.01 ---
-- 1
INSERT INTO `time_slots` (`specialist_id`, `slot_date`, `start_time`, `end_time`, `status`) VALUES (5, '2026-01-05', '09:00:00', '10:00:00', 'BOOKED');
INSERT INTO `bookings` (`customer_id`, `specialist_id`, `slot_id`, `status`, `price`, `topic`) VALUES (@cust_id, 5, LAST_INSERT_ID(), 'COMPLETED', 255.00, 'Routine Checkup');
-- 2
INSERT INTO `time_slots` (`specialist_id`, `slot_date`, `start_time`, `end_time`, `status`) VALUES (9, '2026-01-10', '14:00:00', '15:00:00', 'BOOKED');
INSERT INTO `bookings` (`customer_id`, `specialist_id`, `slot_id`, `status`, `price`, `topic`) VALUES (@cust_id, 9, LAST_INSERT_ID(), 'COMPLETED', 285.00, 'Initial Consultation');
-- 3
INSERT INTO `time_slots` (`specialist_id`, `slot_date`, `start_time`, `end_time`, `status`) VALUES (12, '2026-01-15', '11:00:00', '12:00:00', 'BOOKED');
INSERT INTO `bookings` (`customer_id`, `specialist_id`, `slot_id`, `status`, `price`, `topic`) VALUES (@cust_id, 12, LAST_INSERT_ID(), 'COMPLETED', 135.00, 'Symptom Review');
-- 4
INSERT INTO `time_slots` (`specialist_id`, `slot_date`, `start_time`, `end_time`, `status`) VALUES (4, '2026-01-20', '10:00:00', '11:00:00', 'BOOKED');
INSERT INTO `bookings` (`customer_id`, `specialist_id`, `slot_id`, `status`, `price`, `topic`) VALUES (@cust_id, 4, LAST_INSERT_ID(), 'COMPLETED', 130.00, 'Medication Adjustment');
-- 5
INSERT INTO `time_slots` (`specialist_id`, `slot_date`, `start_time`, `end_time`, `status`) VALUES (5, '2026-01-25', '16:00:00', '17:00:00', 'BOOKED');
INSERT INTO `bookings` (`customer_id`, `specialist_id`, `slot_id`, `status`, `price`, `topic`) VALUES (@cust_id, 5, LAST_INSERT_ID(), 'COMPLETED', 255.00, 'Skin Assessment');

-- --- 2026.02 ---
INSERT INTO `time_slots` (`specialist_id`, `slot_date`, `start_time`, `end_time`, `status`) VALUES (9, '2026-02-05', '09:30:00', '10:30:00', 'BOOKED');
INSERT INTO `bookings` (`customer_id`, `specialist_id`, `slot_id`, `status`, `price`, `topic`) VALUES (@cust_id, 9, LAST_INSERT_ID(), 'COMPLETED', 285.00, 'Monthly Review');
INSERT INTO `time_slots` (`specialist_id`, `slot_date`, `start_time`, `end_time`, `status`) VALUES (12, '2026-02-20', '15:00:00', '16:00:00', 'BOOKED');
INSERT INTO `bookings` (`customer_id`, `specialist_id`, `slot_id`, `status`, `price`, `topic`) VALUES (@cust_id, 12, LAST_INSERT_ID(), 'COMPLETED', 135.00, 'Follow-up');

-- --- 2026.03 ---
INSERT INTO `time_slots` (`specialist_id`, `slot_date`, `start_time`, `end_time`, `status`) VALUES (4, '2026-03-05', '10:00:00', '11:00:00', 'BOOKED');
INSERT INTO `bookings` (`customer_id`, `specialist_id`, `slot_id`, `status`, `price`, `topic`) VALUES (@cust_id, 4, LAST_INSERT_ID(), 'COMPLETED', 130.00, 'Consultation');
INSERT INTO `time_slots` (`specialist_id`, `slot_date`, `start_time`, `end_time`, `status`) VALUES (5, '2026-03-15', '11:00:00', '12:00:00', 'BOOKED');
INSERT INTO `bookings` (`customer_id`, `specialist_id`, `slot_id`, `status`, `price`, `topic`) VALUES (@cust_id, 5, LAST_INSERT_ID(), 'COMPLETED', 255.00, 'Checkup');
INSERT INTO `time_slots` (`specialist_id`, `slot_date`, `start_time`, `end_time`, `status`) VALUES (9, '2026-03-25', '14:00:00', '15:00:00', 'BOOKED');
INSERT INTO `bookings` (`customer_id`, `specialist_id`, `slot_id`, `status`, `price`, `topic`) VALUES (@cust_id, 9, LAST_INSERT_ID(), 'COMPLETED', 285.00, 'Advice');

-- --- 2026.04 ---
INSERT INTO `time_slots` (`specialist_id`, `slot_date`, `start_time`, `end_time`, `status`) VALUES (12, '2026-04-05', '10:00:00', '11:00:00', 'BOOKED');
INSERT INTO `bookings` (`customer_id`, `specialist_id`, `slot_id`, `status`, `price`, `topic`) VALUES (@cust_id, 12, LAST_INSERT_ID(), 'COMPLETED', 135.00, 'General Inquiry');
INSERT INTO `time_slots` (`specialist_id`, `slot_date`, `start_time`, `end_time`, `status`) VALUES (4, '2026-04-10', '16:00:00', '17:00:00', 'AVAILABLE');
INSERT INTO `bookings` (`customer_id`, `specialist_id`, `slot_id`, `status`, `price`, `topic`, `cancelled_by`, `cancel_reason`) 
VALUES (@cust_id, 4, LAST_INSERT_ID(), 'CANCELLED', 130.00, 'Unfinished Topic', 'CUSTOMER', 'Change of plans');

-- --- 2026.05 ---

-- 2026.5.12 21:00
INSERT INTO `time_slots` (`specialist_id`, `slot_date`, `start_time`, `end_time`, `status`) VALUES (5, '2026-05-12', '21:00:00', '22:00:00', 'BOOKED');
INSERT INTO `bookings` (`customer_id`, `specialist_id`, `slot_id`, `status`, `price`, `topic`) VALUES (@cust_id, 5, LAST_INSERT_ID(), 'CONFIRMED', 255.00, 'Evening Consult');

-- 2026.5.19 21:00 
INSERT INTO `time_slots` (`specialist_id`, `slot_date`, `start_time`, `end_time`, `status`) VALUES (9, '2026-05-19', '21:00:00', '22:00:00', 'BOOKED');
INSERT INTO `bookings` (`customer_id`, `specialist_id`, `slot_id`, `status`, `price`, `topic`) VALUES (@cust_id, 9, LAST_INSERT_ID(), 'CONFIRMED', 285.00, 'Urgent Review');

-- 2026.5.20 21:00 
INSERT INTO `time_slots` (`specialist_id`, `slot_date`, `start_time`, `end_time`, `status`) VALUES (12, '2026-05-20', '21:00:00', '22:00:00', 'BOOKED');
INSERT INTO `bookings` (`customer_id`, `specialist_id`, `slot_id`, `status`, `price`, `topic`) VALUES (@cust_id, 12, LAST_INSERT_ID(), 'PENDING', 135.00, 'Waiting Approval');

-- 2026.5.26 21:00 
INSERT INTO `time_slots` (`specialist_id`, `slot_date`, `start_time`, `end_time`, `status`) VALUES (4, '2026-05-26', '21:00:00', '22:00:00', 'BOOKED');
INSERT INTO `bookings` (`customer_id`, `specialist_id`, `slot_id`, `status`, `price`, `topic`) VALUES (@cust_id, 4, LAST_INSERT_ID(), 'CONFIRMED', 130.00, 'Regular Checkup');

COMMIT;