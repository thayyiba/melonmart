
package com.melon.melonmart.service;

import com.melon.melonmart.dao.CartDAO;
import com.melon.melonmart.dao.OrderDAO;
import com.melon.melonmart.dao.OrderItemDAO;
import com.melon.melonmart.listener.DbContextListener;
import com.melon.melonmart.model.CartItem;
import com.melon.melonmart.model.Order;
import com.melon.melonmart.model.OrderItem;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

public class OrderService {

    private final DataSource dataSource;

    private final CartDAO cartDAO;
    private final OrderDAO orderDAO;
    private final OrderItemDAO orderItemDAO;

    public OrderService() {
        this.dataSource = DbContextListener.getDataSource();

        this.cartDAO = new CartDAO();
        this.orderDAO = new OrderDAO();
        this.orderItemDAO = new OrderItemDAO();
    }

    public Order checkout(int userId) throws Exception {

        // Get the user's cart
        List<CartItem> cartItems =
                cartDAO.getCartByUserId(userId);

        // Cart must contain at least one item
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        // Calculate the order total
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cartItems) {

            if (item.getProductPrice() == null) {
                throw new IllegalStateException(
                        "Product price is missing"
                );
            }

            if (item.getQuantity() <= 0) {
                throw new IllegalStateException(
                        "Invalid product quantity"
                );
            }

            BigDecimal itemTotal =
                    item.getProductPrice().multiply(
                            BigDecimal.valueOf(item.getQuantity())
                    );

            totalAmount = totalAmount.add(itemTotal);
        }

        // One connection for the complete checkout transaction
        try (Connection connection =
                     dataSource.getConnection()) {

            connection.setAutoCommit(false);

            try {

                // Create the order
                Order order = new Order();

                order.setUserId(userId);
                order.setTotalAmount(totalAmount);
                order.setStatus("PLACED");

                // Insert order and get generated ID
                int orderId =
                        orderDAO.createOrder(
                                connection,
                                order
                        );

                if (orderId <= 0) {
                    throw new IllegalStateException(
                            "Could not create order"
                    );
                }

                // Create order item for every cart item
                for (CartItem cartItem : cartItems) {

                    OrderItem orderItem =
                            new OrderItem();

                    orderItem.setOrderId(orderId);

                    orderItem.setProductId(
                            cartItem.getProductId()
                    );

                    orderItem.setQuantity(
                            cartItem.getQuantity()
                    );

                    orderItem.setPrice(
                            cartItem.getProductPrice()
                    );

                    orderItemDAO.addOrderItem(
                            connection,
                            orderItem
                    );
                }

                // Clear the cart using the same transaction
                cartDAO.clearCart(
                        connection,
                        userId
                );

                // Everything succeeded
                connection.commit();

                // Put generated ID into the order object
                order.setId(orderId);

                return order;

            } catch (Exception e) {

                // Something failed.
                // Undo the entire checkout.
                try {
                    connection.rollback();
                } catch (Exception rollbackException) {
                    rollbackException.printStackTrace();
                }

                throw e;

            } finally {

                try {
                    connection.setAutoCommit(true);
                } catch (Exception autoCommitException) {
                    autoCommitException.printStackTrace();
                }
            }
        }
    }

    public List<Order> getOrdersByUserId(
            int userId
    ) throws Exception {

        return orderDAO.getOrdersByUserId(userId);
    }

    public Order getOrderById(
            int orderId
    ) throws Exception {

        return orderDAO.getOrderById(orderId);
    }

    public List<OrderItem> getOrderItems(
            int orderId
    ) throws Exception {

        return orderItemDAO.getItemsByOrderId(orderId);
    }

    public List<Order> getAllOrders()
            throws Exception {

        return orderDAO.getAllOrders();
    }

    public void updateOrderStatus(
            int orderId,
            String status
    ) throws Exception {

        orderDAO.updateOrderStatus(
                orderId,
                status
        );
    }
}