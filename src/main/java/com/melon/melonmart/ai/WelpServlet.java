package com.melon.melonmart.ai;

import com.google.gson.Gson;
import com.melon.melonmart.dao.CartDAO;
import com.melon.melonmart.dao.ProductDAO;
import com.melon.melonmart.model.CartItem;
import com.melon.melonmart.model.Product;
import com.melon.melonmart.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@WebServlet("/api/welp")
public class WelpServlet extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();

    private final CartDAO cartDAO = new CartDAO();

    private final WelpService welpService =
            new WelpService(new GeminiAIProvider());

    private final Gson gson = new Gson();

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {

            ChatRequest chatRequest =
                    gson.fromJson(
                            request.getReader(),
                            ChatRequest.class
                    );

            if (chatRequest == null ||
                    chatRequest.message == null ||
                    chatRequest.message.trim().isEmpty()) {

                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST
                );

                response.getWriter().write(
                        gson.toJson(
                                new ChatResponse(
                                        "Please tell me what you're looking for! 🍉"
                                )
                        )
                );

                return;
            }

            String message =
                    chatRequest.message.trim();

            if (isAddToCartRequest(message)) {

                String reply =
                        handleAddToCart(
                                request,
                                message
                        );

                response.getWriter().write(
                        gson.toJson(
                                new ChatResponse(reply)
                        )
                );

                return;
            }

            if (isRemoveFromCartRequest(message)) {

                String reply =
                        handleRemoveFromCart(
                                request,
                                message
                        );

                response.getWriter().write(
                        gson.toJson(
                                new ChatResponse(reply)
                        )
                );

                return;
            }

            List<Product> products =
                    productDAO.getAllProducts();

            String productContext =
                    buildProductContext(products);

            String reply =
                    welpService.chat(
                            message,
                            productContext
                    );

            response.getWriter().write(
                    gson.toJson(
                            new ChatResponse(reply)
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            response.getWriter().write(
                    gson.toJson(
                            new ChatResponse(
                                    "Welp ran into a problem. Please try again! 🍉"
                            )
                    )
            );
        }
    }

    // =========================
    // ADD REQUEST
    // =========================

    private boolean isAddToCartRequest(String message) {

        String lower =
                message.toLowerCase(Locale.ROOT);

        return lower.contains("add")
                && (
                    lower.contains("cart")
                    || lower.contains("basket")
                );
    }

    // =========================
    // REMOVE REQUEST
    // =========================

    private boolean isRemoveFromCartRequest(String message) {

        String lower =
                message.toLowerCase(Locale.ROOT);

        return (
                lower.contains("remove")
                || lower.contains("delete")
                || lower.contains("take out")
        )
                && (
                    lower.contains("cart")
                    || lower.contains("basket")
                );
    }

    // =========================
    // HANDLE ADD
    // =========================

    private String handleAddToCart(
            HttpServletRequest request,
            String message
    ) {

        User user =
                getLoggedInUser(request);

        if (user == null) {

            return "Please log in first so I can add something to your cart! 🍉";
        }

        String productSearch =
                extractProductName(message);

        if (productSearch.isEmpty()) {

            return "Tell me which product you'd like me to add! 🍉";
        }

        int quantity =
                extractQuantity(message);

        List<Product> products =
                productDAO.getAllProducts();

        Product matchedProduct =
                findProduct(
                        products,
                        productSearch
                );

        if (matchedProduct == null) {

            return "I couldn't find that product on Melon Mart. Try using a little more of its name! 🍉";
        }

        if (matchedProduct.getStockQty() <= 0) {

            return matchedProduct.getName()
                    + " is currently out of stock. 🍉";
        }

        List<CartItem> cart =
                cartDAO.getCartByUserId(
                        user.getId()
                );

        CartItem existingItem =
                findCartItem(
                        cart,
                        matchedProduct.getId()
                );

        if (existingItem != null) {

            int newQuantity =
                    existingItem.getQuantity()
                            + quantity;

            if (newQuantity >
                    matchedProduct.getStockQty()) {

                return "I can't add "
                        + quantity
                        + " more. There are only "
                        + matchedProduct.getStockQty()
                        + " "
                        + matchedProduct.getName()
                        + " available. 🍉";
            }

            boolean success =
                    cartDAO.increaseQuantity(
                            user.getId(),
                            matchedProduct.getId(),
                            quantity
                    );

            if (!success) {

                return "I couldn't update your cart. Please try again! 🍉";
            }

            return "Added "
                    + quantity
                    + " more "
                    + matchedProduct.getName()
                    + " to your cart! 🍉🛒";
        }

        if (quantity >
                matchedProduct.getStockQty()) {

            return "I only have "
                    + matchedProduct.getStockQty()
                    + " "
                    + matchedProduct.getName()
                    + " available. 🍉";
        }

        boolean success =
                cartDAO.addToCart(
                        user.getId(),
                        matchedProduct.getId(),
                        quantity
                );

        if (!success) {

            return "I couldn't add "
                    + matchedProduct.getName()
                    + " to your cart. Please try again! 🍉";
        }

        return "Added "
                + quantity
                + " "
                + matchedProduct.getName()
                + " to your cart! 🍉🛒";
    }

    // =========================
    // HANDLE REMOVE
    // =========================

    private String handleRemoveFromCart(
            HttpServletRequest request,
            String message
    ) {

        User user =
                getLoggedInUser(request);

        if (user == null) {

            return "Please log in first so I can change your cart! 🍉";
        }

        String productSearch =
                extractProductName(message);

        if (productSearch.isEmpty()) {

            return "Tell me which product you'd like me to remove! 🍉";
        }

        List<Product> products =
                productDAO.getAllProducts();

        Product matchedProduct =
                findProduct(
                        products,
                        productSearch
                );

        if (matchedProduct == null) {

            return "I couldn't find that product on Melon Mart. 🍉";
        }

        List<CartItem> cart =
                cartDAO.getCartByUserId(
                        user.getId()
                );

        CartItem existingItem =
                findCartItem(
                        cart,
                        matchedProduct.getId()
                );

        if (existingItem == null) {

            return matchedProduct.getName()
                    + " isn't in your cart. 🍉";
        }

        boolean removeEntireItem =
                message.toLowerCase(Locale.ROOT)
                        .matches(
                                ".*\\b(delete|remove)\\b.*\\b(cart|basket)\\b.*"
                        )
                        && !message.toLowerCase(Locale.ROOT)
                        .contains("one");

        if (removeEntireItem) {

            boolean success =
                    cartDAO.removeFromCart(
                            user.getId(),
                            matchedProduct.getId()
                    );

            if (!success) {

                return "I couldn't remove "
                        + matchedProduct.getName()
                        + " from your cart. 🍉";
            }

            return "Removed "
                    + matchedProduct.getName()
                    + " from your cart! 🍉🛒";
        }

        int removeQuantity =
                extractQuantity(message);

        int currentQuantity =
                existingItem.getQuantity();

        int newQuantity =
                currentQuantity - removeQuantity;

        if (newQuantity <= 0) {

            boolean success =
                    cartDAO.removeFromCart(
                            user.getId(),
                            matchedProduct.getId()
                    );

            if (!success) {

                return "I couldn't remove "
                        + matchedProduct.getName()
                        + " from your cart. 🍉";
            }

            return "Removed "
                    + matchedProduct.getName()
                    + " from your cart! 🍉🛒";
        }

        boolean success =
                cartDAO.updateQuantity(
                        user.getId(),
                        matchedProduct.getId(),
                        newQuantity
                );

        if (!success) {

            return "I couldn't update your cart. Please try again! 🍉";
        }

        return "Removed "
                + removeQuantity
                + " "
                + matchedProduct.getName()
                + " from your cart. You now have "
                + newQuantity
                + ". 🍉🛒";
    }

    // =========================
    // EXTRACT PRODUCT NAME
    // =========================

    private String extractProductName(String message) {

        String productName =
                message.replaceFirst(
                        "(?i)^.*?\\b(add|remove|delete|take out)\\b",
                        ""
                ).trim();

        productName =
                productName.replaceAll(
                        "(?i)\\b\\d+\\b",
                        ""
                );

        productName =
                productName.replaceAll(
                        "(?i)\\b(one|two|three|four|five|six|seven|eight|nine|ten)\\b",
                        ""
                );

        productName =
                productName.replaceAll(
                        "(?i)\\b(to|from|in|out of|into)\\s+(my\\s+)?(cart|basket)\\b",
                        ""
                );

        return productName.trim();
    }

    // =========================
    // EXTRACT QUANTITY
    // =========================

    private int extractQuantity(String message) {

        String lower =
                message.toLowerCase(Locale.ROOT);

        String[] words = {
                "one",
                "two",
                "three",
                "four",
                "five",
                "six",
                "seven",
                "eight",
                "nine",
                "ten"
        };

        int[] values = {
                1, 2, 3, 4, 5,
                6, 7, 8, 9, 10
        };

        for (int i = 0; i < words.length; i++) {

            if (lower.matches(
                    ".*\\b"
                            + words[i]
                            + "\\b.*"
            )) {

                return values[i];
            }
        }

        java.util.regex.Matcher matcher =
                java.util.regex.Pattern
                        .compile("\\b(\\d+)\\b")
                        .matcher(lower);

        if (matcher.find()) {

            try {

                int quantity =
                        Integer.parseInt(
                                matcher.group(1)
                        );

                if (quantity > 0) {
                    return quantity;
                }

            } catch (NumberFormatException ignored) {
            }
        }

        return 1;
    }

    // =========================
    // FIND PRODUCT
    // =========================

    private Product findProduct(
            List<Product> products,
            String search
    ) {

        String searchText =
                search.toLowerCase(Locale.ROOT)
                        .trim();

        for (Product product : products) {

            if (product.getName() == null) {
                continue;
            }

            if (product.getName()
                    .toLowerCase(Locale.ROOT)
                    .equals(searchText)) {

                return product;
            }
        }

        Product possibleMatch = null;

        for (Product product : products) {

            if (product.getName() == null) {
                continue;
            }

            String productName =
                    product.getName()
                            .toLowerCase(Locale.ROOT);

            if (productName.contains(searchText)) {

                if (possibleMatch != null) {
                    return null;
                }

                possibleMatch = product;
            }
        }

        return possibleMatch;
    }

    // =========================
    // FIND CART ITEM
    // =========================

    private CartItem findCartItem(
            List<CartItem> cart,
            int productId
    ) {

        for (CartItem item : cart) {

            if (item.getProductId() == productId) {
                return item;
            }
        }

        return null;
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
    // BUILD PRODUCT CONTEXT
    // =========================

    private String buildProductContext(
            List<Product> products
    ) {

        if (products == null ||
                products.isEmpty()) {

            return "There are currently no products available.";
        }

        StringBuilder context =
                new StringBuilder();

        for (Product product : products) {

            context.append("Product: ")
                    .append(product.getName())
                    .append("\n");

            context.append("Description: ")
                    .append(product.getDescription())
                    .append("\n");

            context.append("Price: ₹")
                    .append(product.getPrice())
                    .append("\n");

            context.append("Category: ")
                    .append(product.getCategory())
                    .append("\n");

            context.append("Stock: ")
                    .append(product.getStockQty())
                    .append("\n");

            context.append("\n");
        }

        return context.toString();
    }

    private static class ChatRequest {
        String message;
    }

    private static class ChatResponse {

        String reply;

        ChatResponse(String reply) {
            this.reply = reply;
        }
    }
}