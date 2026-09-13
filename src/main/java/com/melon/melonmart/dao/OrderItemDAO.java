package com.melon.melonmart.dao;

import com.melon.melonmart.listener.DbContextListener;
import com.melon.melonmart.model.OrderItem;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class OrderItemDAO {

    private final DataSource dataSource;

    public OrderItemDAO() {
        this.dataSource = DbContextListener.getDataSource();
    }

    public void addOrderItem(OrderItem item) throws Exception {

        try (Connection connection = dataSource.getConnection()) {

            addOrderItem(connection, item);
        }
    }

    public void addOrderItem(
            Connection connection,
            OrderItem item
    ) throws Exception {

        String sql = """
                INSERT INTO order_items
                (order_id, product_id, quantity, price)
                VALUES (?, ?, ?, ?)
                """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(1, item.getOrderId());
            statement.setInt(2, item.getProductId());
            statement.setInt(3, item.getQuantity());
            statement.setBigDecimal(4, item.getPrice());

            statement.executeUpdate();
        }
    }

    public List<OrderItem> getItemsByOrderId(
            int orderId
    ) throws Exception {

        String sql = """
                SELECT
                    id,
                    order_id,
                    product_id,
                    quantity,
                    price
                FROM order_items
                WHERE order_id = ?
                """;

        List<OrderItem> items = new ArrayList<>();

        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(1, orderId);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    OrderItem item = new OrderItem();

                    item.setId(
                            result.getInt("id")
                    );

                    item.setOrderId(
                            result.getInt("order_id")
                    );

                    item.setProductId(
                            result.getInt("product_id")
                    );

                    item.setQuantity(
                            result.getInt("quantity")
                    );

                    item.setPrice(
                            result.getBigDecimal("price")
                    );

                    items.add(item);
                }
            }
        }

        return items;
    }
}