package com.melon.melonmart;

import com.melon.melonmart.dao.OrderDAO;
import com.melon.melonmart.model.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderCheckoutDetailsTest {
    @Test
    void createOrderPersistsDeliveryAndPaymentDetails() throws Exception {
        String databaseName = "order_details_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:" + databaseName, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE orders (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        buyer_id INT NOT NULL,
                        total_amount DECIMAL(10,2) NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        delivery_name VARCHAR(100),
                        delivery_email VARCHAR(150),
                        delivery_phone VARCHAR(20),
                        delivery_address VARCHAR(500),
                        delivery_city VARCHAR(100),
                        delivery_state VARCHAR(100),
                        delivery_pincode VARCHAR(10),
                        delivery_landmark VARCHAR(150),
                        payment_method VARCHAR(20) NOT NULL DEFAULT 'COD'
                    )
                    """);

            Order order = new Order();
            order.setUserId(7);
            order.setTotalAmount(new BigDecimal("149.50"));
            order.setStatus("PENDING");
            order.setDeliveryName("Demo Buyer");
            order.setDeliveryEmail("buyer@example.com");
            order.setDeliveryPhone("9876543210");
            order.setDeliveryAddress("12 Market Road");
            order.setDeliveryCity("Coimbatore");
            order.setDeliveryState("Tamil Nadu");
            order.setDeliveryPincode("641001");
            order.setDeliveryLandmark("Near the library");
            order.setPaymentMethod("UPI");

            int orderId = new OrderDAO().createOrder(connection, order);
            assertTrue(orderId > 0);

            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT total_amount, delivery_name, delivery_email, delivery_phone, "
                            + "delivery_address, delivery_city, delivery_state, delivery_pincode, "
                            + "delivery_landmark, payment_method FROM orders WHERE id = ?")) {
                query.setInt(1, orderId);
                try (ResultSet result = query.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(new BigDecimal("149.50"), result.getBigDecimal("total_amount"));
                    assertEquals("Demo Buyer", result.getString("delivery_name"));
                    assertEquals("buyer@example.com", result.getString("delivery_email"));
                    assertEquals("9876543210", result.getString("delivery_phone"));
                    assertEquals("12 Market Road", result.getString("delivery_address"));
                    assertEquals("Coimbatore", result.getString("delivery_city"));
                    assertEquals("Tamil Nadu", result.getString("delivery_state"));
                    assertEquals("641001", result.getString("delivery_pincode"));
                    assertEquals("Near the library", result.getString("delivery_landmark"));
                    assertEquals("UPI", result.getString("payment_method"));
                }
            }
        }
    }
}
