package com.melon.melonmart.dao;

import com.melon.melonmart.listener.DbContextListener;
import com.melon.melonmart.model.CartItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CartDAO {

    private Connection getConnection() throws Exception {
        return DbContextListener.getDataSource().getConnection();
    }

    public List<CartItem> getCartByUserId(int userId) {

        List<CartItem> cart = new ArrayList<>();

        String sql = """
                SELECT
                    ci.id,
                    ci.user_id,
                    ci.product_id,
                    ci.quantity,
                    p.name,
                    p.price,
                    p.image_url
                FROM cart_items ci
                JOIN products p
                    ON ci.product_id = p.id
                WHERE ci.user_id = ?
                ORDER BY ci.id
                """;

        try (
                Connection connection = getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(1, userId);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    CartItem item = new CartItem();

                    item.setId(result.getInt("id"));
                    item.setUserId(result.getInt("user_id"));
                    item.setProductId(result.getInt("product_id"));
                    item.setQuantity(result.getInt("quantity"));

                    item.setProductName(
                            result.getString("name")
                    );

                    item.setProductPrice(
                            result.getBigDecimal("price")
                    );

                    item.setProductImage(
                            result.getString("image_url")
                    );

                    cart.add(item);
                }
            }

        } catch (Exception e) {

            System.err.println("Error getting cart:");
            e.printStackTrace();
        }

        return cart;
    }

    /**
     * Adds to an existing cart line, or creates one if absent.
     * Locks the product row while validating the requested quantity.
     */
    public boolean addToCart(int userId, int productId, int quantity) {
        if (userId <= 0 || productId <= 0 || quantity <= 0) return false;

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                int stock;
                try (PreparedStatement product = connection.prepareStatement(
                        "SELECT stock_qty FROM products WHERE id = ? FOR UPDATE")) {
                    product.setInt(1, productId);
                    try (ResultSet result = product.executeQuery()) {
                        if (!result.next()) {
                            connection.rollback();
                            return false;
                        }
                        stock = result.getInt("stock_qty");
                    }
                }

                int currentQuantity = 0;
                try (PreparedStatement existing = connection.prepareStatement(
                        "SELECT quantity FROM cart_items WHERE user_id = ? AND product_id = ?")) {
                    existing.setInt(1, userId);
                    existing.setInt(2, productId);
                    try (ResultSet result = existing.executeQuery()) {
                        if (result.next()) currentQuantity = result.getInt("quantity");
                    }
                }

                if (currentQuantity + quantity > stock) {
                    connection.rollback();
                    return false;
                }

                String sql = currentQuantity > 0
                        ? "UPDATE cart_items SET quantity = quantity + ? WHERE user_id = ? AND product_id = ?"
                        : "INSERT INTO cart_items (user_id, product_id, quantity) VALUES (?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    if (currentQuantity > 0) {
                        statement.setInt(1, quantity);
                        statement.setInt(2, userId);
                        statement.setInt(3, productId);
                    } else {
                        statement.setInt(1, userId);
                        statement.setInt(2, productId);
                        statement.setInt(3, quantity);
                    }
                    if (statement.executeUpdate() != 1) {
                        connection.rollback();
                        return false;
                    }
                }

                connection.commit();
                return true;
            } catch (Exception e) {
                try { connection.rollback(); } catch (Exception rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                System.err.println("Error adding item to cart:");
                e.printStackTrace();
                return false;
            } finally {
                try { connection.setAutoCommit(true); } catch (Exception ignored) { }
            }
        } catch (Exception e) {
            System.err.println("Error adding item to cart:");
            e.printStackTrace();
            return false;
        }
    }

    // Increase an existing cart item's quantity.
    public boolean increaseQuantity(
            int userId,
            int productId,
            int quantity
    ) {

        String sql = """
                UPDATE cart_items
                SET quantity = quantity + ?
                WHERE user_id = ?
                  AND product_id = ?
                """;

        try (
                Connection connection = getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(1, quantity);
            statement.setInt(2, userId);
            statement.setInt(3, productId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            System.err.println("Error increasing cart quantity:");
            e.printStackTrace();

            return false;
        }
    }

    /**
     * Sets a cart line to the requested quantity, validating current stock.
     */
    public boolean updateQuantity(int userId, int productId, int quantity) {
        if (userId <= 0 || productId <= 0 || quantity <= 0) return false;
        String sql = """
                UPDATE cart_items
                SET quantity = ?
                WHERE user_id = ?
                  AND product_id = ?
                  AND EXISTS (
                      SELECT 1 FROM products p
                      WHERE p.id = cart_items.product_id
                        AND p.stock_qty >= ?
                  )
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, quantity);
            statement.setInt(2, userId);
            statement.setInt(3, productId);
            statement.setInt(4, quantity);
            return statement.executeUpdate() == 1;
        } catch (Exception e) {
            System.err.println("Error updating cart quantity:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeFromCart(
            int userId,
            int productId
    ) {

        String sql = """
                DELETE FROM cart_items
                WHERE user_id = ?
                  AND product_id = ?
                """;

        try (
                Connection connection = getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(1, userId);
            statement.setInt(2, productId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            System.err.println("Error removing cart item:");
            e.printStackTrace();

            return false;
        }
    }

    public boolean clearCart(int userId) {

        String sql = """
                DELETE FROM cart_items
                WHERE user_id = ?
                """;

        try (
                Connection connection = getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(1, userId);

            statement.executeUpdate();

            return true;

        } catch (Exception e) {

            System.err.println("Error clearing cart:");
            e.printStackTrace();

            return false;
        }
    }

    public void clearCart(
            Connection connection,
            int userId
    ) throws Exception {

        String sql = """
                DELETE FROM cart_items
                WHERE user_id = ?
                """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(1, userId);

            statement.executeUpdate();
        }
    }
}