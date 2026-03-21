package com.kanbana.application.service;

import com.kanbana.application.dto.AuthResponseDTO;
import com.kanbana.application.dto.LoginRequestDTO;
import com.kanbana.application.dto.RegisterRequestDTO;
import com.kanbana.domain.model.User;
import com.kanbana.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthService authService;

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    void register_happyPath_returnsToken() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(UUID.class), anyString())).thenReturn("mock-token");

        AuthResponseDTO result = authService.register(request);

        assertThat(result.getToken()).isEqualTo("mock-token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateUsername_throwsConflictException() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("alice");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicateEmail_throwsConflictException() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("alice@example.com");

        verify(userRepository, never()).save(any());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_happyPath_returnsToken() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "alice", "alice@example.com", "hashed", Instant.now());

        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("alice");
        request.setPassword("password123");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(userId, "alice")).thenReturn("mock-token");

        AuthResponseDTO result = authService.login(request);

        assertThat(result.getToken()).isEqualTo("mock-token");
    }

    @Test
    void login_wrongPassword_throwsEntityNotFoundException() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "alice", "alice@example.com", "hashed", Instant.now());

        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("alice");
        request.setPassword("wrongpassword");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashed")).thenReturn(false);

        // OWASP: same exception for wrong password as for unknown user — no enumeration
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void login_unknownUser_throwsEntityNotFoundException() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("ghost");
        request.setPassword("password123");

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
