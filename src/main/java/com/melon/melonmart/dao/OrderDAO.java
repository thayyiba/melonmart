package com.melon.melonmart.dao;

import com.melon.melonmart.listener.DbContextListener;
import com.melon.melonmart.model.Order;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    private final DataSource dataSource;

    public OrderDAO() {
        this.dataSource = DbContextListener.getDataSource();
    }

    public int createOrder(Order order) throws Exception {

        try (Connection connection = dataSource.getConnection()) {
            return createOrder(connection, order);
        }
    }

    public int createOrder(Connection connection, Order order)
            throws Exception {

        String sql = """
                INSERT INTO orders
                (user_id, total_amount, status)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(
                sql,
                java.sql.Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, order.getUserId());
            statement.setBigDecimal(2, order.getTotalAmount());
            statement.setString(3, order.getStatus());

            statement.executeUpdate();

            try (ResultSet result = statement.getGeneratedKeys()) {

                if (result.next()) {
                    return result.getInt(1);
                }
            }
        }

        return 0;
    }

    public Order getOrderById(int orderId) throws Exception {

        String sql = """
                SELECT id, user_id, total_amount, status, created_at
                FROM orders
                WHERE id = ?
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, orderId);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {

                    Order order = new Order();

                    order.setId(result.getInt("id"));
                    order.setUserId(result.getInt("user_id"));
                    order.setTotalAmount(
                            result.getBigDecimal("total_amount")
                    );
                    order.setStatus(result.getString("status"));
                    order.setCreatedAt(
                            result.getTimestamp("created_at")
                    );

                    return order;
                }
            }
        }

        return null;
    }

    public List<Order> getOrdersByUserId(int userId) throws Exception {

        String sql = """
                SELECT id, user_id, total_amount, status, created_at
                FROM orders
                WHERE user_id = ?
                ORDER BY created_at DESC
                """;

        List<Order> orders = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    Order order = new Order();

                    order.setId(result.getInt("id"));
                    order.setUserId(result.getInt("user_id"));
                    order.setTotalAmount(
                            result.getBigDecimal("total_amount")
                    );
                    order.setStatus(result.getString("status"));
                    order.setCreatedAt(
                            result.getTimestamp("created_at")
                    );

                    orders.add(order);
                }
            }
        }

        return orders;
    }

    public List<Order> getAllOrders() throws Exception {

        String sql = """
                SELECT id, user_id, total_amount, status, created_at
                FROM orders
                ORDER BY created_at DESC
                """;

        List<Order> orders = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {

                Order order = new Order();

                order.setId(result.getInt("id"));
                order.setUserId(result.getInt("user_id"));
                order.setTotalAmount(
                        result.getBigDecimal("total_amount")
                );
                order.setStatus(result.getString("status"));
                order.setCreatedAt(
                        result.getTimestamp("created_at")
                );

                orders.add(order);
            }
        }

        return orders;
    }

    public void updateOrderStatus(int orderId, String status)
            throws Exception {

        String sql = """
                UPDATE orders
                SET status = ?
                WHERE id = ?
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status);
            statement.setInt(2, orderId);

            statement.executeUpdate();
        }
    }
}