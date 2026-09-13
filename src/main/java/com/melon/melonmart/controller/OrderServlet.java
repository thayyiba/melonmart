
package com.melon.melonmart.controller;

import com.google.gson.Gson;
import com.melon.melonmart.model.Order;
import com.melon.melonmart.model.OrderItem;
import com.melon.melonmart.model.User;
import com.melon.melonmart.service.OrderService;

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

@WebServlet("/api/orders/*")
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

            Order order =
                    orderService.checkout(user.getId());

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