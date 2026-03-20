package com.kanbana.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

// Spring Boot 4 removed @AutoConfigureMockMvc and @WebMvcTest from test-autoconfigure.
// We build MockMvc manually from the WebApplicationContext instead.
// @SpringBootTest starts the full context — controllers, services, repos, H2, Flyway.
// @ActiveProfiles("test") loads application-test.properties → H2 instead of Postgres.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class BoardControllerTest {

    @Autowired
    private WebApplicationContext context;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Build MockMvc from the full web context — same as @AutoConfigureMockMvc did
    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    // ── POST /api/v1/boards → 201 ─────────────────────────────────────────────

    @Test
    void createBoard_validRequest_returns201WithBody() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("title", "My Board"));

        mockMvc().perform(post("/api/v1/boards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.title").value("My Board"));
    }

    // ── GET /api/v1/boards/{id} → 200 ────────────────────────────────────────

    @Test
    void getBoard_existingId_returns200() throws Exception {
        // Step 1 — create a board to get a real id
        String body = objectMapper.writeValueAsString(Map.of("title", "Fetch Me"));

        String responseJson = mockMvc().perform(post("/api/v1/boards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        // Step 2 — extract the id from the POST response
        String id = objectMapper.readTree(responseJson).get("id").asText();

        // Step 3 — GET by that id
        mockMvc().perform(get("/api/v1/boards/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.title").value("Fetch Me"));
    }

    // ── GET /api/v1/boards/{id} → 404 ────────────────────────────────────────

    @Test
    void getBoard_unknownId_returns404() throws Exception {
        String randomId = UUID.randomUUID().toString();

        mockMvc().perform(get("/api/v1/boards/" + randomId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    // ── POST /api/v1/boards — blank title → 400 ──────────────────────────────

    @Test
    void createBoard_blankTitle_returns400WithValidationMessage() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("title", ""));

        mockMvc().perform(post("/api/v1/boards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value(
                org.hamcrest.Matchers.containsString("title")));
    }
}
