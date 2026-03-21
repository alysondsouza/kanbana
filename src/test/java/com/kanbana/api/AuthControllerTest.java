package com.kanbana.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Full Spring context + H2 + Security filter chain
// applySecurity() ensures JwtAuthFilter and SecurityConfig are active in tests
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc() {
        return MockMvcBuilders
                .webAppContextSetup(context)
                .addFilter(springSecurityFilterChain)   // activates JwtAuthFilter and SecurityConfig
                .build();
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    void register_validRequest_returns201WithToken() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", "bob", "email", "bob@example.com", "password", "password123"));

        mockMvc().perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void register_blankUsername_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", "", "email", "bob@example.com", "password", "password123"));

        mockMvc().perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        // Register first
        String registerBody = objectMapper.writeValueAsString(
                Map.of("username", "carol", "email", "carol@example.com", "password", "password123"));
        mockMvc().perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody));

        // Then login
        String loginBody = objectMapper.writeValueAsString(
                Map.of("username", "carol", "password", "password123"));

        mockMvc().perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_wrongPassword_returns404() throws Exception {
        // Register first
        String registerBody = objectMapper.writeValueAsString(
                Map.of("username", "dave", "email", "dave@example.com", "password", "password123"));
        mockMvc().perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody));

        // Login with wrong password
        String loginBody = objectMapper.writeValueAsString(
                Map.of("username", "dave", "password", "wrongpassword"));

        mockMvc().perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isNotFound());
    }

    // ── protected endpoint ────────────────────────────────────────────────────

    @Test
    void boards_withoutToken_returns401() throws Exception {
        mockMvc().perform(get("/api/v1/boards"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void boards_withValidToken_returns200() throws Exception {
        // Register + extract token
        String registerBody = objectMapper.writeValueAsString(
                Map.of("username", "eve", "email", "eve@example.com", "password", "password123"));

        String response = mockMvc().perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(response).get("token").asText();

        // Use token on protected endpoint
        mockMvc().perform(get("/api/v1/boards")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
