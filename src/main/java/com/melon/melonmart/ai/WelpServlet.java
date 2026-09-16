package com.melon.melonmart.ai;

import com.google.gson.Gson;
import com.melon.melonmart.dao.CartDAO;
import com.melon.melonmart.dao.ProductDAO;
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

            /*
             * First check whether the user is
             * asking Welp to add something
             * to the cart.
             */
            if (isAddToCartRequest(message)) {

                String cartReply =
                        handleAddToCart(
                                request,
                                message
                        );

                response.getWriter().write(
                        gson.toJson(
                                new ChatResponse(cartReply)
                        )
                );

                return;
            }

            /*
             * Normal questions continue to Gemini.
             */
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
    // CHECK ADD-TO-CART REQUEST
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
    // HANDLE ADD TO CART
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

        boolean success =
                cartDAO.addToCart(
                        user.getId(),
                        matchedProduct.getId(),
                        1
                );

        if (!success) {

            return "I couldn't add "
                    + matchedProduct.getName()
                    + " to your cart. Please try again! 🍉";
        }

        return "Added "
                + matchedProduct.getName()
                + " to your cart! 🍉🛒";
    }

    // =========================
    // EXTRACT PRODUCT NAME
    // =========================

    private String extractProductName(String message) {

        String lower =
                message.toLowerCase(Locale.ROOT);

        int addIndex =
                lower.indexOf("add");

        if (addIndex == -1) {
            return "";
        }

        String productName =
                message.substring(addIndex + 3).trim();

        productName =
                productName.replaceAll(
                        "(?i)\\bto\\s+(my\\s+)?cart\\b",
                        ""
                );

        productName =
                productName.replaceAll(
                        "(?i)\\bin\\s+(my\\s+)?cart\\b",
                        ""
                );

        productName =
                productName.replaceAll(
                        "(?i)\\binto\\s+(my\\s+)?cart\\b",
                        ""
                );

        productName =
                productName.replaceAll(
                        "(?i)\\bto\\s+(my\\s+)?basket\\b",
                        ""
                );

        productName =
                productName.replaceAll(
                        "(?i)\\bin\\s+(my\\s+)?basket\\b",
                        ""
                );

        productName =
                productName.replaceAll(
                        "(?i)\\binto\\s+(my\\s+)?basket\\b",
                        ""
                );

        return productName.trim();
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

        /*
         * First try an exact product-name match.
         */
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

        /*
         * If there is no exact match,
         * allow a partial match.
         *
         * Example:
         * "yumzie"
         * matches
         * "Yumzie Tiramisu"
         */
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

                    /*
                     * More than one product matched,
                     * so don't guess.
                     */
                    return null;
                }

                possibleMatch = product;
            }
        }

        return possibleMatch;
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

    // =========================
    // REQUEST / RESPONSE
    // =========================

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