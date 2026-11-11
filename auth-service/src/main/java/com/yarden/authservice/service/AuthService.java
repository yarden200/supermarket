package com.yarden.authservice.service;

import com.yarden.authservice.dto.LoginRequest;
import com.yarden.authservice.dto.RegisterRequest;
import com.yarden.authservice.dto.UserResponse;
import com.yarden.authservice.exception.DuplicateResourceException;
import com.yarden.authservice.exception.NotAuthenticatedException;
import com.yarden.authservice.model.AppUser;
import com.yarden.authservice.model.Role;
import com.yarden.authservice.repository.AppUserRepository;
import com.yarden.authservice.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Transactional
    public AppUser register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("User", "username", request.username());
        }
        AppUser user = AppUser.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .roles(new HashSet<>(Set.of(Role.USER)))
                .build();
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AppUser login(LoginRequest request) {
        AppUser user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return user;
    }

    @SuppressWarnings("unchecked")
    public UserResponse me(String token) {
        if (token == null) {
            throw new NotAuthenticatedException();
        }
        try {
            Claims claims = jwtService.parse(token);
            Long id = ((Number) claims.get("uid")).longValue();
            List<String> roles = (List<String>) claims.get("roles");
            return new UserResponse(id, claims.getSubject(), roles);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new NotAuthenticatedException();
        }
    }

    public String issueToken(AppUser user) {
        return jwtService.issue(user);
    }

    public long tokenExpirationSeconds() {
        return jwtService.getExpirationSeconds();
    }

    public UserResponse toResponse(AppUser user) {
        List<String> roles = user.getRoles().stream().map(Role::name).sorted().toList();
        return new UserResponse(user.getId(), user.getUsername(), roles);
    }
}