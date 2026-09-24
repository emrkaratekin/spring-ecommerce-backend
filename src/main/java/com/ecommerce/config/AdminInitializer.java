package com.ecommerce.config;

import com.ecommerce.entity.User;
import com.ecommerce.entity.enums.Role;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Creates the initial ADMIN account on startup if it does not exist yet.
 * Credentials come from configuration (environment variables in production), never from code.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final AdminProperties adminProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = adminProperties.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            return;
        }

        User admin = User.builder()
                .firstName(adminProperties.firstName())
                .lastName(adminProperties.lastName())
                .email(email)
                .password(passwordEncoder.encode(adminProperties.password()))
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);
        log.info("Initial admin account created: {}", email);
    }
}
