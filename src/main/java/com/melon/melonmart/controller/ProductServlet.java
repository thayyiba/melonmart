package com.melon.melonmart.controller;

import com.google.gson.Gson;
import com.melon.melonmart.dao.ProductDAO;
import com.melon.melonmart.model.Product;
import com.melon.melonmart.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/products/*")
public class ProductServlet extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();
    private final Gson gson = new Gson();

    // =========================
    // GET PRODUCTS
    // =========================

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        String category = request.getParameter("category");

        List<Product> products;

        if (category != null
                && !category.isBlank()
                && !"ALL".equalsIgnoreCase(category)) {

            products = productDAO.getProductsByCategory(category);

        } else {

            products = productDAO.getAllProducts();
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        response.getWriter().write(gson.toJson(products));
    }

    // =========================
    // ADD PRODUCT
    // =========================

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        User user = null;

        if (session != null) {
            user = (User) session.getAttribute("user");
        }

        // Only sellers can add products
        if (user == null
                || !"SELLER".equalsIgnoreCase(user.getRole())) {

            sendError(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "Only sellers can add products."
            );

            return;
        }

        try {

            Map<String, Object> data = readJson(request);

            String name = getString(data, "name");

if (name == null || name.isBlank()) {
    name = getString(data, "title");
}
            String description = getString(data, "description");
            String category = getString(data, "category");
            String imageUrl = getString(data, "imageUrl");

            BigDecimal price = getBigDecimal(data, "price");

            int stockQty = getInt(data, "stockQty");

            // Validation
            if (name == null || name.isBlank()) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Product name is required."
                );

                return;
            }

            if (category == null || category.isBlank()) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Product category is required."
                );

                return;
            }

            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Price must be greater than zero."
                );

                return;
            }

            if (stockQty < 0) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Stock quantity cannot be negative."
                );

                return;
            }

            Product product = new Product();

            product.setSellerId(user.getId());
            product.setName(name);
            product.setDescription(description);
            product.setPrice(price);
            product.setStockQty(stockQty);
            product.setCategory(category.toUpperCase());
            product.setImageUrl(imageUrl);

            boolean created = productDAO.addProduct(product);

            if (!created) {

                sendError(
                        response,
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Failed to create product."
                );

                return;
            }

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_CREATED);

            Map<String, Object> result = new HashMap<>();

            result.put("success", true);
            result.put("message", "Product created successfully.");

            response.getWriter().write(gson.toJson(result));

        } catch (Exception e) {

            e.printStackTrace();

            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid product data."
            );
        }
    }

    // =========================
    // JSON READER
    // =========================

    private Map<String, Object> readJson(
            HttpServletRequest request
    ) throws IOException {

        StringBuilder json = new StringBuilder();

        try (BufferedReader reader = request.getReader()) {

            String line;

            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
        }

        Map<String, Object> data =
                gson.fromJson(json.toString(), Map.class);

        return data != null
                ? data
                : new HashMap<>();
    }

    // =========================
    // GET STRING
    // =========================

    private String getString(
            Map<String, Object> data,
            String key
    ) {

        Object value = data.get(key);

        if (value == null) {
            return null;
        }

        return value.toString().trim();
    }

    // =========================
    // GET PRICE
    // =========================

    private BigDecimal getBigDecimal(
            Map<String, Object> data,
            String key
    ) {

        Object value = data.get(key);

        if (value == null) {
            return null;
        }

        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
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

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);

        Map<String, Object> result = new HashMap<>();

        result.put("success", false);
        result.put("message", message);

        response.getWriter().write(
                gson.toJson(result)
        );
    }
}