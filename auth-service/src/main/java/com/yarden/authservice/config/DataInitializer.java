package com.yarden.authservice.config;

import com.yarden.authservice.model.AppUser;
import com.yarden.authservice.model.Role;
import com.yarden.authservice.repository.AppUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Value("${app.seed.user-password}")
    private String userPassword;

    @Override
    public void run(String... args) {
        createIfMissing("admin", adminPassword, Set.of(Role.ADMIN, Role.USER));
        createIfMissing("user", userPassword, Set.of(Role.USER));
        createIfMissing("user2", userPassword, Set.of(Role.USER));
    }

    private void createIfMissing(String username, String password, Set<Role> roles) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        AppUser user = AppUser.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .roles(new HashSet<>(roles))
                .build();
        userRepository.save(user);
        log.info("Created seed user '{}' with roles {}", username, roles);
    }
}