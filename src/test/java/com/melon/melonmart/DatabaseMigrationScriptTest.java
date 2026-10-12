package com.melon.melonmart;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMigrationScriptTest {
    @Test
    void migrationsSchemaAndSeedAreIdempotent() throws Exception {
        String databaseName = "migration_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE schema_migrations (
                        version VARCHAR(50) PRIMARY KEY,
                        applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            String[] migrations = {
                    "db/migrations/V1__init_schema.sql",
                    "db/migrations/V2__wishlist.sql",
                    "db/migrations/V3__checkout_details.sql"
            };
            for (String migration : migrations) {
                for (String sql : statements(migration)) {
                    statement.execute(sql);
                }
            }
            for (String sql : statements("schema.sql")) statement.execute(sql);
            for (String sql : statements("seed.sql")) statement.execute(sql);

            // Re-running the idempotent schema and seed scripts must not duplicate demo records.
            for (String sql : statements("schema.sql")) statement.execute(sql);
            for (String sql : statements("seed.sql")) statement.execute(sql);

            assertEquals(3, count(connection, "SELECT COUNT(*) FROM users"));
            assertEquals(5, count(connection, "SELECT COUNT(*) FROM products"));
            assertTrue(count(connection,
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                            + "WHERE TABLE_NAME = 'ORDERS' AND COLUMN_NAME = 'DELIVERY_ADDRESS'") == 1);
            assertEquals(1, count(connection, "SELECT COUNT(*) FROM wishlist"));
            assertEquals(1, count(connection, "SELECT COUNT(*) FROM reviews"));
        }
    }

    private List<String> statements(String resource) throws Exception {
        StringBuilder source = new StringBuilder();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (input == null) throw new AssertionError("Missing SQL resource: " + resource);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                        source.append(trimmed).append('\n');
                    }
                }
            }
        }
        List<String> result = new ArrayList<>();
        for (String sql : source.toString().split(";")) {
            if (!sql.isBlank()) result.add(sql.trim());
        }
        return result;
    }

    private int count(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }
}
