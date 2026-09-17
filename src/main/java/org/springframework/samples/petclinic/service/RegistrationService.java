package org.springframework.samples.petclinic.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public RegistrationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void register(String username, String password) {
        // INSERT deliberately rejects duplicate usernames instead of updating an existing account.
        jdbc.update("INSERT INTO users (username, password, enabled) VALUES (?, ?, ?)",
            username, encoder.encode(password), true);
        jdbc.update("INSERT INTO roles (username, role) VALUES (?, ?)", username, "ROLE_USER");
    }
}
