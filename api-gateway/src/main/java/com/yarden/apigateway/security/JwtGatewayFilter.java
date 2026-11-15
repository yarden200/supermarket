package com.yarden.apigateway.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    private static final String COOKIE_NAME = "ACCESS_TOKEN";

    @Value("${app.jwt.secret}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();

        if (path.startsWith("/api/products/stock")) {
            return reject(exchange, HttpStatus.NOT_FOUND, "Not found");
        }

        if (path.startsWith("/api/images") && !HttpMethod.GET.equals(method) ) {
            return reject(exchange, HttpStatus.NOT_FOUND, "Not found");
        }

        if (isPublic(path, method)) {
            return chain.filter(exchange);
        }

        HttpCookie cookie = request.getCookies().getFirst(COOKIE_NAME);
        if (cookie == null || cookie.getValue().isBlank()) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(cookie.getValue());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejecting invalid JWT: {}", ex.getMessage());
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        return chain.filter(exchange);
    }

    private boolean isPublic(String path, HttpMethod method) {
        if (path.startsWith("/api/auth/")) {
            return true;
        }
        boolean isRead = HttpMethod.GET.equals(method);
        return isRead && (path.startsWith("/api/products")
                || path.startsWith("/api/categories")
                || path.startsWith("/api/search")
                || path.startsWith("/api/images"));
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String detail) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        String body = """
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s","instance":"%s"}
                """.formatted(status.getReasonPhrase(), status.value(), detail, exchange.getRequest().getPath().value());

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}