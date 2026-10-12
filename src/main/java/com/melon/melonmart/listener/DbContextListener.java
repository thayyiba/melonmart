package com.melon.melonmart.listener;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;

@WebListener
public class DbContextListener implements ServletContextListener {
    private static HikariDataSource dataSource;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            Properties props = new Properties();
            try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("db.properties")) {
                if (input == null) throw new RuntimeException("db.properties not found in src/main/resources");
                props.load(input);
            }

            HikariConfig config = new HikariConfig();
            config.setDriverClassName(props.getProperty("db.driver"));
            // Use the configured URL exactly. Never redirect the application to another DB location.
            String jdbcUrl = resolveJdbcUrl(props);
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(environmentOrProperty("MELONMART_DB_USERNAME", props, "db.username"));
            config.setPassword(environmentOrProperty("MELONMART_DB_PASSWORD", props, "db.password"));
            config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.maxSize", "10")));
            config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.minIdle", "2")));
            config.setIdleTimeout(Long.parseLong(props.getProperty("db.pool.idleTimeout", "30000")));
            config.setConnectionTimeout(Long.parseLong(props.getProperty("db.pool.connTimeout", "10000")));

            dataSource = new HikariDataSource(config);
            System.out.println("=================================");
            System.out.println("MelonMart H2 Database Connected!");
            System.out.println("Using configured H2 database URL (value omitted).");
            System.out.println("=================================");

            // Only ensure missing tables exist. Do not rewrite existing users, passwords, roles, orders, or data.
            runSchema();
        } catch (Exception e) {
            System.err.println("Failed to initialize database.");
            e.printStackTrace();
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private String environmentOrProperty(String environmentName, Properties properties, String propertyName) {
        String value = System.getenv(environmentName);
        return value == null || value.isBlank()
                ? properties.getProperty(propertyName, "")
                : value;
    }

    /**
     * Allows deployment configuration to override the local database URL.
     * The historical Windows-only default is mapped to a local data directory
     * on non-Windows hosts; on Windows, the existing database location remains
     * unchanged to avoid silently switching the developer's database.
     */
    private String resolveJdbcUrl(Properties properties) throws IOException {
        String url = System.getenv("MELONMART_DB_URL");
        if (url == null || url.isBlank()) {
            url = properties.getProperty("db.url");
        }
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Configure MELONMART_DB_URL or db.url");
        }

        String legacyUrlPrefix = "jdbc:h2:file:C:/apache-tomcat-9.0.121/bin/data/melonmartdb";
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")
                && url.startsWith(legacyUrlPrefix)) {
            Path base = Paths.get(System.getProperty(
                    "catalina.base", System.getProperty("user.dir")));
            Path dataDirectory = base.resolve("data");
            Files.createDirectories(dataDirectory);
            url = "jdbc:h2:file:" + dataDirectory.resolve("melonmartdb").toAbsolutePath().normalize()
                    + url.substring(legacyUrlPrefix.length());
        }

        // H2 does not create missing parent directories for file databases.
        if (url.startsWith("jdbc:h2:file:")) {
            String filePart = url.substring("jdbc:h2:file:".length()).split(";", 2)[0];
            if (!filePart.startsWith("~/")) {
                Path databaseFile = Paths.get(filePart).toAbsolutePath().normalize();
                Path parent = databaseFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
            }
        }
        return url;
    }

    private void runSchema() {
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("schema.sql")) {
            if (input == null) throw new RuntimeException("schema.sql not found in src/main/resources");
            StringBuilder sql = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("--")) continue;
                    sql.append(line).append("\n");
                }
            }
            String[] statements = sql.toString().split(";");
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                for (String query : statements) {
                    query = query.trim();
                    if (!query.isEmpty()) statement.execute(query);
                }
            }
            System.out.println("MelonMart database schema initialized successfully.");
        } catch (Exception e) {
            System.err.println("Failed to initialize database schema.");
            e.printStackTrace();
            throw new RuntimeException("Schema initialization failed", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("MelonMart Database Connection Closed.");
        }
    }

    public static HikariDataSource getDataSource() { return dataSource; }
}
