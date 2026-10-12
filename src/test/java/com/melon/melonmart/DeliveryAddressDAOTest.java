package com.melon.melonmart;

import com.melon.melonmart.dao.DeliveryAddressDAO;
import com.melon.melonmart.model.DeliveryAddress;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryAddressDAOTest {
    @Test
    void createsSelectsAndDeletesAddressesWithoutCrossingUsers() throws Exception {
        JdbcDataSource dataSource = newDataSource();
        DeliveryAddressDAO dao = new DeliveryAddressDAO(dataSource);

        DeliveryAddress home = dao.create(1, address("Home", "12 Mango Street"));
        assertTrue(home.isDefault());

        DeliveryAddress work = dao.create(1, address("College", "42 Campus Road"));
        assertTrue(work.isDefault());

        List<DeliveryAddress> saved = dao.findByUserId(1);
        assertEquals(2, saved.size());
        assertFalse(saved.stream().filter(a -> a.getId() == home.getId()).findFirst().orElseThrow().isDefault());
        assertTrue(saved.stream().filter(a -> a.getId() == work.getId()).findFirst().orElseThrow().isDefault());

        assertFalse(dao.setDefault(2, home.getId()), "Another user must not select this address.");
        assertTrue(dao.setDefault(1, home.getId()));
        assertTrue(dao.delete(1, home.getId()));

        List<DeliveryAddress> remaining = dao.findByUserId(1);
        assertEquals(1, remaining.size());
        assertEquals(work.getId(), remaining.get(0).getId());
        assertTrue(remaining.get(0).isDefault());
        assertTrue(dao.findByUserId(2).isEmpty());
        assertFalse(dao.delete(2, work.getId()), "Another user must not delete this address.");
        assertTrue(dao.delete(1, work.getId()), "A user must be able to remove the final saved address.");
        assertTrue(dao.findByUserId(1).isEmpty(), "Removing the final address should leave the account empty.");
    }

    private JdbcDataSource newDataSource() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:delivery_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE users (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        email VARCHAR(150) NOT NULL UNIQUE,
                        password_hash VARCHAR(255) NOT NULL,
                        role VARCHAR(20) NOT NULL DEFAULT 'BUYER'
                    )
                    """);
            statement.execute("INSERT INTO users (name,email,password_hash,role) VALUES "
                    + "('Buyer One','buyer1@example.test','hash','BUYER'),"
                    + "('Buyer Two','buyer2@example.test','hash','BUYER')");
            statement.execute("""
                    CREATE TABLE delivery_addresses (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT NOT NULL,
                        label VARCHAR(80) NOT NULL,
                        address VARCHAR(500) NOT NULL,
                        city VARCHAR(100),
                        state VARCHAR(100),
                        pincode VARCHAR(10),
                        landmark VARCHAR(150),
                        latitude DECIMAL(10,7),
                        longitude DECIMAL(10,7),
                        accuracy DECIMAL(10,2),
                        is_default BOOLEAN NOT NULL DEFAULT FALSE,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_delivery_addresses_user FOREIGN KEY (user_id)
                            REFERENCES users(id) ON DELETE CASCADE
                    )
                    """);
        }
        return dataSource;
    }

    private DeliveryAddress address(String label, String value) {
        DeliveryAddress address = new DeliveryAddress();
        address.setLabel(label);
        address.setAddress(value);
        address.setCity("Coimbatore");
        address.setState("Tamil Nadu");
        address.setPincode("641001");
        return address;
    }
}
