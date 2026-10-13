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
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import org.mindrot.jbcrypt.BCrypt;

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

            // Apply versioned, idempotent DDL before ensuring the complete schema and demo data.
            runMigrations();
            runSchema();
            runSeed();
            assignUnownedProductsToDemoSellerOnce();
            resetDemoPasswordsOnce();
            resetBootstrapAdminPasswordOnce();
            ensureBootstrapAdmin();
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

    private void runMigrations() throws Exception {
        String[] migrations = {
                "db/migrations/V1__init_schema.sql",
                "db/migrations/V2__wishlist.sql",
                "db/migrations/V3__checkout_details.sql",
                "db/migrations/V4__delivery_addresses.sql"
        };

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS schema_migrations (
                        version VARCHAR(50) PRIMARY KEY,
                        applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            for (String resource : migrations) {
                String filename = resource.substring(resource.lastIndexOf('/') + 1);
                String version = filename.substring(0, filename.indexOf("__"));
                boolean applied;
                try (java.sql.PreparedStatement check = connection.prepareStatement(
                        "SELECT 1 FROM schema_migrations WHERE version = ?")) {
                    check.setString(1, version);
                    try (java.sql.ResultSet result = check.executeQuery()) {
                        applied = result.next();
                    }
                }

                if (!applied) {
                    for (String sql : loadSqlStatements(resource)) {
                        // The V1 migration historically also contained demo inserts.
                        // Skip those known demo statements when production seeding is disabled.
                        if (!isDemoSeedEnabled() && isDemoSeedStatement(sql)) {
                            continue;
                        }
                        statement.execute(sql);
                    }
                    try (java.sql.PreparedStatement record = connection.prepareStatement(
                            "INSERT INTO schema_migrations (version) VALUES (?)")) {
                        record.setString(1, version);
                        record.executeUpdate();
                    }
                }
            }
        }
    }

    private void runSchema() throws Exception {
        executeSqlResource("schema.sql");
        System.out.println("MelonMart database schema initialized successfully.");
    }

    private boolean isDemoSeedEnabled() {
        String configured = System.getenv("MELONMART_SEED_DEMO_DATA");
        // Preserve local development behavior unless explicitly disabled.
        return configured == null || configured.isBlank()
                || Boolean.parseBoolean(configured);
    }

    private boolean isDemoSeedStatement(String sql) {
        String normalized = sql.toLowerCase(Locale.ROOT);
        boolean demoAccountInsert = normalized.startsWith("insert into users")
                && (normalized.contains("admin@melonmart.com")
                || normalized.contains("buyer@melonmart.com")
                || normalized.contains("seller@melonmart.com"));
        boolean demoOrderInsert = (normalized.startsWith("insert into orders")
                || normalized.startsWith("insert into order_items")
                || normalized.startsWith("insert into reviews"))
                && (normalized.contains("buyer@melonmart.com")
                || normalized.contains("seller@melonmart.com"));
        // Product inserts can reference the optional demo seller but must still run
        // when demo accounts are disabled, so the public catalog is never empty.
        return demoAccountInsert || demoOrderInsert;
    }

    private void runSeed() throws Exception {
        if (!isDemoSeedEnabled()) {
            // Catalog products are safe public content and do not create demo logins.
            // This idempotent catalog seed also fills databases whose migrations were
            // applied earlier while production demo seeding was disabled.
            executeSqlResource("catalog.sql");
            System.out.println("MelonMart public catalog seed checked successfully.");
            System.out.println("Demo accounts and demo orders remain disabled.");
            return;
        }
        executeSqlResource("seed.sql");
        System.out.println("MelonMart demo seed data checked successfully.");
    }

    /**
     * Assigns only legacy catalog products without an owner to the existing demo seller.
     * Products already owned by a real seller are never changed. The migration marker
     * makes this a one-time repair and leaves future products untouched.
     */
    private void assignUnownedProductsToDemoSellerOnce() throws Exception {
        final String version = "UNOWNED_PRODUCTS_TO_DEMO_SELLER_20261013";
        try (Connection connection = dataSource.getConnection()) {
            try (java.sql.PreparedStatement check = connection.prepareStatement(
                    "SELECT 1 FROM schema_migrations WHERE version = ?")) {
                check.setString(1, version);
                try (java.sql.ResultSet result = check.executeQuery()) {
                    if (result.next()) {
                        return;
                    }
                }
            }

            // Prefer the designated catalog seller when present. If it is absent,
            // use an existing real SELLER account instead of leaving legacy products
            // unowned. Never promote a buyer/admin or overwrite another seller's products.
            Integer sellerId = null;
            try (java.sql.PreparedStatement seller = connection.prepareStatement(
                    "SELECT id FROM users WHERE UPPER(role) = 'SELLER' " +
                    "ORDER BY CASE WHEN LOWER(email) = ? THEN 0 ELSE 1 END, id ASC LIMIT 1")) {
                seller.setString(1, "seller@melonmart.com");
                try (java.sql.ResultSet result = seller.executeQuery()) {
                    if (result.next()) {
                        sellerId = result.getInt("id");
                    }
                }
            }

            if (sellerId == null) {
                System.out.println("Unowned catalog products left unchanged: no SELLER account exists.");
                return;
            }

            int updated;
            try (java.sql.PreparedStatement update = connection.prepareStatement(
                    "UPDATE products SET seller_id = ? WHERE seller_id IS NULL")) {
                update.setInt(1, sellerId);
                updated = update.executeUpdate();
            }

            try (java.sql.PreparedStatement record = connection.prepareStatement(
                    "INSERT INTO schema_migrations (version) VALUES (?)")) {
                record.setString(1, version);
                record.executeUpdate();
            }
            System.out.println("One-time catalog ownership repair completed; assigned "
                    + updated + " unowned products to the demo seller.");
        }
    }

    /**
     * Applies a one-time shared password for demo accounts when explicitly enabled.
     * The migration marker ensures restarts do not keep overwriting later password changes.
     * Do not enable on a production deployment with real users.
     */
    private void resetDemoPasswordsOnce() throws Exception {
        if (!Boolean.parseBoolean(System.getenv("MELONMART_DEMO_RESET_ALL_PASSWORDS"))) {
            return;
        }

        String password = System.getenv("MELONMART_DEMO_RESET_PASSWORD");
        if (password == null || password.isEmpty()) {
            throw new IllegalStateException(
                    "MELONMART_DEMO_RESET_PASSWORD is required for the one-time demo reset.");
        }

        String marker = "DEMO_PASSWORD_RESET_20261013";
        try (Connection connection = dataSource.getConnection()) {
            try (java.sql.PreparedStatement check = connection.prepareStatement(
                    "SELECT 1 FROM schema_migrations WHERE version = ?")) {
                check.setString(1, marker);
                try (java.sql.ResultSet result = check.executeQuery()) {
                    if (result.next()) {
                        System.out.println("One-time demo password reset already applied; skipping.");
                        return;
                    }
                }
            }

            String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
            try (java.sql.PreparedStatement update = connection.prepareStatement(
                    "UPDATE users SET password_hash = ?")) {
                update.setString(1, hash);
                int affected = update.executeUpdate();
                try (java.sql.PreparedStatement record = connection.prepareStatement(
                        "INSERT INTO schema_migrations (version) VALUES (?)")) {
                    record.setString(1, marker);
                    record.executeUpdate();
                }
                System.out.println("One-time demo password reset applied to " + affected + " account(s).");
            }
        }
    }

    /**
     * One-time reset for the configured bootstrap administrator only.
     * This never changes buyer or seller passwords and will not repeat after its marker is recorded.
     */
    private void resetBootstrapAdminPasswordOnce() throws Exception {
        if (!Boolean.parseBoolean(System.getenv("MELONMART_RESET_ADMIN_PASSWORD_ONCE"))) {
            return;
        }

        String email = System.getenv("MELONMART_ADMIN_EMAIL");
        String password = System.getenv("MELONMART_ADMIN_PASSWORD");
        if (email == null || email.isBlank() || password == null || password.length() < 12) {
            throw new IllegalStateException(
                    "Admin one-time reset requires MELONMART_ADMIN_EMAIL and a MELONMART_ADMIN_PASSWORD of at least 12 characters.");
        }

        String marker = "BOOTSTRAP_ADMIN_PASSWORD_RESET_20261013";
        try (Connection connection = dataSource.getConnection()) {
            try (java.sql.PreparedStatement check = connection.prepareStatement(
                    "SELECT 1 FROM schema_migrations WHERE version = ?")) {
                check.setString(1, marker);
                try (java.sql.ResultSet result = check.executeQuery()) {
                    if (result.next()) {
                        System.out.println("One-time bootstrap admin password reset already applied; skipping.");
                        return;
                    }
                }
            }

            String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
            try (java.sql.PreparedStatement update = connection.prepareStatement(
                    "UPDATE users SET password_hash = ? WHERE LOWER(email) = LOWER(?) AND UPPER(role) = 'ADMIN'")) {
                update.setString(1, hash);
                update.setString(2, email.trim());
                int affected = update.executeUpdate();
                if (affected != 1) {
                    throw new IllegalStateException(
                            "One-time admin password reset expected exactly one matching administrator, found " + affected + ".");
                }
                try (java.sql.PreparedStatement record = connection.prepareStatement(
                        "INSERT INTO schema_migrations (version) VALUES (?)")) {
                    record.setString(1, marker);
                    record.executeUpdate();
                }
                System.out.println("One-time bootstrap admin password reset completed for the configured administrator.");
            }
        }
    }

    /**
     * Optionally creates the first administrator for a deployment that disables
     * demo accounts. Existing accounts are never overwritten or promoted.
     */
    private void ensureBootstrapAdmin() throws Exception {
        if (isDemoSeedEnabled()) {
            return;
        }

        String email = System.getenv("MELONMART_ADMIN_EMAIL");
        String password = System.getenv("MELONMART_ADMIN_PASSWORD");
        if ((email == null || email.isBlank())
                && (password == null || password.isBlank())) {
            return;
        }
        if (email == null || email.isBlank() || password == null || password.length() < 12) {
            throw new IllegalStateException(
                    "Set both MELONMART_ADMIN_EMAIL and MELONMART_ADMIN_PASSWORD (at least 12 characters).");
        }

        try (Connection connection = dataSource.getConnection()) {
            try (java.sql.PreparedStatement lookup = connection.prepareStatement(
                    "SELECT role FROM users WHERE email = ?")) {
                lookup.setString(1, email.trim());
                try (java.sql.ResultSet result = lookup.executeQuery()) {
                    if (result.next()) {
                        if (!"ADMIN".equalsIgnoreCase(result.getString("role"))) {
                            throw new IllegalStateException(
                                    "MELONMART_ADMIN_EMAIL already belongs to a non-admin user.");
                        }
                        System.out.println("Bootstrap administrator already exists; credentials left unchanged.");
                        return;
                    }
                }
            }

            String name = System.getenv("MELONMART_ADMIN_NAME");
            if (name == null || name.isBlank()) {
                name = "Melon Mart Admin";
            }
            String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
            try (java.sql.PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO users (name, email, password_hash, role) VALUES (?, ?, ?, 'ADMIN')")) {
                insert.setString(1, name.trim());
                insert.setString(2, email.trim());
                insert.setString(3, hash);
                insert.executeUpdate();
            }
            System.out.println("Bootstrap administrator created from environment configuration.");
        }
    }

    private void executeSqlResource(String resource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : loadSqlStatements(resource)) {
                statement.execute(sql);
            }
        }
    }

    private String[] loadSqlStatements(String resource) throws IOException {
        StringBuilder sql = new StringBuilder();
        try (InputStream input = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException(resource + " not found in src/main/resources");
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("--")) continue;
                    sql.append(line).append('\n');
                }
            }
        }

        List<String> statements = new ArrayList<>();
        for (String query : sql.toString().split(";")) {
            query = query.trim();
            if (!query.isEmpty()) statements.add(query);
        }
        return statements.toArray(new String[0]);
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
