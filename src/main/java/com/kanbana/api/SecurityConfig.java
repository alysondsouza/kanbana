package com.kanbana.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — stateless JWT apps don't use cookies for auth
            .csrf(csrf -> csrf.disable())

            // Apply CORS config — must be before auth filters
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Stateless — no session, no JSESSIONID cookie
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Return 401 instead of redirect to login page
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

            // Endpoint rules — specific before general
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()       // register + login
                .requestMatchers("/swagger-ui/**").permitAll()        // Swagger UI
                .requestMatchers("/v3/api-docs/**").permitAll()       // Swagger API docs
                .requestMatchers("/hello", "/bye").permitAll()        // smoke test endpoints
                .anyRequest().authenticated()                         // everything else requires JWT
            )

            // Run JwtAuthFilter before Spring's default username/password filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // BCrypt password encoder — injected into AuthService
    // Cost factor 12: ~250ms per hash — strong enough, not too slow
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // CORS — allows the frontend to call the API from a different origin.
    // 12-Factor App: allowed origins come from an env var, never hardcoded.
    //
    // Dev:  ALLOWED_ORIGINS="http://172.25.36.218:5173" mvn spring-boot:run
    // Prod: ALLOWED_ORIGINS="https://kanbana.pages.dev" (set in Ansible vault)
    //
    // Multiple origins supported — comma-separated:
    //   ALLOWED_ORIGINS="http://localhost:5173,http://172.25.36.218:5173"
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Read from env var — fall back to localhost for dev if not set
        String allowedOriginsEnv = System.getenv()
                .getOrDefault("ALLOWED_ORIGINS", "http://localhost:5173");

        List<String> allowedOrigins = Arrays.asList(allowedOriginsEnv.split(","));

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);

        // OPTIONS must be included — browsers send a preflight OPTIONS before the real request
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));

        // Authorization — needed to send the JWT; Content-Type — needed for JSON bodies
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // Allows the browser to read response headers from cross-origin responses
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // apply to every endpoint
        return source;
    }
}
