package edu.xjtlu.cpt202.backend.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Seeds minimal specialist data so schedule pages can be exercised without the auth module.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevScheduleBypassInitializer {

    private static final long DEV_USER_ID = 1L;
    private static final long DEV_CATEGORY_ID = 1L;
    private static final long DEV_SPECIALIST_ID = 1L;

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        Integer specialistCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM specialist_profiles",
                Integer.class
        );
        if (specialistCount != null && specialistCount > 0) {
            log.info("Detected existing specialist profiles, skipping dev specialist seed");
            return;
        }

        seedCategory();
        seedUser();
        seedSpecialistProfile();
    }

    private void seedCategory() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM expertise_categories WHERE id = ?",
                Integer.class,
                DEV_CATEGORY_ID
        );
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update(
                "INSERT INTO expertise_categories (id, category_name) VALUES (?, ?)",
                DEV_CATEGORY_ID,
                "Pediatrics"
        );
        log.info("Seeded dev expertise category {}", DEV_CATEGORY_ID);
    }

    private void seedUser() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?",
                Integer.class,
                DEV_USER_ID
        );
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update(
                """
                INSERT INTO users (
                    id, email, password_hash, role, status, full_name, phone_number, login_fail_count, lock_time
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """,
                DEV_USER_ID,
                "schedule-dev@example.com",
                "dev-bypass-no-login",
                "SPECIALIST",
                "ACTIVE",
                "Dr. Dev Pediatrics",
                "0000000000",
                0
        );
        log.info("Seeded dev specialist user {}", DEV_USER_ID);
    }

    private void seedSpecialistProfile() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM specialist_profiles WHERE id = ?",
                Integer.class,
                DEV_SPECIALIST_ID
        );
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update(
                """
                INSERT INTO specialist_profiles (
                    id, user_id, category_id, level, consultation_fee, bio, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                DEV_SPECIALIST_ID,
                DEV_USER_ID,
                DEV_CATEGORY_ID,
                "SENIOR",
                220.00,
                "Sample pediatric doctor profile used for local schedule development.",
                "ACTIVE"
        );
        log.info("Seeded dev specialist profile {}", DEV_SPECIALIST_ID);
    }
}
