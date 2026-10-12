
package com.melon.melonmart.controller;

import com.melon.melonmart.dto.SellerOrderDTO;
import com.melon.melonmart.dto.SellerSalesDTO;
import com.google.gson.Gson;
import com.melon.melonmart.model.Order;
import com.melon.melonmart.model.OrderItem;
import com.melon.melonmart.model.User;
import com.melon.melonmart.service.OrderService;
import com.melon.melonmart.service.OrderStatusPolicy;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@WebServlet({"/api/orders/*", "/api/v1/orders/*"})
public class OrderServlet extends HttpServlet {

    private final Gson gson = new Gson();

    private OrderService orderService;

    @Override
    public void init() throws ServletException {
        orderService = new OrderService();
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String path = request.getPathInfo();

        if (path == null || !path.equals("/checkout")) {

            sendError(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "Endpoint not found"
            );

            return;
        }

        User user = getLoggedInUser(request);

        if (user == null) {

            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Please login first"
            );

            return;
        }

        try {
            String fullName = parameter(request, "fullName");
            String email = parameter(request, "email");
            String phone = parameter(request, "phone");
            String address = parameter(request, "address");
            String city = parameter(request, "city");
            String state = parameter(request, "state");
            String pincode = parameter(request, "pincode");
            String landmark = parameter(request, "landmark");
            String payment = parameter(request, "payment").toUpperCase(Locale.ROOT);
            if (payment.isBlank()) payment = "COD";

            boolean valid = fullName.length() >= 2 && fullName.length() <= 100
                    && email.length() <= 150
                    && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                    && phone.matches("[0-9]{10}")
                    && !address.isBlank() && address.length() <= 500
                    && !city.isBlank() && city.length() <= 100
                    && !state.isBlank() && state.length() <= 100
                    && pincode.matches("[0-9]{6}")
                    && landmark.length() <= 150
                    && List.of("COD", "UPI", "CARD").contains(payment);
            if (!valid) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Please check your contact, delivery and payment details.");
                return;
            }

            Order checkoutDetails = new Order();
            checkoutDetails.setDeliveryName(fullName);
            checkoutDetails.setDeliveryEmail(email);
            checkoutDetails.setDeliveryPhone(phone);
            checkoutDetails.setDeliveryAddress(address);
            checkoutDetails.setDeliveryCity(city);
            checkoutDetails.setDeliveryState(state);
            checkoutDetails.setDeliveryPincode(pincode);
            checkoutDetails.setDeliveryLandmark(landmark);
            checkoutDetails.setPaymentMethod(payment);

            Order order = orderService.checkout(user.getId(), checkoutDetails);

            Map<String, Object> result =
                    new HashMap<>();

            result.put("success", true);
            result.put(
                    "message",
                    "Order placed successfully"
            );
            result.put("order", order);

            sendJson(
                    response,
                    HttpServletResponse.SC_CREATED,
                    result
            );

        } catch (IllegalStateException e) {

            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    e.getMessage()
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not place order"
            );
        }
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        User user = getLoggedInUser(request);

        if (user == null) {

            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Please login first"
            );

            return;
        }

        String path = request.getPathInfo();

        try {

                // GET /api/orders/seller/sales
if (path != null && path.equals("/seller/sales")) {

    if (!"SELLER".equals(user.getRole())) {

        sendError(
                response,
                HttpServletResponse.SC_FORBIDDEN,
                "Seller access required"
        );

        return;
    }

    SellerSalesDTO sales =
            orderService.getSellerSales(
                    user.getId()
            );

    sendJson(
            response,
            HttpServletResponse.SC_OK,
            sales
    );

    return;
}
                // GET /api/orders/seller
if (path != null && path.equals("/seller")) {

    if (!"SELLER".equals(user.getRole())) {

        sendError(
                response,
                HttpServletResponse.SC_FORBIDDEN,
                "Seller access required"
        );

        return;
    }

    List<SellerOrderDTO> orders =
            orderService.getOrdersBySellerId(
                    user.getId()
            );

    sendJson(
            response,
            HttpServletResponse.SC_OK,
            orders
    );

    return;
}

            // GET /api/orders
            if (path == null || path.equals("/")) {

                List<Order> orders =
                        orderService.getOrdersByUserId(
                                user.getId()
                        );

                sendJson(
                        response,
                        HttpServletResponse.SC_OK,
                        orders
                );

                return;
            }

            // GET /api/orders/{id}
            String idText = path.substring(1);

            int orderId;

            try {

                orderId = Integer.parseInt(idText);

            } catch (NumberFormatException e) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid order ID"
                );

                return;
            }

            Order order =
                    orderService.getOrderById(orderId);

            if (order == null) {

                sendError(
                        response,
                        HttpServletResponse.SC_NOT_FOUND,
                        "Order not found"
                );

                return;
            }

            // Users can only view their own orders
            if (order.getUserId() != user.getId()) {

                sendError(
                        response,
                        HttpServletResponse.SC_FORBIDDEN,
                        "You cannot view this order"
                );

                return;
            }

            List<OrderItem> items =
                    orderService.getOrderItems(orderId);

            Map<String, Object> result =
                    new HashMap<>();

            result.put("order", order);
            result.put("items", items);

            sendJson(
                    response,
                    HttpServletResponse.SC_OK,
                    result
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not retrieve orders"
            );
        }
    }

    @Override
protected void doPut(
        HttpServletRequest request,
        HttpServletResponse response
) throws ServletException, IOException {

    User user = getLoggedInUser(request);

    if (user == null) {

        sendError(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "Please login first"
        );

        return;
    }

    String path = request.getPathInfo();

    if (path == null || path.equals("/")) {

        sendError(
                response,
                HttpServletResponse.SC_BAD_REQUEST,
                "Order ID is required"
        );

        return;
    }

    String[] parts =
            path.split("/");

    if (parts.length != 3 ||
            !parts[2].equals("status")) {

        sendError(
                response,
                HttpServletResponse.SC_NOT_FOUND,
                "Status endpoint not found"
        );

        return;
    }

    int orderId;

    try {

        orderId =
                Integer.parseInt(parts[1]);

    } catch (NumberFormatException e) {

        sendError(
                response,
                HttpServletResponse.SC_BAD_REQUEST,
                "Invalid order ID"
        );

        return;
    }

    if (
            !"SELLER".equals(user.getRole()) &&
            !"ADMIN".equals(user.getRole())
    ) {

        sendError(
                response,
                HttpServletResponse.SC_FORBIDDEN,
                "Seller or admin access required"
        );

        return;
    }

    String status =
            request.getParameter("status");

    if (status == null ||
            status.trim().isEmpty()) {

        sendError(
                response,
                HttpServletResponse.SC_BAD_REQUEST,
                "Order status is required"
        );

        return;
    }

    status =
            status.trim().toUpperCase();

    if (!OrderStatusPolicy.isAllowedUpdateStatus(status)) {

        sendError(
                response,
                HttpServletResponse.SC_BAD_REQUEST,
                "Invalid order status"
        );

        return;
    }

    try {

        if ("SELLER".equals(user.getRole())) {

            boolean ownsOrder =
                    orderService.sellerOwnsOrder(
                            user.getId(),
                            orderId
                    );

            if (!ownsOrder) {

                sendError(
                        response,
                        HttpServletResponse.SC_FORBIDDEN,
                        "You cannot update this order"
                );

                return;
            }
        }

        Order order =
                orderService.getOrderById(orderId);

        if (order == null) {

            sendError(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "Order not found"
            );

            return;
        }

        orderService.updateOrderStatus(
                orderId,
                status
        );

        Map<String, Object> result =
                new HashMap<>();

        result.put(
                "success",
                true
        );

        result.put(
                "message",
                "Order status updated successfully"
        );

        result.put(
                "orderId",
                orderId
        );

        result.put(
                "status",
                status
        );

        sendJson(
                response,
                HttpServletResponse.SC_OK,
                result
        );

    } catch (Exception e) {

        e.printStackTrace();

        sendError(
                response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Could not update order status"
        );
    }
}


    private String parameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    private User getLoggedInUser(
            HttpServletRequest request
    ) {

        HttpSession session =
                request.getSession(false);

        if (session == null) {
            return null;
        }

        Object user =
                session.getAttribute("user");

        if (user instanceof User) {
            return (User) user;
        }

        return null;
    }

    private void sendJson(
            HttpServletResponse response,
            int status,
            Object data
    ) throws IOException {

        response.setStatus(status);

        response.setContentType(
                "application/json"
        );

        response.setCharacterEncoding(
                "UTF-8"
        );

        response.getWriter().write(
                gson.toJson(data)
        );
    }

    private void sendError(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {

        Map<String, Object> error =
                new HashMap<>();

        error.put("success", false);
        error.put("message", message);

        sendJson(
                response,
                status,
                error
        );
    }
}