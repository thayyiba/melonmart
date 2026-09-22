package com.melon.melonmart.listener;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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
            config.setJdbcUrl(props.getProperty("db.url"));
            config.setUsername(props.getProperty("db.username"));
            config.setPassword(props.getProperty("db.password"));
            config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.maxSize", "10")));
            config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.minIdle", "2")));
            config.setIdleTimeout(Long.parseLong(props.getProperty("db.pool.idleTimeout", "30000")));
            config.setConnectionTimeout(Long.parseLong(props.getProperty("db.pool.connTimeout", "10000")));

            dataSource = new HikariDataSource(config);
            System.out.println("=================================");
            System.out.println("MelonMart H2 Database Connected!");
            System.out.println("Using configured database: " + props.getProperty("db.url"));
            System.out.println("=================================");

            // Only ensure missing tables exist. Do not rewrite existing users, passwords, roles, orders, or data.
            runSchema();
        } catch (Exception e) {
            System.err.println("Failed to initialize database.");
            e.printStackTrace();
            throw new RuntimeException("Database initialization failed", e);
        }
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
