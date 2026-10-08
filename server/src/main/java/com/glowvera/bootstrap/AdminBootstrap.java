package com.glowvera.bootstrap;

import com.glowvera.config.AppProperties;
import com.glowvera.domain.Role;
import com.glowvera.entity.User;
import com.glowvera.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@Order(1)
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AppProperties props;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AdminBootstrap(AppProperties props, UserRepository users, PasswordEncoder encoder) {
        this.props = props;
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = props.admin().email() == null ? "" : props.admin().email().trim().toLowerCase();
        String password = props.admin().password();

        if (email.isEmpty() || password == null || password.isEmpty()) {
            log.info("ADMIN_EMAIL / ADMIN_PASSWORD not set: skipping admin bootstrap");
            return;
        }
        if (password.length() < 12) {
            log.error("ADMIN_PASSWORD must be at least 12 characters: admin account NOT created");
            return;
        }

        String name = props.admin().name() == null || props.admin().name().isBlank()
                ? "Store Admin" : props.admin().name();

        User admin = users.findByEmail(email).orElseGet(User::new);
        admin.setEmail(email);
        admin.setName(name);
        admin.setRole(Role.ADMIN);
        admin.setPasswordHash(encoder.encode(password));
        users.save(admin);
        log.info("Admin ready: {}", email);
    }
}
