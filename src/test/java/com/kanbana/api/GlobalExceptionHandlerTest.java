package com.kanbana.api;

import com.kanbana.application.service.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// No @SpringBootTest — we build MockMvc manually in standalone mode.
// Only GlobalExceptionHandler is loaded. No Spring context, no DB, very fast.
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Standalone mode: wire a fake controller + the handler under test.
        // Spring is NOT started — MockMvc simulates the DispatcherServlet only.
        mockMvc = MockMvcBuilders
            .standaloneSetup(new FakeController())           // controller that always throws
            .setControllerAdvice(new GlobalExceptionHandler()) // handler under test
            .build();
    }

    // ── EntityNotFoundException → 404 ────────────────────────────────────────

    @Test
    void entityNotFound_returns404WithCorrectBody() throws Exception {
        mockMvc.perform(get("/fake/not-found"))
            .andExpect(status().isNotFound())                      // HTTP 404
            .andExpect(jsonPath("$.status").value(404))            // body.status = 404
            .andExpect(jsonPath("$.message").value("Board not found: abc")) // body.message
            .andExpect(jsonPath("$.timestamp").exists());           // timestamp is present
    }

    // ── Generic exception → 500 ───────────────────────────────────────────────

    @Test
    void genericException_returns500WithSafeMessage() throws Exception {
        mockMvc.perform(get("/fake/error"))
            .andExpect(status().isInternalServerError())           // HTTP 500
            .andExpect(jsonPath("$.status").value(500))
            // OWASP: stack trace must never reach the client — only a safe generic message
            .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    // ── Fake controller ───────────────────────────────────────────────────────
    // A minimal controller used only in this test to trigger specific exceptions.
    // It has no business logic — it just throws on demand.

    @org.springframework.web.bind.annotation.RestController
    static class FakeController {

        @org.springframework.web.bind.annotation.GetMapping("/fake/not-found")
        public void throwNotFound() {
            throw new EntityNotFoundException("Board not found: abc");
        }

        @org.springframework.web.bind.annotation.GetMapping("/fake/error")
        public void throwGeneric() {
            throw new RuntimeException("Something exploded internally");
        }
    }
}
