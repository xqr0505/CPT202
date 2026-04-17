ALTER TABLE `availability_recurring_rules`
    MODIFY `effective_end_date` DATE NULL;

UPDATE `availability_recurring_rules` arr
JOIN `specialist_profiles` sp
    ON sp.id = arr.specialist_id
JOIN `users` u
    ON u.id = sp.user_id
SET arr.`effective_end_date` = NULL
WHERE u.email LIKE '%@medlink.local'
  AND arr.`effective_end_date` = '2026-05-17';
