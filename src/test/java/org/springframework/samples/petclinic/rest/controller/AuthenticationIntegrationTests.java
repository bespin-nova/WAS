package org.springframework.samples.petclinic.rest.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles({"h2", "spring-data-jpa"})
class AuthenticationIntegrationTests {
    @Autowired private WebApplicationContext context;
    @Autowired private JdbcTemplate jdbc;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jdbc.update("DELETE FROM roles WHERE username = ?", "auth_test");
        jdbc.update("DELETE FROM users WHERE username = ?", "auth_test");
    }

    @Test
    void registrationLoginSessionAndLogout() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
            .content("{\"username\":\"auth_test\",\"password\":\"test-password\",\"roles\":[{\"name\":\"ROLE_ADMIN\"}]}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.password").doesNotExist());
        String hash = jdbc.queryForObject("SELECT password FROM users WHERE username = ?", String.class, "auth_test");
        assertTrue(new BCryptPasswordEncoder().matches("test-password", hash));
        assertEquals("ROLE_USER", jdbc.queryForObject("SELECT role FROM roles WHERE username = ?", String.class, "auth_test"));
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
            .content("{\"username\":\"auth_test\",\"password\":\"replacement\"}"))
            .andExpect(status().isConflict());
        assertEquals(hash, jdbc.queryForObject("SELECT password FROM users WHERE username = ?", String.class, "auth_test"));
        mvc.perform(post("/api/auth/login").with(csrf()).param("username", "auth_test").param("password", "wrong"))
            .andExpect(status().isUnauthorized());
        MockHttpSession session = (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
            .param("username", "auth_test").param("password", "test-password"))
            .andExpect(status().isNoContent()).andReturn().getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("auth_test"));
        mvc.perform(post("/api/users").session(session).with(csrf()).contentType("application/json")
            .content("{\"username\":\"forbidden\",\"password\":\"password\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void csrfAndValidationAreRequired() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json")
            .content("{\"username\":\"auth_test\",\"password\":\"password\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").param("username", "admin").param("password", "admin"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
            .content("{\"username\":\"auth_test\",\"password\":\"" + "a".repeat(73) + "\"}"))
            .andExpect(status().isBadRequest());
    }
}
