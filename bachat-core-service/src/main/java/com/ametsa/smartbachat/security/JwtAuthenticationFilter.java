package com.ametsa.smartbachat.security;

import com.ametsa.smartbachat.config.JwtConfig;
import com.ametsa.smartbachat.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtConfig jwtConfig;
    private final SecretKey secretKey;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtConfig jwtConfig, ObjectMapper objectMapper) {
        this.jwtConfig = jwtConfig;
        this.secretKey = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String requestPath = request.getRequestURI();

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Check if it's a refresh token (should not be used for API access)
            if ("refresh".equals(claims.get("type"))) {
                sendErrorResponse(response, ApiErrorResponse.tokenInvalid(requestPath,
                    "Refresh tokens cannot be used for API access"));
                return;
            }

            String userId = claims.getSubject();
            String profileId = claims.get("profileId", String.class);

            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);

            List<SimpleGrantedAuthority> authorities = roles != null
                    ? roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList())
                    : List.of();

            // Create authentication with user principal containing both userId and profileId
            UserPrincipal principal = new UserPrincipal(
                    UUID.fromString(userId),
                    profileId != null ? UUID.fromString(profileId) : null,
                    claims.get("email", String.class),
                    roles,
                    token  // Store token for service-to-service calls
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired for request to {}: {}", requestPath, e.getMessage());
            sendErrorResponse(response, ApiErrorResponse.tokenExpired(requestPath));
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token for request to {}: {}", requestPath, e.getMessage());
            sendErrorResponse(response, ApiErrorResponse.tokenInvalid(requestPath, "Token is malformed"));
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature for request to {}: {}", requestPath, e.getMessage());
            sendErrorResponse(response, ApiErrorResponse.tokenInvalid(requestPath, "Token signature is invalid"));
        } catch (Exception e) {
            log.warn("JWT authentication failed for request to {}: {}", requestPath, e.getMessage());
            sendErrorResponse(response, ApiErrorResponse.tokenInvalid(requestPath, e.getMessage()));
        }
    }

    private void sendErrorResponse(HttpServletResponse response, ApiErrorResponse errorResponse)
            throws IOException {
        response.setStatus(errorResponse.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}

