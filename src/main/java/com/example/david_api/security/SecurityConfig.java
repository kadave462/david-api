package com.example.david_api.security;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Without this, Spring Security's default for an unauthenticated
                // request on a stateless API (no formLogin/httpBasic configured)
                // is 403, not 401 — but the dashboard's authFetch() only treats
                // 401 as "not logged in, redirect to /login". 403 should stay
                // reserved for "authenticated but not allowed".
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        // The browser's CORS preflight is a bare OPTIONS with no
                        // Authorization header — anyRequest().authenticated() was
                        // rejecting it with 401 before the CorsFilter above ever
                        // got to attach Access-Control-Allow-* headers, which made
                        // every cross-origin authenticated call fail in-browser
                        // even though the same request worked fine from curl.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/ingest/health").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // WebConfig's addCorsMappings (MVC-level CORS) never runs for these
    // routes — Spring Security's filter chain sits in front of the
    // DispatcherServlet and, once locked down with anyRequest().authenticated(),
    // intercepts the preflight before MVC ever sees it. CORS has to be
    // configured at this layer instead so the CorsFilter Spring Security
    // installs (via .cors() above) runs early enough to handle it.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "https://tramed-fe-service.onrender.com"));
        configuration.setAllowedMethods(List.of("GET", "POST"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
