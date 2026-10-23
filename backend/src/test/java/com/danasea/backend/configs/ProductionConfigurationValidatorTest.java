package com.danasea.backend.configs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.danasea.backend.configs.properties.CheckinProperties;
import com.danasea.backend.configs.properties.CloudinaryProperties;
import com.danasea.backend.configs.properties.CorsProperties;
import com.danasea.backend.configs.properties.PaymentProperties;
import com.danasea.backend.modules.ai.infrastructure.cliproxy.configs.CliProxyProperties;
import com.danasea.backend.security.authentication.infrastructure.security.JwtProperties;

class ProductionConfigurationValidatorTest {
    private static final String KEY = "d9f317e42d8c1647ef793728e9be7118ca9d6ecb7c21914be7804e96b3587a6d";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withPropertyValues(
                    "spring.profiles.active=prod",
                    "cloudinary.cloud-name=danasea",
                    "cloudinary.api-key=valid-cloud-key",
                    "cloudinary.api-secret=valid-cloud-secret",
                    "spring.mail.username=sender@danasea.test",
                    "spring.mail.password=valid-mail-password",
                    "ai.cliproxy.enabled=true",
                    "ai.cliproxy.base-url=http://cli-proxy-api:8317/v1",
                    "ai.cliproxy.api-key=proxy-" + KEY,
                    "ai.cliproxy.model=model", "ai.cliproxy.fallback-model=fallback",
                    "app.jwt.secret=" + KEY,
                    "app.jwt.access-token-minutes=15", "app.jwt.refresh-token-days=7",
                    "app.checkin.qr-secret=qr-" + KEY,
                    "app.payment.webhook-secret=webhook-" + KEY,
                    "app.cors.allowed-origins=https://danasea.tailfc3563.ts.net");

    @Test
    void acceptsConfiguredProductionSecrets() {
        runner.run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void refusesSharedSigningSecrets() {
        runner.withPropertyValues("app.checkin.qr-secret=" + KEY)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void refusesWeakProxyClientSecret() {
        runner.withPropertyValues("ai.cliproxy.api-key=short")
                .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @CsvSource({
            "app.jwt.secret,your_jwt_secret_key_minimum_256_bits",
            "app.checkin.qr-secret,danasea-secure-qr-checkin-hmac-sha256-default-key-32bytes-min!",
            "app.payment.webhook-secret,local-development-payment-webhook-secret-change-me",
            "ai.cliproxy.api-key,sk-cliproxy-pbl6-local-key"
    })
    void refusesKnownDevelopmentSecretsWithoutLeakingThem(String property, String value) {
        runner.withPropertyValues(property + "=" + value).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(
                    "Production configuration must not use a development secret: " + property);
            assertThat(context.getStartupFailure().getMessage()).doesNotContain(value);
        });
    }

    @ParameterizedTest
    @CsvSource({"http://localhost:5173", "https://*.example.com", "https://example.com/path", "http://example.com"})
    void refusesNonExactOrInsecureProductionOrigins(String origin) {
        runner.withPropertyValues("app.cors.allowed-origins=" + origin)
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @Import(ProductionConfigurationValidator.class)
    @EnableConfigurationProperties({CloudinaryProperties.class, CorsProperties.class,
            JwtProperties.class, CheckinProperties.class, CliProxyProperties.class, MailProperties.class, PaymentProperties.class})
    static class PropertiesConfiguration {
    }
}
