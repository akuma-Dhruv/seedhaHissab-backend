package com.seedhahisaab.config;

import com.seedhahisaab.entity.User;
import com.seedhahisaab.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

/**
 * Seeds a default admin user (admin@admin.com / admin) on startup.
 *
 * Behaviour:
 *   - If no user exists with email admin@admin.com, one is created.
 *   - If the user already exists, nothing is touched (idempotent — safe across restarts).
 *
 * The password is hashed with the same BCryptPasswordEncoder used by registration.
 * "admin" is a weak password and is intended for local/dev convenience only.
 */
@Configuration
public class AdminSeeder {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private static final String ADMIN_EMAIL = "admin@admin.com";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String ADMIN_NAME = "Admin";

    public CommandLineRunner seedAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.existsByEmail(ADMIN_EMAIL)) {
                log.info("AdminSeeder: '{}' already exists — leaving untouched.", ADMIN_EMAIL);
                return;
            }
            User admin = User.builder()
                    .id(UUID.randomUUID())
                    .name(ADMIN_NAME)
                    .email(ADMIN_EMAIL)
                    .password(passwordEncoder.encode(ADMIN_PASSWORD))
                    .build();
            userRepository.save(admin);
            log.warn("AdminSeeder: created default admin user '{}' with password 'admin'. " +
                    "CHANGE THIS PASSWORD BEFORE DEPLOYING TO PRODUCTION.", ADMIN_EMAIL);
        };
    }

    @org.springframework.context.annotation.Bean
    public CommandLineRunner adminSeederRunner(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return seedAdmin(userRepository, passwordEncoder);
    }
}
