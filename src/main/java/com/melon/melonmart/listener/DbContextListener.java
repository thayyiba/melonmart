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

            InputStream input = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("db.properties");

            if (input == null) {
                throw new RuntimeException(
                        "db.properties not found in src/main/resources"
                );
            }

            props.load(input);
            input.close();

            HikariConfig config = new HikariConfig();

            // Database settings
            config.setDriverClassName(
                    props.getProperty("db.driver")
            );

            config.setJdbcUrl(
                    props.getProperty("db.url")
            );

            config.setUsername(
                    props.getProperty("db.username")
            );

            config.setPassword(
                    props.getProperty("db.password")
            );

            // Connection pool settings
            config.setMaximumPoolSize(
                    Integer.parseInt(
                            props.getProperty("db.pool.maxSize", "10")
                    )
            );

            config.setMinimumIdle(
                    Integer.parseInt(
                            props.getProperty("db.pool.minIdle", "2")
                    )
            );

            config.setIdleTimeout(
                    Long.parseLong(
                            props.getProperty("db.pool.idleTimeout", "30000")
                    )
            );

            config.setConnectionTimeout(
                    Long.parseLong(
                            props.getProperty("db.pool.connTimeout", "10000")
                    )
            );

            // Create database connection pool
            dataSource = new HikariDataSource(config);

            System.out.println("=================================");
            System.out.println("MelonMart H2 Database Connected!");
            System.out.println("=================================");

            // Initialize database tables and demo data
            runSchema();

        } catch (Exception e) {

            System.err.println("Failed to initialize database.");
            e.printStackTrace();

            throw new RuntimeException(
                    "Database initialization failed",
                    e
            );
        }
    }

    private void runSchema() {

        try (
                InputStream input = Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream("schema.sql")
        ) {

            if (input == null) {
                throw new RuntimeException(
                        "schema.sql not found in src/main/resources"
                );
            }

            StringBuilder sql = new StringBuilder();

            try (
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(
                                    input,
                                    StandardCharsets.UTF_8
                            )
                    )
            ) {

                String line;

                while ((line = reader.readLine()) != null) {

                    line = line.trim();

                    // Ignore blank lines and SQL comments
                    if (line.isEmpty() || line.startsWith("--")) {
                        continue;
                    }

                    sql.append(line).append("\n");
                }
            }

            // Split schema into individual SQL statements
            String[] statements = sql.toString().split(";");

            try (
                    Connection connection = dataSource.getConnection();
                    Statement statement = connection.createStatement()
            ) {

                for (String query : statements) {

                    query = query.trim();

                    if (!query.isEmpty()) {
                        statement.execute(query);
                    }
                }
            }

            System.out.println(
                    "MelonMart database schema initialized successfully."
            );

        } catch (Exception e) {

            System.err.println(
                    "Failed to initialize database schema."
            );

            e.printStackTrace();

            throw new RuntimeException(
                    "Schema initialization failed",
                    e
            );
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {

        if (dataSource != null && !dataSource.isClosed()) {

            dataSource.close();

            System.out.println(
                    "MelonMart Database Connection Closed."
            );
        }
    }

    public static HikariDataSource getDataSource() {
        return dataSource;
    }
}