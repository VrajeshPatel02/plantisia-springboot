
package com.nikyfarm.config;

import com.nikyfarm.entity.User;
import com.nikyfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
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

        boolean adminExists = userRepository.findAll()
                .stream()
                .anyMatch(user -> user.getRole() == User.Role.ADMIN);

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
