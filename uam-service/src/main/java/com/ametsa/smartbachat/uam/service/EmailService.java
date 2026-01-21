package com.ametsa.smartbachat.uam.service;

import com.ametsa.smartbachat.uam.config.EmailConfig;
import com.ametsa.smartbachat.uam.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailConfig emailConfig;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine, EmailConfig emailConfig) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.emailConfig = emailConfig;
    }

    @Async
    public void sendVerificationEmail(User user, String token) {
        String verificationUrl = emailConfig.getBaseUrl() + "/verify-email?token=" + token;
        
        Context context = new Context();
        context.setVariable("name", user.getFullName());
        context.setVariable("verificationUrl", verificationUrl);
        context.setVariable("expirationHours", emailConfig.getVerificationTokenExpirationMinutes() / 60);

        String htmlContent = templateEngine.process("email-verification", context);
        
        sendHtmlEmail(user.getEmail(), "Verify your email - Smart Bachat", htmlContent);
        log.info("Verification email sent to: {}", user.getEmail());
    }

    @Async
    public void sendPasswordResetEmail(User user, String token) {
        String resetUrl = emailConfig.getBaseUrl() + "/reset-password?token=" + token;
        
        Context context = new Context();
        context.setVariable("name", user.getFullName());
        context.setVariable("resetUrl", resetUrl);
        context.setVariable("expirationMinutes", emailConfig.getPasswordResetTokenExpirationMinutes());

        String htmlContent = templateEngine.process("password-reset", context);
        
        sendHtmlEmail(user.getEmail(), "Reset your password - Smart Bachat", htmlContent);
        log.info("Password reset email sent to: {}", user.getEmail());
    }

    @Async
    public void sendPasswordChangedNotification(User user) {
        Context context = new Context();
        context.setVariable("name", user.getFullName());

        String htmlContent = templateEngine.process("password-changed", context);
        
        sendHtmlEmail(user.getEmail(), "Your password has been changed - Smart Bachat", htmlContent);
        log.info("Password changed notification sent to: {}", user.getEmail());
    }

    @Async
    public void sendWelcomeEmail(User user) {
        Context context = new Context();
        context.setVariable("name", user.getFullName());
        context.setVariable("loginUrl", emailConfig.getBaseUrl() + "/login");

        String htmlContent = templateEngine.process("welcome", context);
        
        sendHtmlEmail(user.getEmail(), "Welcome to Smart Bachat!", htmlContent);
        log.info("Welcome email sent to: {}", user.getEmail());
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        // Mock mode - just log the email instead of sending
        if (emailConfig.isMockEnabled()) {
            log.info("=== MOCK EMAIL ===");
            log.info("To: {}", to);
            log.info("Subject: {}", subject);
            log.info("Content preview: {}", htmlContent.substring(0, Math.min(200, htmlContent.length())) + "...");
            log.info("==================");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailConfig.getFromAddress(), emailConfig.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }
}

