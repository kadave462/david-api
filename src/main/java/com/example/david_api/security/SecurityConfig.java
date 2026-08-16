package com.example.david_api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;

// Two independent client types hit this API: the sparkbind pharmacy agents
// (POST/GET /api/v1/ingest/**, authenticated by ApiKeyAuthFilter's shared
// key) and the dashboard, a logged-in human (everything else, authenticated
// by JwtAuthFilter's bearer token). Both filters run before Spring
// Security's own UsernamePasswordAuthenticationFilter and each only acts on
// the requests relevant to it, so a single filter chain covers both without
// them interfering with each other.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http, JwtAuthFilter jwtAuthFilter, ApiKeyAuthFilter apiKeyAuthFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // stateless token API, no browser-session cookies to forge
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Without this, Spring Security's default for an unauthenticated
                // request on a stateless API (no formLogin/httpBasic configured)
                // is 403, not 401 — but the dashboard's authFetch() only treats
                // 401 as "not logged in, redirect to /login". 403 should stay
                // reserved for "authenticated but not allowed".
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/ingest/health").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
