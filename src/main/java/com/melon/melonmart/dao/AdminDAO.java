package com.melon.melonmart.dao;

import com.melon.melonmart.listener.DbContextListener;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
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