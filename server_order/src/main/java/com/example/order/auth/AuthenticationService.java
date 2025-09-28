package com.example.order.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final RestTemplate restTemplate;
    private final String sessionBaseUrl;
    private final String jwtSecret;

    public AuthenticationService(
            RestTemplate restTemplate,
            @Value("${services.auth.session-base-url:http://localhost:8081}") String sessionBaseUrl,
            @Value("${services.auth.jwt-secret:change-me-to-a-long-secret-key-please-keep-32-bytes}") String jwtSecret) {
        this.restTemplate = restTemplate;
        this.sessionBaseUrl = sessionBaseUrl;
        this.jwtSecret = jwtSecret;
    }

    public Optional<UserPrincipal> authenticate(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring("Bearer ".length());
            try {
                String username = Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getSubject();
                if (username != null && !username.isBlank()) {
                    return Optional.of(new UserPrincipal(username));
                }
            } catch (Exception ex) {
                log.warn("Invalid JWT token", ex);
            }
        }

        List<String> cookies = Collections.list(request.getHeaders(HttpHeaders.COOKIE));
        if (!cookies.isEmpty()) {
            HttpHeaders headers = new HttpHeaders();
            headers.put(HttpHeaders.COOKIE, cookies);
            try {
                ResponseEntity<WhoAmIResponse> response = restTemplate.exchange(
                        sessionBaseUrl + "/whoami",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        WhoAmIResponse.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return Optional.of(new UserPrincipal(response.getBody().username()));
                }
            } catch (RestClientException ex) {
                log.warn("Session validation failed", ex);
            }
        }

        return Optional.empty();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WhoAmIResponse(@JsonProperty("username") String username) {
    }
}
