package com.melon.melonmart.dao;

import com.melon.melonmart.dto.AdminProductDTO;
import com.melon.melonmart.dto.AdminUserDTO;
import com.melon.melonmart.listener.DbContextListener;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminDAO {

    private final DataSource dataSource;

    public AdminDAO() {
        this.dataSource =
                DbContextListener.getDataSource();
    }

    public Map<String, Object> getDashboardStats()
            throws Exception {

        Map<String, Object> stats =
                new HashMap<>();

        stats.put(
                "totalUsers",
                getCount(
                        "SELECT COUNT(*) FROM users"
                )
        );

        stats.put(
                "totalSellers",
                getCount(
                        "SELECT COUNT(*) FROM users WHERE role = 'SELLER'"
                )
        );

        stats.put(
                "totalBuyers",
                getCount(
                        "SELECT COUNT(*) FROM users WHERE role = 'BUYER'"
                )
        );

        stats.put(
                "totalAdmins",
                getCount(
                        "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'"
                )
        );

        stats.put(
                "totalProducts",
                getCount(
                        "SELECT COUNT(*) FROM products"
                )
        );

        stats.put(
                "totalOrders",
                getCount(
                        "SELECT COUNT(*) FROM orders"
                )
        );

        stats.put(
                "totalOrderValue",
                getTotalOrderValue()
        );

        return stats;
    }

    public List<AdminUserDTO> getAllUsers()
        throws Exception {

    String sql = """
            SELECT
                id,
                name,
                email,
                role
            FROM users
            ORDER BY id ASC
            """;

    List<AdminUserDTO> users =
            new ArrayList<>();

    try (
            Connection connection =
                    dataSource.getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql);

            ResultSet result =
                    statement.executeQuery()
    ) {

        while (result.next()) {

            AdminUserDTO user =
                    new AdminUserDTO();

            user.setId(
                    result.getInt("id")
            );

            user.setName(
                    result.getString("name")
            );

            user.setEmail(
                    result.getString("email")
            );

            user.setRole(
                    result.getString("role")
            );

            users.add(user);
        }
    }

    return users;
}

public List<AdminProductDTO> getAllProducts()
        throws Exception {

    String sql = """
            SELECT
                p.id,
                p.seller_id,
                u.name AS seller_name,
                p.name,
                p.description,
                p.price,
                p.stock_qty,
                p.category,
                p.image_url
            FROM products p
            LEFT JOIN users u
                ON p.seller_id = u.id
            ORDER BY p.id ASC
            """;

    List<AdminProductDTO> products =
            new ArrayList<>();

    try (
            Connection connection =
                    dataSource.getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql);

            ResultSet result =
                    statement.executeQuery()
    ) {

        while (result.next()) {

            AdminProductDTO product =
                    new AdminProductDTO();

            product.setId(
                    result.getInt("id")
            );

            product.setSellerId(
                    result.getInt("seller_id")
            );

            product.setSellerName(
                    result.getString("seller_name")
            );

            product.setName(
                    result.getString("name")
            );

            product.setDescription(
                    result.getString("description")
            );

            product.setPrice(
                    result.getBigDecimal("price")
            );

            product.setStockQty(
                    result.getInt("stock_qty")
            );

            product.setCategory(
                    result.getString("category")
            );

            product.setImageUrl(
                    result.getString("image_url")
            );

            products.add(product);
        }
    }

    return products;
}


    private int getCount(String sql)
            throws Exception {

        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet result =
                        statement.executeQuery()
        ) {

            if (result.next()) {
                return result.getInt(1);
            }
        }

        return 0;
    }

    private BigDecimal getTotalOrderValue()
            throws Exception {

        String sql = """
                SELECT COALESCE(
                    SUM(total_amount),
                    0
                )
                FROM orders
                """;

        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet result =
                        statement.executeQuery()
        ) {

            if (result.next()) {
                return result.getBigDecimal(1);
            }
        }

        return BigDecimal.ZERO;
    }
}