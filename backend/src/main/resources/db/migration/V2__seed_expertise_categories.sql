INSERT INTO `expertise_categories` (`category_name`)
SELECT * FROM (
    SELECT 'Psychology' AS `category_name`
    UNION ALL
    SELECT 'Finance'
    UNION ALL
    SELECT 'Education'
) AS seed_categories
WHERE NOT EXISTS (
    SELECT 1
    FROM `expertise_categories` existing
    WHERE existing.`category_name` = seed_categories.`category_name`
);
