package com.kanbana.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// No Spring context — JwtService has no Spring dependencies, just @Value fields.
// ReflectionTestUtils injects values that @Value would normally provide.
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "test-secret-key-must-be-at-least-32-characters-long");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 86400000L);
    }

    @Test
    void generateToken_validInputs_returnsNonBlankToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "alice");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    void extractUserId_validToken_returnsCorrectId() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "alice");

        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void extractUsername_validToken_returnsCorrectUsername() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "alice");

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void isValid_validToken_returnsTrue() {
        String token = jwtService.generateToken(UUID.randomUUID(), "alice");

        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void isValid_expiredToken_returnsFalse() {
        // Set expiry to -1ms so token is immediately expired
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1L);
        String token = jwtService.generateToken(UUID.randomUUID(), "alice");

        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void isValid_tamperedToken_returnsFalse() {
        String token = jwtService.generateToken(UUID.randomUUID(), "alice");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(jwtService.isValid(tampered)).isFalse();
    }
}
