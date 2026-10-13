package com.melon.melonmart.tools;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

/**
 * Exports the local H2 database in memory and sends it over HTTPS to the
 * opt-in restore endpoint. No database file is uploaded or copied to GitHub.
 *
 * Required environment variables:
 * MELONMART_OLD_DB_URL, MELONMART_RESTORE_TARGET_URL,
 * MELONMART_RESTORE_OLD_DB_KEY
 *
 * The program refuses to run without the explicit --replace-all-data argument.
 */
public final class OldDatabaseRestoreClient {
    private static final Gson GSON = new Gson();
    private static final List<String> TABLES = List.of(
            "users", "products", "orders", "order_items",
            "cart_items", "reviews", "wishlist", "delivery_addresses");

    private OldDatabaseRestoreClient() { }

    public static void main(String[] args) throws Exception {
        boolean checkOnly = args.length == 1 && "--check-only".equals(args[0]);
        boolean replaceAll = args.length == 1 && "--replace-all-data".equals(args[0]);
        if (!checkOnly && !replaceAll) {
            throw new IllegalArgumentException(
                    "Use --check-only to validate the source without sending data, or --replace-all-data only after backup.");
        }
        String oldUrl = required("MELONMART_OLD_DB_URL");
        String target = required("MELONMART_RESTORE_TARGET_URL");
        String key = required("MELONMART_RESTORE_OLD_DB_KEY");
        if (!target.startsWith("https://")) {
            throw new IllegalArgumentException("Restore target must use HTTPS.");
        }
        if (!target.endsWith("/api/v1/restore/old-database")) {
            throw new IllegalArgumentException("Target must end with /api/v1/restore/old-database.");
        }
        if (key.length() < 32) throw new IllegalArgumentException("Restore key must be at least 32 characters.");

        JsonObject payload = new JsonObject();
        payload.addProperty("replaceAllData", true);
        JsonObject tables = new JsonObject();
        Map<String, Integer> counts = new LinkedHashMap<>();
        try (Connection connection = DriverManager.getConnection(oldUrl)) {
            for (String table : TABLES) {
                JsonArray rows = readTable(connection, table);
                tables.add(table, rows);
                counts.put(table, rows.size());
            }
        }
        if (counts.get("users") == 0 || counts.get("products") == 0) {
            throw new IllegalStateException("Old database has no users or products; refusing to send it.");
        }
        int adminCount = 0;
        for (com.google.gson.JsonElement userElement : tables.getAsJsonArray("users")) {
            JsonObject user = userElement.getAsJsonObject();
            if (user.has("role") && !user.get("role").isJsonNull()
                    && "ADMIN".equalsIgnoreCase(user.get("role").getAsString())) {
                adminCount++;
            }
        }
        if (adminCount == 0) {
            throw new IllegalStateException(
                    "Old database has no ADMIN account; refusing to replace live data and risk losing admin access.");
        }
        System.out.println("Source check passed: administrator accounts found = " + adminCount
                + " (no email addresses or password data printed).");
        payload.add("tables", tables);
        byte[] bytes = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 8 * 1024 * 1024) {
            throw new IllegalStateException("Export exceeds 8 MiB safety limit.");
        }
        if (checkOnly) {
            System.out.println("PRECHECK PASSED — no data was sent and no destination was changed.");
            System.out.println("Source table counts: " + GSON.toJson(counts));
            System.out.println("Serialized payload size: " + bytes.length + " bytes.");
            return;
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(target).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("X-MelonMart-Restore-Key", key);
        connection.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream out = connection.getOutputStream()) {
            out.write(bytes);
        }
        int status = connection.getResponseCode();
        InputStream responseStream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String responseBody = "";
        if (responseStream != null) {
            try (InputStream in = responseStream) {
                responseBody = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        connection.disconnect();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("Restore endpoint returned HTTP " + status + ": " + responseBody);
        }
        System.out.println("Restore response: " + responseBody);
        System.out.println("Source table counts (no personal data printed): " + GSON.toJson(counts));
    }

    private static JsonArray readTable(Connection connection, String table) throws Exception {
        JsonArray rows = new JsonArray();
        try (java.sql.Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT * FROM " + table)) {
            ResultSetMetaData meta = result.getMetaData();
            while (result.next()) {
                JsonObject row = new JsonObject();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    String name = meta.getColumnLabel(i).toLowerCase(java.util.Locale.ROOT);
                    Object value = result.getObject(i);
                    if (value == null) {
                        row.add(name, com.google.gson.JsonNull.INSTANCE);
                    } else if (value instanceof Timestamp) {
                        row.add(name, new JsonPrimitive(value.toString()));
                    } else if (value instanceof BigDecimal) {
                        row.add(name, new JsonPrimitive((BigDecimal) value));
                    } else if (value instanceof Boolean) {
                        row.add(name, new JsonPrimitive((Boolean) value));
                    } else if (value instanceof Number) {
                        row.add(name, new JsonPrimitive((Number) value));
                    } else {
                        row.add(name, new JsonPrimitive(value.toString()));
                    }
                }
                rows.add(row);
            }
        }
        return rows;
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required environment variable is missing: " + name);
        }
        return value;
    }
}
