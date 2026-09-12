package com.nikyfarm.config;

import com.nikyfarm.entity.User;
import com.nikyfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates one ADMIN user on startup if none exists yet -- otherwise nobody could ever call an
 * admin-only endpoint, since registration always creates CUSTOMER accounts. Dev-only convenience;
 * change the seeded password (app.admin.password) after first login.
 */
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        boolean adminExists = userRepository.findAll().stream().anyMatch(u -> u.getRole() == User.Role.ADMIN);
        if (adminExists) {
            return;
        }

        User admin = new User();
        admin.setName("Admin");
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(User.Role.ADMIN);
        admin.setIsActive(true);
        userRepository.save(admin);
    }
}
