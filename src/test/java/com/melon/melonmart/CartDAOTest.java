package com.melon.melonmart;

import com.melon.melonmart.dao.CartDAO;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartDAOTest {
    private JdbcDataSource dataSource;
    private CartDAO cartDAO;

    @BeforeEach
    void setUp() throws Exception {
        String databaseName = "cart_" + UUID.randomUUID().toString().replace("-", "");
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE products (
                        id INT PRIMARY KEY,
                        stock_qty INT NOT NULL CHECK (stock_qty >= 0)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE cart_items (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT NOT NULL,
                        product_id INT NOT NULL,
                        quantity INT NOT NULL CHECK (quantity > 0),
                        CONSTRAINT uq_cart_user_product UNIQUE (user_id, product_id)
                    )
                    """);
            statement.execute("INSERT INTO products (id, stock_qty) VALUES (1, 5), (2, 0)");
        }
        cartDAO = new CartDAO(dataSource);
    }

    @Test
    void addingSameProductIncrementsExistingQuantity() throws Exception {
        assertTrue(cartDAO.addToCart(7, 1, 1));
        assertTrue(cartDAO.addToCart(7, 1, 2));
        assertEquals(3, quantityFor(7, 1));
    }

    @Test
    void addRejectsQuantityAboveAvailableStock() throws Exception {
        assertTrue(cartDAO.addToCart(7, 1, 4));
        assertFalse(cartDAO.addToCart(7, 1, 2));
        assertEquals(4, quantityFor(7, 1));
    }

    @Test
    void addRejectsProductWithNoStock() {
        assertFalse(cartDAO.addToCart(7, 2, 1));
    }

    @Test
    void updateQuantitySetsAbsoluteQuantityAndChecksStock() throws Exception {
        assertTrue(cartDAO.addToCart(7, 1, 2));
        assertTrue(cartDAO.updateQuantity(7, 1, 4));
        assertEquals(4, quantityFor(7, 1));

        assertFalse(cartDAO.updateQuantity(7, 1, 6));
        assertEquals(4, quantityFor(7, 1));
    }

    private int quantityFor(int userId, int productId) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT quantity FROM cart_items WHERE user_id = ? AND product_id = ?")) {
            statement.setInt(1, userId);
            statement.setInt(2, productId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new AssertionError("Cart item not found");
                }
                return result.getInt("quantity");
            }
        }
    }
}
