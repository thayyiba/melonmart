package com.melon.melonmart.dao;

import com.melon.melonmart.listener.DbContextListener;
import com.melon.melonmart.model.Order;

import javax.sql.DataSource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import com.melon.melonmart.dto.SellerOrderDTO;
import com.melon.melonmart.dto.SellerOrderItemDTO;
import com.melon.melonmart.dto.SellerSalesDTO;

import java.util.HashMap;
import java.util.Map;

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
                (buyer_id, total_amount, status)
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
                SELECT id, buyer_id AS user_id, total_amount, status, created_at
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
                SELECT id, buyer_id AS user_id, total_amount, status, created_at
                FROM orders
                WHERE buyer_id = ?
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
                SELECT id, buyer_id AS user_id, total_amount, status, created_at
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
    public List<SellerOrderDTO> getOrdersBySellerId(
        int sellerId
) throws Exception {

    String sql = """
            SELECT
                o.id AS order_id,
                o.buyer_id AS buyer_id,
                u.name AS buyer_name,
                u.email AS buyer_email,
                o.total_amount AS order_total,
                o.status,
                o.created_at,
                oi.product_id,
                p.name AS product_name,
                p.image_url,
                oi.quantity,
                oi.unit_price
            FROM orders o
            JOIN users u
                ON o.buyer_id = u.id
            JOIN order_items oi
                ON o.id = oi.order_id
            JOIN products p
                ON oi.product_id = p.id
            WHERE p.seller_id = ?
            ORDER BY o.created_at DESC
            """;

    Map<Integer, SellerOrderDTO> orderMap =
            new HashMap<>();

    try (
            Connection connection =
                    dataSource.getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql)
    ) {

        statement.setInt(1, sellerId);

        try (ResultSet result = statement.executeQuery()) {

            while (result.next()) {

                int orderId =
                        result.getInt("order_id");

                SellerOrderDTO order =
                        orderMap.get(orderId);

                if (order == null) {

                    order = new SellerOrderDTO();

                    order.setOrderId(orderId);

                    order.setBuyerId(
                            result.getInt("buyer_id")
                    );

                    order.setBuyerName(
                            result.getString("buyer_name")
                    );

                    order.setBuyerEmail(
                            result.getString("buyer_email")
                    );

                    order.setOrderTotal(
                            result.getBigDecimal("order_total")
                    );

                    order.setStatus(
                            result.getString("status")
                    );

                    order.setCreatedAt(
                            result.getTimestamp("created_at")
                    );

                    order.setItems(
                            new ArrayList<>()
                    );

                    orderMap.put(
                            orderId,
                            order
                    );
                }

                SellerOrderItemDTO item =
                        new SellerOrderItemDTO();

                item.setProductId(
                        result.getInt("product_id")
                );

                item.setProductName(
                        result.getString("product_name")
                );

                item.setImageUrl(
                        result.getString("image_url")
                );

                item.setQuantity(
                        result.getInt("quantity")
                );

                item.setPrice(
                        result.getBigDecimal("price")
                );

                order.getItems().add(item);
            }
        }
    }

    return new ArrayList<>(
            orderMap.values()
    );
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

    public boolean sellerOwnsOrder(
        int sellerId,
        int orderId
) throws Exception {

    String sql = """
            SELECT COUNT(*)
            FROM order_items oi
            JOIN products p
                ON oi.product_id = p.id
            WHERE oi.order_id = ?
              AND p.seller_id = ?
            """;

    try (
            Connection connection =
                    dataSource.getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql)
    ) {

        statement.setInt(1, orderId);
        statement.setInt(2, sellerId);

        try (ResultSet result =
                     statement.executeQuery()) {

            if (result.next()) {
                return result.getInt(1) > 0;
            }
        }
    }

    return false;
   }

   public SellerSalesDTO getSellerSales(
        int sellerId
) throws Exception {

    String sql = """
            SELECT
                COALESCE(
                    SUM(
                        CASE
                            WHEN o.status = 'DELIVERED'
                            THEN oi.unit_price * oi.quantity
                            ELSE 0
                        END
                    ),
                    0
                ) AS total_revenue,

                COUNT(
                    DISTINCT
                    CASE
                        WHEN o.status = 'DELIVERED'
                        THEN o.id
                    END
                ) AS total_orders,

                COALESCE(
                    SUM(
                        CASE
                            WHEN o.status = 'DELIVERED'
                            THEN oi.quantity
                            ELSE 0
                        END
                    ),
                    0
                ) AS products_sold

            FROM orders o

            JOIN order_items oi
                ON o.id = oi.order_id

            JOIN products p
                ON oi.product_id = p.id

            WHERE p.seller_id = ?
            """;

    try (
            Connection connection =
                    dataSource.getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql)
    ) {

        statement.setInt(1, sellerId);

        try (
                ResultSet result =
                        statement.executeQuery()
        ) {

            if (result.next()) {

                SellerSalesDTO sales =
                        new SellerSalesDTO();

                sales.setTotalRevenue(
                        result.getBigDecimal(
                                "total_revenue"
                        )
                );

                sales.setTotalOrders(
                        result.getInt(
                                "total_orders"
                        )
                );

                sales.setProductsSold(
                        result.getInt(
                                "products_sold"
                        )
                );

                BigDecimal revenue =
                        sales.getTotalRevenue();

                int orders =
                        sales.getTotalOrders();

                BigDecimal averageOrderValue =
                        BigDecimal.ZERO;

                if (
                        revenue != null &&
                        orders > 0
                ) {
                    averageOrderValue =
                            revenue.divide(
                                    BigDecimal.valueOf(orders),
                                    2,
                                    java.math.RoundingMode.HALF_UP
                            );
                }

                sales.setAverageOrderValue(
                        averageOrderValue
                );

                return sales;
            }
        }
    }

    SellerSalesDTO emptySales =
            new SellerSalesDTO();

    emptySales.setTotalRevenue(
            BigDecimal.ZERO
    );

    emptySales.setTotalOrders(0);

    emptySales.setProductsSold(0);

    emptySales.setAverageOrderValue(
            BigDecimal.ZERO
    );

    return emptySales;
}

}