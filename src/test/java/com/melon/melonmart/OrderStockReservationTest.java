package com.melon.melonmart;

import com.melon.melonmart.dao.OrderDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStockReservationTest {
    private Connection connection;
    private OrderDAO orderDAO;

    @BeforeEach
    void setUp() throws Exception {
        String databaseName = "stock_" + UUID.randomUUID().toString().replace("-", "");
        connection = DriverManager.getConnection(
                "jdbc:h2:mem:" + databaseName,
                "sa",
                ""
        );
        connection.createStatement().execute(
                "CREATE TABLE products (id INT PRIMARY KEY, stock_qty INT NOT NULL CHECK (stock_qty >= 0))"
        );
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO products (id, stock_qty) VALUES (?, ?)"
        )) {
            statement.setInt(1, 1);
            statement.setInt(2, 5);
            statement.executeUpdate();
        }
        orderDAO = new OrderDAO();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void reserveStockDecreasesAvailableQuantity() throws Exception {
        assertTrue(orderDAO.reserveStock(connection, 1, 3));
        assertEquals(2, stockForProduct(1));
    }

    @Test
    void reserveStockRejectsQuantityAboveAvailableStock() throws Exception {
        assertFalse(orderDAO.reserveStock(connection, 1, 6));
        assertEquals(5, stockForProduct(1));
    }

    @Test
    void reserveStockRejectsUnknownProduct() throws Exception {
        assertFalse(orderDAO.reserveStock(connection, 99, 1));
    }

    private int stockForProduct(int productId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT stock_qty FROM products WHERE id = ?"
        )) {
            statement.setInt(1, productId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new AssertionError("Product not found: " + productId);
                }
                return result.getInt("stock_qty");
            }
        }
    }
}
