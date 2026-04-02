SET FOREIGN_KEY_CHECKS = 0;

-- ========================================================
-- 1.Users
-- ========================================================
INSERT INTO `users` (`id`, `email`, `password_hash`, `role`, `status`, `full_name`, `phone_number`, `lock_time`) VALUES
(1, 'customer1@example.com', '$2a$10$abcdefghijklmnopqrstuvwxy1234567890123456789012345678901', 'CUSTOMER', 'ACTIVE', 'Mock Customer A', '13800000001', '1970-01-01 00:00:00'),
(2, 'customer2@example.com', '$2a$10$abcdefghijklmnopqrstuvwxy1234567890123456789012345678901', 'CUSTOMER', 'ACTIVE', 'Mock Customer B', '13800000002', '1970-01-01 00:00:00'),
(3, 'specialist1@example.com', '$2a$10$abcdefghijklmnopqrstuvwxy1234567890123456789012345678901', 'SPECIALIST', 'ACTIVE', 'Dr. Mock Specialist A', '13900000001', '1970-01-01 00:00:00'),
(4, 'specialist2@example.com', '$2a$10$abcdefghijklmnopqrstuvwxy1234567890123456789012345678901', 'SPECIALIST', 'ACTIVE', 'Prof. Mock Specialist B', '13900000002', '1970-01-01 00:00:00'),
(5, 'admin@example.com', '$2a$10$abcdefghijklmnopqrstuvwxy1234567890123456789012345678901', 'ADMIN', 'ACTIVE', 'Super Admin', '13700000000', '1970-01-01 00:00:00');

-- ========================================================
-- 2. Expertise Categories
-- ========================================================
INSERT INTO `expertise_categories` (`id`, `category_name`) VALUES
(1, 'Software Engineering'),
(2, 'Career Transition'),
(3, 'Mental Health');

-- ========================================================
-- 4. Specialist Profiles
-- ========================================================
INSERT INTO `specialist_profiles` (`id`, `user_id`, `category_id`, `level`, `consultation_fee`, `avatar_url`, `bio`, `status`) VALUES
(1, 3, 1, 'Senior', 150.00, 'https://api.dicebear.com/7.x/avataaars/svg?seed=specialist1', '10 years of Backend Architecture experience.', 'ACTIVE'),
(2, 4, 2, 'Expert', 200.00, 'https://api.dicebear.com/7.x/avataaars/svg?seed=specialist2', 'Helped over 500 students landed their dream jobs.', 'ACTIVE');

-- ========================================================
-- 5. Availability Recurring Rules
-- ========================================================
INSERT INTO `availability_recurring_rules` (`id`, `specialist_id`, `day_of_week`, `start_time`, `end_time`, `effective_end_date`) VALUES
(1, 1, 1, '09:00:00', '12:00:00', '2026-12-31'),
(2, 2, 3, '14:00:00', '17:00:00', '2026-12-31');

-- ========================================================
-- 6. Time Slots
-- ========================================================
INSERT INTO `time_slots` (`id`, `specialist_id`, `recurring_rule_id`, `slot_date`, `start_time`, `end_time`, `status`) VALUES
-- Specialist 1 (Past and Future slots)
(1, 1, 1, '2026-04-10', '09:00:00', '10:00:00', 'BOOKED'),
(2, 1, 1, '2026-04-10', '10:00:00', '11:00:00', 'AVAILABLE'),
(3, 1, 1, '2026-03-25', '09:00:00', '10:00:00', 'BOOKED'), -- 过去已完成的 Slot
(4, 1, 1, '2026-04-15', '14:00:00', '15:00:00', 'BOOKED'), -- 未来被取消的 Slot

-- Specialist 2
(5, 2, 2, '2026-04-11', '14:00:00', '15:00:00', 'BOOKED'),
(6, 2, 2, '2026-04-12', '15:00:00', '16:00:00', 'AVAILABLE');

-- ========================================================
-- 7. Bookings
-- ========================================================
INSERT INTO `bookings` (`id`, `customer_id`, `specialist_id`, `slot_id`, `status`, `price`, `topic`, `customer_notes`,
                        `parent_booking_id`, `decision_time`, `cancelled_by`, `change_type`, `refund_status`) VALUES

(1, 1, 1, 1, 'PENDING', 150.00, 'Spring Boot Architecture Review', 'Please help me review my system design.',
 1, '1970-01-01 00:00:00', 'NONE', 'NONE', 'NONE'),

(2, 2, 2, 5, 'CONFIRMED', 200.00, 'Mock Interview Prcatice', 'Targeting Big Tech companies.',
 2, '2026-04-01 10:00:00', 'NONE', 'NONE', 'NONE'),

(3, 1, 1, 3, 'COMPLETED', 150.00, 'Database Query Optimization', 'Need advice on MySQL slow queries.',
 3, '2026-03-24 10:00:00', 'NONE', 'NONE', 'NONE'),

(4, 1, 1, 4, 'CANCELLED', 150.00, 'Java Concurrent Programming', 'I have a bug related to thread pool.',
 4, '2026-04-02 12:00:00', 'CUSTOMER', 'CANCEL', 'PENDING'),
(5, 1, 1, 6, 'PENDING', 150.00, 'Spring Boot Architecture Review', 'Please help me review my system design.',
    5, '2026-04-02 23:00:00', 'NONE', 'NONE', 'NONE'),

(6, 1, 2, 7, 'CONFIRMED', 180.00, 'React Performance Optimization', 'Need help with slow rendering.',
6, '2026-05-15 14:00:00', 'NONE', 'NONE', 'NONE'),
(7, 2, 2, 8, 'CONFIRMED', 200.00, 'System Design Interview Prep', 'Targeting FAANG companies.',
 7, '2026-05-20 10:00:00', 'NONE', 'NONE', 'NONE'),

(8, 1, 1, 9, 'CONFIRMED', 150.00, 'Microservices Architecture', 'Need guidance on service decomposition.',
 8, '2026-06-05 15:00:00', 'NONE', 'NONE', 'NONE'),

(9, 1, 1, 10, 'PENDING', 180.00, 'Kubernetes Deployment Strategy', 'Want to move from Docker to K8s.',
 9, '2026-06-10 20:00:00', 'NONE', 'NONE', 'NONE');


SET FOREIGN_KEY_CHECKS = 1;
