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
    'test.admin@expertlink.com',
    '$2a$10$zdR7zU/HylNS6Xs0vvjmBenYrMJljRWm88U.vTHBHyG32LGv9TH.S',
    'ADMIN',
    'ACTIVE',
    'Test Admin',
    '13800009999',
    0,
    NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM `users`
    WHERE `email` = 'test.admin@expertlink.com'
);
