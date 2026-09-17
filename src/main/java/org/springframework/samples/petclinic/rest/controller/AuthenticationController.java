package org.springframework.samples.petclinic.rest.controller;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.service.RegistrationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final RegistrationService registration;

    public AuthenticationController(RegistrationService registration) {
        this.registration = registration;
    }

    public record RegistrationRequest(String username, String password) { }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest request) {
        String username = request.username() == null ? "" : request.username().trim();
        String password = request.password();
        if (username.isEmpty() || username.length() > 20 || password == null || password.isBlank()
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid username or password"));
        }
        try {
            registration.register(username, password);
        } catch (DuplicateKeyException exception) {
            return ResponseEntity.status(409).body(Map.of("message", "Username already exists"));
        }
        return ResponseEntity.status(201).body(Map.of("username", username));
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        return Map.of("username", authentication.getName(), "roles",
            authentication.getAuthorities().stream().map(authority -> authority.getAuthority()).toList());
    }
}
