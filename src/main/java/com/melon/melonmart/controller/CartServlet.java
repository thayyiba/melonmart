package com.melon.melonmart.controller;

import com.google.gson.Gson;
import com.melon.melonmart.dao.CartDAO;
import com.melon.melonmart.model.CartItem;
import com.melon.melonmart.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/cart")
public class CartServlet extends HttpServlet {

    private final CartDAO cartDAO = new CartDAO();
    private final Gson gson = new Gson();

    // =========================
    // GET CART
    // =========================

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
                    "Please log in to view your cart."
            );
            return;
        }

        List<CartItem> cart =
                cartDAO.getCartByUserId(user.getId());

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        response.getWriter().write(gson.toJson(cart));
    }

    // =========================
    // ADD / UPDATE CART
    // =========================

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        User user = getLoggedInUser(request);

        if (user == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Please log in to add items to your cart."
            );
            return;
        }

        try {

            Map<String, Object> data = readJson(request);

            int productId = getInt(data, "productId");
            int quantity = getInt(data, "quantity");

            if (productId <= 0) {
                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid product ID."
                );
                return;
            }

            if (quantity <= 0) {
                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Quantity must be greater than zero."
                );
                return;
            }

            boolean success = cartDAO.addToCart(
                    user.getId(),
                    productId,
                    quantity
            );

            if (!success) {
                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Could not update cart."
                );
                return;
            }

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);

            Map<String, Object> result = new HashMap<>();

            result.put("success", true);
            result.put("message", "Cart updated successfully.");

            response.getWriter().write(
                    gson.toJson(result)
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid cart data."
            );
        }
    }

    // =========================
    // REMOVE CART ITEM
    // =========================

    @Override
    protected void doDelete(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        User user = getLoggedInUser(request);

        if (user == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Please log in first."
            );
            return;
        }

        String productIdParam =
                request.getParameter("productId");

        if (productIdParam == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Product ID is required."
            );
            return;
        }

        try {

            int productId =
                    Integer.parseInt(productIdParam);

            boolean removed =
                    cartDAO.removeFromCart(
                            user.getId(),
                            productId
                    );

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> result =
                    new HashMap<>();

            result.put("success", removed);

            if (removed) {
                result.put(
                        "message",
                        "Item removed from cart."
                );
            } else {
                result.put(
                        "message",
                        "Item was not found in cart."
                );
            }

            response.setStatus(
                    removed
                            ? HttpServletResponse.SC_OK
                            : HttpServletResponse.SC_NOT_FOUND
            );

            response.getWriter().write(
                    gson.toJson(result)
            );

        } catch (NumberFormatException e) {

            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid product ID."
            );
        }
    }

    // =========================
    // GET LOGGED-IN USER
    // =========================

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

    // =========================
    // READ JSON
    // =========================

    private Map<String, Object> readJson(
            HttpServletRequest request
    ) throws IOException {

        StringBuilder json =
                new StringBuilder();

        try (BufferedReader reader =
                     request.getReader()) {

            String line;

            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
        }

        Map<String, Object> data =
                gson.fromJson(
                        json.toString(),
                        Map.class
                );

        return data != null
                ? data
                : new HashMap<>();
    }

    // =========================
    // GET INTEGER
    // =========================

  private int getInt(
        Map<String, Object> data,
        String key
) {

    Object value = data.get(key);

    if (value == null) {
        return 0;
    }

    try {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        return Integer.parseInt(value.toString());

    } catch (NumberFormatException e) {
        return 0;
    }
}

    // =========================
    // ERROR RESPONSE
    // =========================

    private void sendError(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {

        response.setContentType(
                "application/json"
        );

        response.setCharacterEncoding(
                "UTF-8"
        );

        response.setStatus(status);

        Map<String, Object> result =
                new HashMap<>();

        result.put("success", false);
        result.put("message", message);

        response.getWriter().write(
                gson.toJson(result)
        );
    }
}