package com.ametsa.smartbachat.uam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    private String secret;
    private Long expirationMs;
    private Long refreshExpirationMs;
    private String issuer;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public Long getExpirationMs() { return expirationMs; }
    public void setExpirationMs(Long expirationMs) { this.expirationMs = expirationMs; }
    public Long getRefreshExpirationMs() { return refreshExpirationMs; }
    public void setRefreshExpirationMs(Long refreshExpirationMs) { this.refreshExpirationMs = refreshExpirationMs; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
}

