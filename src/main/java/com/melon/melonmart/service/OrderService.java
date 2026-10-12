
package com.melon.melonmart.service;

import com.melon.melonmart.dao.CartDAO;
import com.melon.melonmart.dao.OrderDAO;
import com.melon.melonmart.dao.OrderItemDAO;
import com.melon.melonmart.listener.DbContextListener;
import com.melon.melonmart.model.CartItem;
import com.melon.melonmart.model.Order;
import com.melon.melonmart.model.OrderItem;
import com.melon.melonmart.dto.SellerOrderDTO;
import com.melon.melonmart.dto.SellerSalesDTO;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
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
        List<CartItem> cartItems = cartDAO.getCartByUserId(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }
        for (CartItem item : cartItems) {
            if (item.getQuantity() <= 0) {
                throw new IllegalStateException("Invalid product quantity");
            }
        }

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                BigDecimal totalAmount = BigDecimal.ZERO;
                Map<Integer, BigDecimal> currentPrices = new HashMap<>();

                // Reserve each line and read its authoritative price in this transaction.
                for (CartItem cartItem : cartItems) {
                    boolean stockReserved = orderDAO.reserveStock(
                            connection, cartItem.getProductId(), cartItem.getQuantity());
                    if (!stockReserved) {
                        throw new IllegalStateException(
                                "Insufficient stock for product ID: " + cartItem.getProductId());
                    }

                    BigDecimal currentPrice = orderDAO.getProductPrice(
                            connection, cartItem.getProductId());
                    if (currentPrice == null) {
                        throw new IllegalStateException(
                                "Product is no longer available: " + cartItem.getProductId());
                    }

                    currentPrices.put(cartItem.getProductId(), currentPrice);
                    totalAmount = totalAmount.add(
                            currentPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
                }

                Order order = new Order();
                order.setUserId(userId);
                order.setTotalAmount(totalAmount);
                order.setStatus("PENDING");

                int orderId = orderDAO.createOrder(connection, order);
                if (orderId <= 0) {
                    throw new IllegalStateException("Could not create order");
                }

                for (CartItem cartItem : cartItems) {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrderId(orderId);
                    orderItem.setProductId(cartItem.getProductId());
                    orderItem.setQuantity(cartItem.getQuantity());
                    orderItem.setPrice(currentPrices.get(cartItem.getProductId()));
                    orderItemDAO.addOrderItem(connection, orderItem);
                }

                cartDAO.clearCart(connection, userId);
                connection.commit();
                order.setId(orderId);
                return order;
            } catch (Exception e) {
                try {
                    connection.rollback();
                } catch (Exception rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw e;
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (Exception ignored) {
                    // Closing the connection returns it to the pool.
                }
            }
        }
    }

    public List<Order> getOrdersByUserId(
            int userId
    ) throws Exception {

        return orderDAO.getOrdersByUserId(userId);
    }

    public List<SellerOrderDTO> getOrdersBySellerId(
        int sellerId
) throws Exception {

    return orderDAO.getOrdersBySellerId(sellerId);
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
    public boolean sellerOwnsOrder(
        int sellerId,
        int orderId
) throws Exception {

    return orderDAO.sellerOwnsOrder(
            sellerId,
            orderId
    );
}

public SellerSalesDTO getSellerSales(
        int sellerId
) throws Exception {

    return orderDAO.getSellerSales(
            sellerId
    );
}

}