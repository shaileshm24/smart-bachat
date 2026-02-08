package com.ametsa.smartbachat.controller;

import com.ametsa.smartbachat.config.SetuConfig;
import com.ametsa.smartbachat.dto.setu.SetuWebhookPayload;
import com.ametsa.smartbachat.service.BankConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Controller for handling Setu AA webhook notifications.
 */
@RestController
@RequestMapping("/api/v1/webhooks/setu")
public class SetuWebhookController {

    private static final Logger log = LoggerFactory.getLogger(SetuWebhookController.class);

    private final BankConnectionService bankConnectionService;
    private final SetuConfig setuConfig;

    public SetuWebhookController(BankConnectionService bankConnectionService, SetuConfig setuConfig) {
        this.bankConnectionService = bankConnectionService;
        this.setuConfig = setuConfig;
    }

    /**
     * Handle webhook notifications from Setu.
     * Setu sends notifications for consent and session status updates.
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "x-setu-signature", required = false) String signature) {

        log.info("Received Setu webhook");

        // Verify signature if webhook secret is configured
        if (setuConfig.getWebhookSecret() != null && !setuConfig.getWebhookSecret().isEmpty()) {
            if (!verifySignature(rawBody, signature)) {
                log.warn("Invalid webhook signature");
                return ResponseEntity.status(401).body("Invalid signature");
            }
        }

        try {
            // Parse payload
            com.fasterxml.jackson.databind.ObjectMapper mapper = 
                    new com.fasterxml.jackson.databind.ObjectMapper();
            SetuWebhookPayload payload = mapper.readValue(rawBody, SetuWebhookPayload.class);

            // Process webhook
            bankConnectionService.handleWebhook(payload);

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.status(500).body("Error processing webhook");
        }
    }

    /**
     * Verify webhook signature using HMAC-SHA256.
     */
    private boolean verifySignature(String payload, String signature) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    setuConfig.getWebhookSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = Base64.getEncoder().encodeToString(hash);
            return computedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Error verifying signature", e);
            return false;
        }
    }

    /**
     * Health check endpoint for webhook.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Webhook endpoint is healthy");
    }
}

