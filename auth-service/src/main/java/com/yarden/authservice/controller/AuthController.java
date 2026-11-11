package com.yarden.authservice.controller;

import com.yarden.authservice.dto.LoginRequest;
import com.yarden.authservice.dto.RegisterRequest;
import com.yarden.authservice.dto.UserResponse;
import com.yarden.authservice.model.AppUser;
import com.yarden.authservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String COOKIE_NAME = "ACCESS_TOKEN";

    @Autowired
    private AuthService authService;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        AppUser user = authService.register(request);
        return withTokenCookie(HttpStatus.CREATED, user);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        AppUser user = authService.login(request);
        return withTokenCookie(HttpStatus.OK, user);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = buildCookie("", Duration.ZERO);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@CookieValue(name = COOKIE_NAME, required = false) String token) {
        return ResponseEntity.ok(authService.me(token));
    }

    private ResponseEntity<UserResponse> withTokenCookie(HttpStatus status, AppUser user) {
        String token = authService.issueToken(user);
        ResponseCookie cookie = buildCookie(token, Duration.ofSeconds(authService.tokenExpirationSeconds()));
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(authService.toResponse(user));
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}