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

    // GET PRODUCTS
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getPathInfo();

        List<Product> products;

        // GET ONLY LOGGED-IN SELLER'S PRODUCTS
        if ("/mine".equalsIgnoreCase(path)) {

            User user = getLoggedInSeller(request);

            if (user == null) {
                sendError(
                        response,
                        HttpServletResponse.SC_FORBIDDEN,
                        "Only sellers can view their listings."
                );
                return;
            }

            products = productDAO.getProductsBySeller(user.getId());

        } else {

            // PUBLIC PRODUCT LIST

            String category = request.getParameter("category");

            if (category != null && !category.isBlank()
                    && !"ALL".equalsIgnoreCase(category)) {

                products = productDAO.getProductsByCategory(category);

            } else {

                products = productDAO.getAllProducts();
            }
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        response.getWriter().write(gson.toJson(products));
    }


    // ADD PRODUCT
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = getLoggedInSeller(request);

        if (user == null) {

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


            // VALIDATION

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


            // CREATE PRODUCT

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


    // EDIT PRODUCT
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = getLoggedInSeller(request);

        if (user == null) {

            sendError(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "Only sellers can edit products."
            );

            return;
        }


        // GET PRODUCT ID FROM URL

        String path = request.getPathInfo();

        if (path == null || path.equals("/") || path.length() <= 1) {

            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Product ID is required."
            );

            return;
        }


        int productId;

        try {

            productId = Integer.parseInt(path.substring(1));

        } catch (NumberFormatException e) {

            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid product ID."
            );

            return;
        }


        try {

            Map<String, Object> data = readJson(request);

            String name = getString(data, "name");
            String description = getString(data, "description");
            String category = getString(data, "category");
            String imageUrl = getString(data, "imageUrl");

            BigDecimal price = getBigDecimal(data, "price");

            int stockQty = getInt(data, "stockQty");


            // VALIDATION

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


            // CREATE UPDATED PRODUCT

            Product product = new Product();

            product.setId(productId);
            product.setSellerId(user.getId());
            product.setName(name);
            product.setDescription(description);
            product.setPrice(price);
            product.setStockQty(stockQty);
            product.setCategory(category.toUpperCase());
            product.setImageUrl(imageUrl);


            // UPDATE DATABASE

            boolean updated = productDAO.updateProduct(product);

            if (!updated) {

                sendError(
                        response,
                        HttpServletResponse.SC_NOT_FOUND,
                        "Product not found or you do not own this product."
                );

                return;
            }


            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);

            Map<String, Object> result = new HashMap<>();

            result.put("success", true);
            result.put("message", "Product updated successfully.");

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


    // DELETE PRODUCT
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = getLoggedInSeller(request);

        if (user == null) {

            sendError(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "Only sellers can delete products."
            );

            return;
        }


        // GET PRODUCT ID FROM URL

        String path = request.getPathInfo();

        if (path == null || path.equals("/") || path.length() <= 1) {

            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Product ID is required."
            );

            return;
        }


        int productId;

        try {

            productId = Integer.parseInt(path.substring(1));

        } catch (NumberFormatException e) {

            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid product ID."
            );

            return;
        }


        // DELETE ONLY SELLER'S OWN PRODUCT

        boolean deleted = productDAO.deleteProduct(
                productId,
                user.getId()
        );


        if (!deleted) {

            sendError(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "Product not found or you do not own this product."
            );

            return;
        }


        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        Map<String, Object> result = new HashMap<>();

        result.put("success", true);
        result.put("message", "Product deleted successfully.");

        response.getWriter().write(gson.toJson(result));
    }


    // GET LOGGED-IN SELLER

    private User getLoggedInSeller(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            return null;
        }

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return null;
        }

        if (!"SELLER".equalsIgnoreCase(user.getRole())) {
            return null;
        }

        return user;
    }


    // READ JSON

    private Map<String, Object> readJson(HttpServletRequest request)
            throws IOException {

        StringBuilder json = new StringBuilder();

        try (BufferedReader reader = request.getReader()) {

            String line;

            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
        }

        Map<String, Object> data =
                gson.fromJson(json.toString(), Map.class);

        return data != null ? data : new HashMap<>();
    }


    // GET STRING

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


    // GET PRICE

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


    // GET INTEGER

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


    // ERROR RESPONSE

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

        response.getWriter().write(gson.toJson(result));
    }
}