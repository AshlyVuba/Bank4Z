package com.bank4z.backend.adminservice;

import com.bank4z.backend.authservice.User;
import com.bank4z.backend.authservice.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first ADMIN account on startup, but ONLY if ADMIN_EMAIL and
 * ADMIN_PASSWORD are set in the environment. Credentials never live in the
 * repo, and there is no API anywhere that can create or promote an admin.
 *
 * The placeholder ID and phone below are deliberately impossible values
 * (00/00/00 isn't a real date; the phone has no valid mobile prefix), so the
 * registration validators reject them and no real customer can ever collide
 * with, or impersonate, the admin record.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private static final String ADMIN_ID_PLACEHOLDER = "0000000000000";
    private static final String ADMIN_PHONE_PLACEHOLDER = "0000000000";
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrap(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${bank4z.admin.email:}") String adminEmail,
                          @Value("${bank4z.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("Admin bootstrap skipped: ADMIN_EMAIL / ADMIN_PASSWORD not set");
            return;
        }
        if (adminPassword.length() < MIN_PASSWORD_LENGTH) {
            log.warn("Admin bootstrap refused: ADMIN_PASSWORD must be at least {} characters", MIN_PASSWORD_LENGTH);
            return;
        }
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin bootstrap: an account for {} already exists, leaving it untouched", adminEmail);
            return;
        }
        if (userRepository.existsByIdNumber(ADMIN_ID_PLACEHOLDER)) {
            log.info("Admin bootstrap: an admin record already exists, not creating another");
            return;
        }

        User admin = new User(
                "Bank4Z Admin",
                adminEmail,
                ADMIN_ID_PLACEHOLDER,
                ADMIN_PHONE_PLACEHOLDER,
                passwordEncoder.encode(adminPassword)
        );
        admin.setRole(User.Role.ADMIN);
        userRepository.save(admin);

        // Never log the password, only that it happened.
        log.info("Admin bootstrap: created admin account for {}", adminEmail);
    }
}