package com.melon.melonmart.dao;

import com.melon.melonmart.listener.DbContextListener;
import com.melon.melonmart.model.Review;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ReviewDAO {

    private final DataSource dataSource;

    public ReviewDAO() {
        this.dataSource = DbContextListener.getDataSource();
    }

    public boolean canReviewProduct(
        int userId,
        int productId,
        int orderId
) throws Exception {

    String sql = """
            SELECT COUNT(*)
            FROM orders o
            JOIN order_items oi
                ON o.id = oi.order_id
            WHERE o.id = ?
              AND o.buyer_id = ?
              AND oi.product_id = ?
              AND o.status = 'DELIVERED'
            """;

    try (
            Connection connection =
                    dataSource.getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql)
    ) {

        statement.setInt(1, orderId);
        statement.setInt(2, userId);
        statement.setInt(3, productId);

        try (ResultSet result = statement.executeQuery()) {

            if (result.next()) {
                return result.getInt(1) > 0;
            }
        }
    }

    return false;
}

    public boolean hasReviewed(
            int userId,
            int productId,
            int orderId
    ) throws Exception {

        String sql = """
                SELECT COUNT(*)
                FROM reviews
                WHERE user_id = ?
                  AND product_id = ?
                  AND order_id = ?
                """;

        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(1, userId);
            statement.setInt(2, productId);
            statement.setInt(3, orderId);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    return result.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    public int createReview(Review review)
            throws Exception {

        String sql = """
                INSERT INTO reviews
                (user_id, product_id, order_id, rating, comment)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql,
                                java.sql.Statement.RETURN_GENERATED_KEYS
                        )
        ) {

            statement.setInt(
                    1,
                    review.getUserId()
            );

            statement.setInt(
                    2,
                    review.getProductId()
            );

            statement.setInt(
                    3,
                    review.getOrderId()
            );

            statement.setInt(
                    4,
                    review.getRating()
            );

            statement.setString(
                    5,
                    review.getComment()
            );

            statement.executeUpdate();

            try (ResultSet result =
                         statement.getGeneratedKeys()) {

                if (result.next()) {
                    return result.getInt(1);
                }
            }
        }

        return 0;
    }

    public List<Review> getReviewsByProductId(
            int productId
    ) throws Exception {

        String sql = """
                SELECT
                    r.id,
                    r.user_id,
                    r.product_id,
                    r.order_id,
                    r.rating,
                    r.comment,
                    r.created_at,
                    u.name AS user_name
                FROM reviews r
                JOIN users u ON u.id = r.user_id
                WHERE r.product_id = ?
                ORDER BY created_at DESC
                """;

        List<Review> reviews =
                new ArrayList<>();

        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    productId
            );

            try (ResultSet result =
                         statement.executeQuery()) {

                while (result.next()) {

                    Review review =
                            new Review();

                    review.setId(
                            result.getInt("id")
                    );

                    review.setUserId(
                            result.getInt("user_id")
                    );

                    review.setProductId(
                            result.getInt("product_id")
                    );

                    review.setOrderId(
                            result.getInt("order_id")
                    );

                    review.setRating(
                            result.getInt("rating")
                    );

                    review.setComment(
                            result.getString("comment")
                    );
                    review.setUserName(
                            result.getString("user_name")
                    );

                    review.setCreatedAt(
                            result.getTimestamp("created_at")
                    );

                    reviews.add(review);
                }
            }
        }

        return reviews;
    }

    public double getAverageRating(
            int productId
    ) throws Exception {

        String sql = """
                SELECT AVG(rating)
                FROM reviews
                WHERE product_id = ?
                """;

        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    productId
            );

            try (ResultSet result =
                         statement.executeQuery()) {

                if (result.next()) {

                    double average =
                            result.getDouble(1);

                    if (result.wasNull()) {
                        return 0.0;
                    }

                    return average;
                }
            }
        }

        return 0.0;
    }
}