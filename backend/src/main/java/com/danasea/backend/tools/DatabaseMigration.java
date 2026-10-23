package com.danasea.backend.tools;

import java.util.Map;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

/** One-shot migration entrypoint using the exact SQL and libraries packaged with the release. */
public final class DatabaseMigration {
    private DatabaseMigration() {
    }

    public static void main(String[] args) {
        configure(System.getenv()).load().migrate();
    }

    static FluentConfiguration configure(Map<String, String> environment) {
        return Flyway.configure()
                .dataSource(required(environment, "FLYWAY_URL"), required(environment, "FLYWAY_USER"),
                        required(environment, "FLYWAY_PASSWORD"))
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .baselineOnMigrate(false);
    }

    private static String required(Map<String, String> environment, String key) {
        String value = environment.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing migration configuration: " + key);
        }
        return value;
    }
}
