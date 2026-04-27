CREATE TABLE IF NOT EXISTS `availability_recurring_rule_exceptions` (
    `id` BIGINT AUTO_INCREMENT,
    `recurring_rule_id` BIGINT NOT NULL,
    `slot_date` DATE NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_slot_date` (`recurring_rule_id`, `slot_date`)
) COMMENT='Recurring rule occurrence overrides';

ALTER TABLE `availability_recurring_rule_exceptions`
    ADD CONSTRAINT `fk_rule_exceptions_rule_id`
        FOREIGN KEY (`recurring_rule_id`) REFERENCES `availability_recurring_rules` (`id`)
        ON UPDATE NO ACTION ON DELETE CASCADE;
