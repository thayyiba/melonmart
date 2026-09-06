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

    // =========================
    // GET USER CART
    // =========================

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
                PreparedStatement statement = connection.prepareStatement(sql)
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

    // =========================
    // ADD / UPDATE CART ITEM
    // =========================

    public boolean addToCart(
            int userId,
            int productId,
            int quantity
    ) {

        String sql = """
                MERGE INTO cart_items (user_id, product_id, quantity)
                KEY (user_id, product_id)
                VALUES (?, ?, ?)
                """;

        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setInt(1, userId);
            statement.setInt(2, productId);
            statement.setInt(3, quantity);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            System.err.println("Error adding item to cart:");
            e.printStackTrace();

            return false;
        }
    }

    // =========================
    // SET EXACT QUANTITY
    // =========================

    public boolean updateQuantity(
            int userId,
            int productId,
            int quantity
    ) {

        String sql = """
                UPDATE cart_items
                SET quantity = ?
                WHERE user_id = ?
                  AND product_id = ?
                """;

        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setInt(1, quantity);
            statement.setInt(2, userId);
            statement.setInt(3, productId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            System.err.println("Error updating cart quantity:");
            e.printStackTrace();

            return false;
        }
    }

    // =========================
    // REMOVE ONE ITEM
    // =========================

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
                PreparedStatement statement = connection.prepareStatement(sql)
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

    // =========================
    // CLEAR CART
    // =========================

    public boolean clearCart(int userId) {

        String sql = """
                DELETE FROM cart_items
                WHERE user_id = ?
                """;

        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
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
}