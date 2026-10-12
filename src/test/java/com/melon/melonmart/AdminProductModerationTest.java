package com.melon.melonmart;

import com.melon.melonmart.dao.AdminDAO;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminProductModerationTest {

    @Test
    void adminCanRemoveListingWithoutOrderHistory() throws Exception {
        DataSource dataSource = dataSource();
        createSchema(dataSource);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO products (id, name) VALUES (?, ?)")) {
            insert.setInt(1, 10);
            insert.setString(2, "Unpurchased product");
            insert.executeUpdate();
        }

        AdminDAO dao = new AdminDAO(dataSource);
        assertTrue(dao.productExists(10));
        assertTrue(dao.deleteProductForModeration(10));
        assertFalse(dao.productExists(10));
    }

    @Test
    void adminCannotRemoveProductReferencedByOrderHistory() throws Exception {
        DataSource dataSource = dataSource();
        createSchema(dataSource);
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO products (id, name) VALUES (20, 'Purchased product')");
            statement.executeUpdate("INSERT INTO order_items (id, product_id) VALUES (1, 20)");
        }

        AdminDAO dao = new AdminDAO(dataSource);
        assertTrue(dao.productExists(20));
        assertFalse(dao.deleteProductForModeration(20));
        assertTrue(dao.productExists(20));
    }

    @Test
    void missingProductIsNotReportedAsDeleted() throws Exception {
        DataSource dataSource = dataSource();
        createSchema(dataSource);
        AdminDAO dao = new AdminDAO(dataSource);

        assertFalse(dao.productExists(999));
        assertFalse(dao.deleteProductForModeration(999));
    }

    private DataSource dataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:admin_moderation_"
                + UUID.randomUUID().toString().replace("-", "")
                + ";DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private void createSchema(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE products (id INT PRIMARY KEY, name VARCHAR(150) NOT NULL)");
            statement.execute("CREATE TABLE order_items (id INT PRIMARY KEY, product_id INT NOT NULL)");
        }
    }
}
