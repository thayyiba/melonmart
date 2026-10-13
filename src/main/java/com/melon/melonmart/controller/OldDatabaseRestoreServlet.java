package com.melon.melonmart.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.math.BigDecimal;

/**
 * One-time, opt-in restoration endpoint. It is disabled unless a strong
 * MELONMART_RESTORE_OLD_DB_KEY is configured in the deployment environment.
 */
@WebServlet("/api/v1/restore/old-database")
public class OldDatabaseRestoreServlet extends HttpServlet {
    private static final Gson GSON = new Gson();
    private static final int MAX_BODY_CHARS = 8 * 1024 * 1024;
    private static final List<String> INSERT_ORDER = List.of(
            "users", "products", "orders", "order_items",
            "cart_items", "reviews", "wishlist", "delivery_addresses");
    private static final List<String> DELETE_ORDER = List.of(
            "delivery_addresses", "wishlist", "reviews", "cart_items",
            "order_items", "orders", "products", "users");
    private static final Map<String, List<String>> ALLOWED_COLUMNS = allowedColumns();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String configuredKey = System.getenv("MELONMART_RESTORE_OLD_DB_KEY");
        String suppliedKey = request.getHeader("X-MelonMart-Restore-Key");
        if (configuredKey == null || configuredKey.length() < 32
                || suppliedKey == null
                || !MessageDigest.isEqual(configuredKey.getBytes(StandardCharsets.UTF_8),
                                           suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"success\":false,\"message\":\"Not found.\"}");
            return;
        }

        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                if (body.length() + read > MAX_BODY_CHARS) {
                    response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                    response.getWriter().write("{\"success\":false,\"message\":\"Restore payload too large.\"}");
                    return;
                }
                body.append(buffer, 0, read);
            }
        }

        try {
            JsonObject payload = GSON.fromJson(body.toString(), JsonObject.class);
            if (payload == null || !payload.has("replaceAllData")
                    || !payload.get("replaceAllData").getAsBoolean()
                    || !payload.has("tables") || !payload.get("tables").isJsonObject()) {
                sendError(response, 400, "Explicit replaceAllData confirmation and tables are required.");
                return;
            }
            JsonObject tables = payload.getAsJsonObject("tables");
            for (String table : INSERT_ORDER) {
                if (!tables.has(table) || !tables.get(table).isJsonArray()) {
                    sendError(response, 400, "Missing table data: " + table);
                    return;
                }
            }
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String table : INSERT_ORDER) {
                counts.put(table, tables.getAsJsonArray(table).size());
            }
            if (counts.get("users") == 0 || counts.get("products") == 0) {
                sendError(response, 400, "Refusing to restore an empty users or products table.");
                return;
            }
            boolean hasAdmin = false;
            for (JsonElement userElement : tables.getAsJsonArray("users")) {
                if (!userElement.isJsonObject()) {
                    sendError(response, 400, "Invalid user row in restore payload.");
                    return;
                }
                JsonObject user = userElement.getAsJsonObject();
                if (user.has("role") && !user.get("role").isJsonNull()
                        && "ADMIN".equalsIgnoreCase(user.get("role").getAsString())) {
                    hasAdmin = true;
                    break;
                }
            }
            if (!hasAdmin) {
                sendError(response, 400, "Refusing to restore data without an ADMIN account.");
                return;
            }

            try (Connection connection = com.melon.melonmart.listener.DbContextListener.getDataSource().getConnection()) {
                connection.setAutoCommit(false);
                try {
                    try (Statement statement = connection.createStatement()) {
                        for (String table : DELETE_ORDER) statement.executeUpdate("DELETE FROM " + table);
                    }
                    for (String table : INSERT_ORDER) {
                        insertRows(connection, table, tables.getAsJsonArray(table));
                    }
                    connection.commit();
                } catch (Exception e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }

                // Restore auto-increment counters only after all rows have committed.
                try (Statement statement = connection.createStatement()) {
                    for (String table : INSERT_ORDER) {
                        statement.execute("ALTER TABLE " + table + " ALTER COLUMN id RESTART WITH "
                                + (maxId(connection, table) + 1));
                    }
                }
            }
            JsonObject out = new JsonObject();
            out.addProperty("success", true);
            out.addProperty("message", "Old database restored.");
            out.add("counts", GSON.toJsonTree(counts));
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(GSON.toJson(out));
        } catch (Exception e) {
            // Do not log payloads, database rows, keys, or personal data.
            getServletContext().log("Old database restoration failed.", e);
            sendError(response, 500, "Restoration failed; check application logs.");
        }
    }

    private static void insertRows(Connection connection, String table, JsonArray rows) throws Exception {
        List<String> allowed = ALLOWED_COLUMNS.get(table);
        for (JsonElement rowElement : rows) {
            if (!rowElement.isJsonObject()) throw new IllegalArgumentException("Invalid row in " + table);
            JsonObject row = rowElement.getAsJsonObject();
            List<String> columns = new ArrayList<>();
            for (String column : allowed) if (row.has(column)) columns.add(column);
            if (columns.isEmpty()) throw new IllegalArgumentException("No known columns in " + table + " row");
            String names = String.join(", ", columns);
            String marks = String.join(", ", java.util.Collections.nCopies(columns.size(), "?"));
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO " + table + " (" + names + ") VALUES (" + marks + ")")) {
                for (int i = 0; i < columns.size(); i++) {
                    JsonElement value = row.get(columns.get(i));
                    setValue(statement, i + 1, columns.get(i), value);
                }
                statement.executeUpdate();
            }
        }
    }

    private static void setValue(PreparedStatement statement, int index, String column, JsonElement value)
            throws Exception {
        if (value == null || value.isJsonNull()) {
            statement.setObject(index, null);
        } else if (column.endsWith("_at") && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String text = value.getAsString();
            statement.setTimestamp(index, Timestamp.valueOf(text.replace('T', ' ')));
        } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
            statement.setBoolean(index, value.getAsBoolean());
        } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            statement.setBigDecimal(index, new BigDecimal(value.getAsString()));
        } else if (value.isJsonPrimitive()) {
            statement.setString(index, value.getAsString());
        } else {
            throw new IllegalArgumentException("Nested values are not allowed.");
        }
    }

    private static int maxId(Connection connection, String table) throws Exception {
        try (Statement statement = connection.createStatement();
             java.sql.ResultSet result = statement.executeQuery("SELECT COALESCE(MAX(id), 0) FROM " + table)) {
            result.next();
            return result.getInt(1);
        }
    }

    private static Map<String, List<String>> allowedColumns() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("users", Arrays.asList("id","name","email","password_hash","role","created_at"));
        map.put("products", Arrays.asList("id","seller_id","name","description","price","stock_qty","category","image_url","created_at"));
        map.put("orders", Arrays.asList("id","buyer_id","status","total_amount","created_at","delivery_name","delivery_email","delivery_phone","delivery_address","delivery_city","delivery_state","delivery_pincode","delivery_landmark","payment_method"));
        map.put("order_items", Arrays.asList("id","order_id","product_id","quantity","unit_price","created_at"));
        map.put("cart_items", Arrays.asList("id","user_id","product_id","quantity","created_at"));
        map.put("reviews", Arrays.asList("id","product_id","user_id","order_id","rating","comment","created_at"));
        map.put("wishlist", Arrays.asList("id","user_id","product_id","created_at"));
        map.put("delivery_addresses", Arrays.asList("id","user_id","label","address","city","state","pincode","landmark","latitude","longitude","accuracy","is_default","created_at","updated_at"));
        return map;
    }

    private static void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        JsonObject out = new JsonObject();
        out.addProperty("success", false);
        out.addProperty("message", message);
        response.getWriter().write(GSON.toJson(out));
    }
}
