package com.kanbana.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class BoardControllerTest {

    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .addFilter(springSecurityFilterChain)   // activates JwtAuthFilter and SecurityConfig
                .build();

        // Register a unique user per test run to avoid H2 duplicate username errors
        String username = "user-" + UUID.randomUUID().toString().substring(0, 8);
        String body = objectMapper.writeValueAsString(
                Map.of("username", username,
                       "email", username + "@example.com",
                       "password", "password123"));

        String response = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andReturn()
                .getResponse()
                .getContentAsString();

        token = objectMapper.readTree(response).get("token").asText();
    }

    // ── POST /api/v1/boards → 201 ─────────────────────────────────────────────

    @Test
    void createBoard_validRequest_returns201WithBody() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("title", "My Board"));

        mockMvc.perform(post("/api/v1/boards")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("My Board"));
    }

    // ── GET /api/v1/boards/{id} → 200 ────────────────────────────────────────

    @Test
    void getBoard_existingId_returns200() throws Exception {
        // Create a board first
        String body = objectMapper.writeValueAsString(Map.of("title", "Fetch Me"));

        String response = mockMvc.perform(post("/api/v1/boards")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/v1/boards/" + id)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Fetch Me"));
    }

    // ── GET /api/v1/boards/{id} → 404 ────────────────────────────────────────

    @Test
    void getBoard_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/boards/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── POST /api/v1/boards — blank title → 400 ──────────────────────────────

    @Test
    void createBoard_blankTitle_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("title", ""));

        mockMvc.perform(post("/api/v1/boards")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ── No token → 401 ───────────────────────────────────────────────────────

    @Test
    void getBoards_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/boards"))
                .andExpect(status().isUnauthorized());
    }
}
