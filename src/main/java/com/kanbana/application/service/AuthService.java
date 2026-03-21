package com.kanbana.application.service;

import com.kanbana.application.dto.AuthResponseDTO;
import com.kanbana.application.dto.LoginRequestDTO;
import com.kanbana.application.dto.RegisterRequestDTO;
import com.kanbana.domain.model.User;
import com.kanbana.domain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;  // BCrypt — injected from SecurityConfig
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponseDTO register(RegisterRequestDTO request) {
        // Business rule: username and email must be unique
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }

        User user = new User(
                UUID.randomUUID(),
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),  // BCrypt hash — never plaintext
                Instant.now()
        );

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved.getId(), saved.getUsername());
        return new AuthResponseDTO(token);
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.getUsername()));

        // BCrypt verify — compares raw password against stored hash
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new EntityNotFoundException("Invalid credentials");  // same message — no username enumeration
        }

        String token = jwtService.generateToken(user.getId(), user.getUsername());
        return new AuthResponseDTO(token);
    }
}
