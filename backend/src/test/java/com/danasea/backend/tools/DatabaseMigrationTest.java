package com.danasea.backend.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DatabaseMigrationTest {
    @Test
    void configuresClasspathMigrationsWithDestructiveOperationsDisabled() {
        var configuration = DatabaseMigration.configure(Map.of(
                "FLYWAY_URL", "jdbc:postgresql://postgres:5432/app",
                "FLYWAY_USER", "migration_owner", "FLYWAY_PASSWORD", "test-only-password"));
        assertThat(configuration.isCleanDisabled()).isTrue();
        assertThat(configuration.isBaselineOnMigrate()).isFalse();
        assertThat(configuration.getLocations()[0].getDescriptor()).isEqualTo("classpath:db/migration");
    }

    @Test
    void missingCredentialsFailWithoutPrintingOtherValues() {
        assertThatThrownBy(() -> DatabaseMigration.configure(Map.of("FLYWAY_PASSWORD", "private-test-value")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing migration configuration: FLYWAY_URL")
                .hasMessageNotContaining("private-test-value");
    }
}
