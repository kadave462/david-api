package com.example.david_api.auth;

import com.example.david_api.auth.dto.LoginRequest;
import com.example.david_api.auth.dto.LoginResponse;
import com.example.david_api.security.JwtService;
import com.example.david_api.security.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final String adminUsername;
    private final String adminPassword;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    public AuthController(
            @Value("${app.admin-username}") String adminUsername,
            @Value("${app.admin-password}") String adminPassword,
            JwtService jwtService,
            LoginAttemptService loginAttemptService) {
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();

        if (loginAttemptService.isLockedOut(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many failed login attempts. Try again later.");
        }

        boolean valid = request.getUsername() != null && request.getPassword() != null
                && constantTimeEquals(request.getUsername(), adminUsername)
                && constantTimeEquals(request.getPassword(), adminPassword);

        if (!valid) {
            loginAttemptService.recordFailure(ip);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }

        loginAttemptService.recordSuccess(ip);
        String token = jwtService.generateToken(adminUsername);
        return ResponseEntity.ok(new LoginResponse(token, jwtService.getExpirationMs()));
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
