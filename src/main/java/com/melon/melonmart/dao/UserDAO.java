package com.melon.melonmart.dao;

import com.melon.melonmart.model.User;
import com.melon.melonmart.listener.DbContextListener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDAO {

    private Connection getConnection() throws Exception {
        return DbContextListener.getDataSource().getConnection();
    }

    // Register a new user
    public boolean registerUser(User user) {

        String sql = """
                INSERT INTO users (name, email, password, role)
                VALUES (?, ?, ?, ?)
                """;

        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPassword());

            String role = user.getRole();

            if (role == null || role.isBlank()) {
                role = "BUYER";
            }

            statement.setString(4, role);

            int rows = statement.executeUpdate();

            return rows > 0;

        } catch (Exception e) {

            System.err.println("Error registering user:");
            e.printStackTrace();

            return false;
        }
    }

    // Find user by email
    public User findByEmail(String email) {

        String sql = """
                SELECT id, name, email, password, role
                FROM users
                WHERE email = ?
                """;

        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, email);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    return extractUser(result);
                }
            }

        } catch (Exception e) {

            System.err.println("Error finding user by email:");
            e.printStackTrace();
        }

        return null;
    }

    // Find user by ID
    public User findById(int id) {

        String sql = """
                SELECT id, name, email, password, role
                FROM users
                WHERE id = ?
                """;

        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setInt(1, id);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    return extractUser(result);
                }
            }

        } catch (Exception e) {

            System.err.println("Error finding user by ID:");
            e.printStackTrace();
        }

        return null;
    }

    // Convert database row into User object
    private User extractUser(ResultSet result) throws Exception {

        User user = new User();

        user.setId(result.getInt("id"));

        // Database uses "name",
        // Java model uses "username"
        user.setUsername(result.getString("name"));

        user.setEmail(result.getString("email"));
        user.setPassword(result.getString("password"));
        user.setRole(result.getString("role"));

        return user;
    }
}