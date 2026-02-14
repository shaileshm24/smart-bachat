package com.ametsa.smartbachat.uam.security;

import com.ametsa.smartbachat.uam.dto.ApiErrorResponse;
import com.ametsa.smartbachat.uam.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
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
            if (jwtService.validateToken(token) && !jwtService.isRefreshToken(token)) {
                Claims claims = jwtService.getClaimsFromToken(token);
                String userId = claims.getSubject();

                @SuppressWarnings("unchecked")
                List<String> roles = claims.get("roles", List.class);

                List<SimpleGrantedAuthority> authorities = roles != null
                        ? roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList())
                        : List.of();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
            } else if (jwtService.isRefreshToken(token)) {
                sendErrorResponse(response, ApiErrorResponse.tokenInvalid(requestPath,
                    "Refresh tokens cannot be used for API access"));
            } else {
                sendErrorResponse(response, ApiErrorResponse.tokenInvalid(requestPath, "Token validation failed"));
            }
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

