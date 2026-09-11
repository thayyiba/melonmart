package com.melon.melonmart.dao;

import com.melon.melonmart.listener.DbContextListener;
import com.melon.melonmart.model.Product;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    private Connection getConnection() throws Exception {
        return DbContextListener.getDataSource().getConnection();
    }

    // Get all products
    public List<Product> getAllProducts() {

        List<Product> products = new ArrayList<>();

        String sql = """
                SELECT id, seller_id, name, description, price,
                       stock_qty, category, image_url, created_at
                FROM products
                ORDER BY id DESC
                """;

        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()
        ) {

            while (result.next()) {
                products.add(extractProduct(result));
            }

        } catch (Exception e) {
            System.err.println("Error getting products:");
            e.printStackTrace();
        }

        return products;
    }

    // Get products by category
    public List<Product> getProductsByCategory(String category) {

        List<Product> products = new ArrayList<>();

        String sql = """
                SELECT id, seller_id, name, description, price,
                       stock_qty, category, image_url, created_at
                FROM products
                WHERE UPPER(category) = UPPER(?)
                ORDER BY id DESC
                """;

        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, category);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {
                    products.add(extractProduct(result));
                }
            }

        } catch (Exception e) {
            System.err.println("Error getting products by category:");
            e.printStackTrace();
        }

        return products;
    }

    // Get products belonging to a seller
    public List<Product> getProductsBySeller(int sellerId) {

        List<Product> products = new ArrayList<>();

        String sql = """
                SELECT id, seller_id, name, description, price,
                       stock_qty, category, image_url, created_at
                FROM products
                WHERE seller_id = ?
                ORDER BY id DESC
                """;

        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setInt(1, sellerId);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {
                    products.add(extractProduct(result));
                }
            }

        } catch (Exception e) {
            System.err.println("Error getting seller products:");
            e.printStackTrace();
        }

        return products;
    }

    // Add a new product
    public boolean addProduct(Product product) {

        String sql = """
                INSERT INTO products
                (seller_id, name, description, price, stock_qty, category, image_url)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setInt(1, product.getSellerId());
            statement.setString(2, product.getName());
            statement.setString(3, product.getDescription());
            statement.setBigDecimal(4, product.getPrice());
            statement.setInt(5, product.getStockQty());
            statement.setString(6, product.getCategory());
            statement.setString(7, product.getImageUrl());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            System.err.println("Error adding product:");
            e.printStackTrace();
            return false;
        }
    }

    // Update a product belonging to a seller
    public boolean updateProduct(Product product) {

        String sql = """
                UPDATE products
                SET name = ?,
                    description = ?,
                    price = ?,
                    stock_qty = ?,
                    category = ?,
                    image_url = ?
                WHERE id = ?
                  AND seller_id = ?
                """;

        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, product.getName());
            statement.setString(2, product.getDescription());
            statement.setBigDecimal(3, product.getPrice());
            statement.setInt(4, product.getStockQty());
            statement.setString(5, product.getCategory());
            statement.setString(6, product.getImageUrl());
            statement.setInt(7, product.getId());
            statement.setInt(8, product.getSellerId());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            System.err.println("Error updating product:");
            e.printStackTrace();
            return false;
        }
    }

    // Delete a product belonging to a seller
    public boolean deleteProduct(int productId, int sellerId) {

        String sql = """
                DELETE FROM products
                WHERE id = ?
                  AND seller_id = ?
                """;

        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setInt(1, productId);
            statement.setInt(2, sellerId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            System.err.println("Error deleting product:");
            e.printStackTrace();
            return false;
        }
    }

    // Convert database row into Product object
    private Product extractProduct(ResultSet result) throws Exception {

        Product product = new Product();

        product.setId(result.getInt("id"));
        product.setSellerId(result.getInt("seller_id"));
        product.setName(result.getString("name"));
        product.setDescription(result.getString("description"));

        BigDecimal price = result.getBigDecimal("price");
        product.setPrice(price);

        product.setStockQty(result.getInt("stock_qty"));
        product.setCategory(result.getString("category"));
        product.setImageUrl(result.getString("image_url"));
        product.setCreatedAt(result.getTimestamp("created_at"));

        return product;
    }
}