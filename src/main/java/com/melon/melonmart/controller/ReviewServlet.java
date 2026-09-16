package com.melon.melonmart.controller;

import com.google.gson.Gson;
import com.melon.melonmart.dao.ReviewDAO;
import com.melon.melonmart.model.Review;
import com.melon.melonmart.model.User;

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

@WebServlet("/api/reviews/*")
public class ReviewServlet extends HttpServlet {

    private ReviewDAO reviewDAO;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        reviewDAO = new ReviewDAO();
        gson = new Gson();
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        String path = request.getPathInfo();

        if (path == null) {
            path = "/";
        }

        if (path.equals("/product")) {
            getProductReviews(request, response);
            return;
        }

        sendError(
                response,
                HttpServletResponse.SC_NOT_FOUND,
                "Review endpoint not found."
        );
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        String path = request.getPathInfo();

        if (path == null) {
            path = "/";
        }

        if (path.equals("/create")) {
            createReview(request, response);
            return;
        }

        sendError(
                response,
                HttpServletResponse.SC_NOT_FOUND,
                "Review endpoint not found."
        );
    }

    private void getProductReviews(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {

            String productIdText =
                    request.getParameter("productId");

            if (productIdText == null) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Product ID is required."
                );

                return;
            }

            int productId =
                    Integer.parseInt(productIdText);

            List<Review> reviews =
                    reviewDAO.getReviewsByProductId(productId);

            double averageRating =
                    reviewDAO.getAverageRating(productId);

            Map<String, Object> result =
                    new HashMap<>();

            result.put(
                    "success",
                    true
            );

            result.put(
                    "reviews",
                    reviews
            );

            result.put(
                    "averageRating",
                    averageRating
            );

            result.put(
                    "reviewCount",
                    reviews.size()
            );

            sendJson(
                    response,
                    HttpServletResponse.SC_OK,
                    result
            );

        } catch (NumberFormatException e) {

            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid product ID."
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to load reviews."
            );
        }
    }

    private void createReview(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {

            HttpSession session =
                    request.getSession(false);

            if (session == null) {

                sendError(
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Please login first."
                );

                return;
            }

            User user =
                    (User) session.getAttribute("user");

            if (user == null) {

                sendError(
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Please login first."
                );

                return;
            }

            String productIdText =
                    request.getParameter("productId");

            String orderIdText =
                    request.getParameter("orderId");

            String ratingText =
                    request.getParameter("rating");

            String comment =
                    request.getParameter("comment");

            if (
                    productIdText == null ||
                    orderIdText == null ||
                    ratingText == null
            ) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Product ID, order ID and rating are required."
                );

                return;
            }

            int productId =
                    Integer.parseInt(productIdText);

            int orderId =
                    Integer.parseInt(orderIdText);

            int rating =
                    Integer.parseInt(ratingText);

            if (rating < 1 || rating > 5) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Rating must be between 1 and 5."
                );

                return;
            }

           if (
        !reviewDAO.canReviewProduct(
                user.getId(),
                productId,
                orderId
                 )
        ) {

                sendError(
                        response,
                        HttpServletResponse.SC_FORBIDDEN,
                        "You can review a product only after the order is delivered."
                );

                return;
            }

            if (
                    reviewDAO.hasReviewed(
                            user.getId(),
                            productId,
                            orderId
                    )
            ) {

                sendError(
                        response,
                        HttpServletResponse.SC_CONFLICT,
                        "You have already reviewed this product."
                );

                return;
            }

            Review review =
                    new Review();

            review.setUserId(
                    user.getId()
            );

            review.setProductId(
                    productId
            );

            review.setOrderId(
                    orderId
            );

            review.setRating(
                    rating
            );

            review.setComment(
                    comment
            );

            int reviewId =
                    reviewDAO.createReview(review);

            Map<String, Object> result =
                    new HashMap<>();

            result.put(
                    "success",
                    true
            );

            result.put(
                    "reviewId",
                    reviewId
            );

            result.put(
                    "message",
                    "Review submitted successfully."
            );

            sendJson(
                    response,
                    HttpServletResponse.SC_CREATED,
                    result
            );

        } catch (NumberFormatException e) {

            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid review data."
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to create review."
            );
        }
    }

    private void sendJson(
            HttpServletResponse response,
            int status,
            Object data
    ) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                gson.toJson(data)
        );
    }

    private void sendError(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {

        Map<String, Object> result =
                new HashMap<>();

        result.put(
                "success",
                false
        );

        result.put(
                "message",
                message
        );

        sendJson(
                response,
                status,
                result
        );
    }
}