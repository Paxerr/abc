package com.danasea.backend.configs;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.danasea.backend.configs.properties.CloudinaryProperties;
import com.danasea.backend.configs.properties.CorsProperties;
import com.danasea.backend.configs.properties.PaymentProperties;
import com.danasea.backend.configs.properties.CheckinProperties;
import com.danasea.backend.modules.ai.infrastructure.cliproxy.configs.CliProxyProperties;
import com.danasea.backend.security.authentication.infrastructure.security.JwtProperties;

import jakarta.annotation.PostConstruct;

@Component
@Profile("prod")
public class ProductionConfigurationValidator {

    private final CloudinaryProperties cloudinary;
    private final CorsProperties cors;
    private final MailProperties mail;
    private final PaymentProperties payment;
    private final JwtProperties jwt;
    private final CheckinProperties checkin;
    private final CliProxyProperties cliProxy;

    public ProductionConfigurationValidator(
            CloudinaryProperties cloudinary,
            CorsProperties cors,
            MailProperties mail,
            PaymentProperties payment,
            JwtProperties jwt,
            CheckinProperties checkin,
            CliProxyProperties cliProxy) {
        this.cloudinary = cloudinary;
        this.cors = cors;
        this.mail = mail;
        this.payment = payment;
        this.jwt = jwt;
        this.checkin = checkin;
        this.cliProxy = cliProxy;
    }

    @PostConstruct
    void validate() {
        requireText(cloudinary.cloudName(), "cloudinary.cloud-name");
        requireText(cloudinary.apiKey(), "cloudinary.api-key");
        requireText(cloudinary.apiSecret(), "cloudinary.api-secret");
        requireText(mail.getUsername(), "spring.mail.username");
        requireText(mail.getPassword(), "spring.mail.password");
        if (!cliProxy.enabled()) {
            throw new IllegalStateException("Production AI requires CLI Proxy to be enabled.");
        }
        requireNonEmpty(cors.allowedOrigins(), "app.cors.allowed-origins");
        requireText(payment.webhookSecret(), "app.payment.webhook-secret");
        requireProductionSecret(jwt.secret(), "app.jwt.secret");
        requireProductionSecret(checkin.qrSecret(), "app.checkin.qr-secret");
        requireProductionSecret(payment.webhookSecret(), "app.payment.webhook-secret");
        if (cliProxy.enabled()) {
            requireProductionSecret(cliProxy.apiKey(), "ai.cliproxy.api-key");
        }
        if (new HashSet<>(List.of(jwt.secret(), checkin.qrSecret(), payment.webhookSecret(), cliProxy.apiKey())).size() != 4) {
            throw new IllegalStateException("Production signing and proxy secrets must be distinct.");
        }
        if (cors.allowedOrigins().stream().anyMatch(origin -> !isExactHttpsOrigin(origin))) {
            throw new IllegalStateException("Production CORS origins must be exact HTTPS origins without localhost.");
        }
    }

    private void requireText(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required production configuration is missing: " + property);
        }
    }

    private void requireNonEmpty(List<String> values, String property) {
        if (values == null || values.stream().allMatch(value -> value == null || value.isBlank())) {
            throw new IllegalStateException("Required production configuration is missing: " + property);
        }
    }

    private void requireProductionSecret(String value, String property) {
        requireText(value, property);
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.contains("change-me") || normalized.contains("your_")
                || normalized.contains("default-key") || normalized.contains("local-development")
                || normalized.equals("sk-cliproxy-pbl6-local-key")) {
            throw new IllegalStateException("Production configuration must not use a development secret: " + property);
        }
        if (value.length() < 32) {
            throw new IllegalStateException("Production secret must have at least 32 characters: " + property);
        }
    }

    private boolean isExactHttpsOrigin(String origin) {
        try {
            URI uri = URI.create(origin);
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme()) && host != null
                    && !host.equalsIgnoreCase("localhost") && !host.equals("127.0.0.1")
                    && !host.equals("[::1]") && uri.getUserInfo() == null
                    && (uri.getRawPath() == null || uri.getRawPath().isEmpty())
                    && uri.getQuery() == null && uri.getFragment() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
