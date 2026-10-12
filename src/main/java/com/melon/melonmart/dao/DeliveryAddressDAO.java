package com.melon.melonmart.dao;

import com.melon.melonmart.listener.DbContextListener;
import com.melon.melonmart.model.DeliveryAddress;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class DeliveryAddressDAO {
    private final DataSource dataSource;

    public DeliveryAddressDAO() {
        this(DbContextListener.getDataSource());
    }

    public DeliveryAddressDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<DeliveryAddress> findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM delivery_addresses WHERE user_id = ? ORDER BY is_default DESC, id";
        List<DeliveryAddress> addresses = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) addresses.add(map(result));
            }
        }
        return addresses;
    }

    public DeliveryAddress create(int userId, DeliveryAddress address) throws SQLException {
        String sql = """
                INSERT INTO delivery_addresses
                    (user_id, label, address, city, state, pincode, landmark,
                     latitude, longitude, accuracy, is_default)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                boolean makeDefault;
                try (PreparedStatement count = connection.prepareStatement(
                        "SELECT COUNT(*) FROM delivery_addresses WHERE user_id = ?")) {
                    count.setInt(1, userId);
                    try (ResultSet result = count.executeQuery()) {
                        result.next();
                        makeDefault = result.getInt(1) == 0;
                    }
                }
                try (PreparedStatement insert = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    insert.setInt(1, userId);
                    insert.setString(2, address.getLabel());
                    insert.setString(3, address.getAddress());
                    insert.setString(4, address.getCity());
                    insert.setString(5, address.getState());
                    insert.setString(6, address.getPincode());
                    insert.setString(7, address.getLandmark());
                    setNullableDouble(insert, 8, address.getLatitude());
                    setNullableDouble(insert, 9, address.getLongitude());
                    setNullableDouble(insert, 10, address.getAccuracy());
                    insert.setBoolean(11, makeDefault);
                    insert.executeUpdate();
                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Address insert did not return an ID.");
                        address.setId(keys.getInt(1));
                    }
                }
                address.setUserId(userId);
                address.setDefault(makeDefault);
                connection.commit();
                return address;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public boolean setDefault(int userId, int addressId) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement clear = connection.prepareStatement(
                        "UPDATE delivery_addresses SET is_default = FALSE WHERE user_id = ?")) {
                    clear.setInt(1, userId);
                    clear.executeUpdate();
                }
                int updated;
                try (PreparedStatement select = connection.prepareStatement(
                        "UPDATE delivery_addresses SET is_default = TRUE WHERE id = ? AND user_id = ?")) {
                    select.setInt(1, addressId);
                    select.setInt(2, userId);
                    updated = select.executeUpdate();
                }
                if (updated == 0) {
                    connection.rollback();
                    return false;
                }
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public boolean delete(int userId, int addressId) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                boolean wasDefault = false;
                try (PreparedStatement lookup = connection.prepareStatement(
                        "SELECT is_default FROM delivery_addresses WHERE id = ? AND user_id = ?")) {
                    lookup.setInt(1, addressId);
                    lookup.setInt(2, userId);
                    try (ResultSet result = lookup.executeQuery()) {
                        if (!result.next()) {
                            connection.rollback();
                            return false;
                        }
                        wasDefault = result.getBoolean(1);
                    }
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM delivery_addresses WHERE id = ? AND user_id = ?")) {
                    delete.setInt(1, addressId);
                    delete.setInt(2, userId);
                    if (delete.executeUpdate() == 0) {
                        connection.rollback();
                        return false;
                    }
                }
                if (wasDefault) {
                    try (PreparedStatement promote = connection.prepareStatement("""
                            UPDATE delivery_addresses SET is_default = TRUE
                            WHERE user_id = ? AND id = (
                                SELECT MIN(id) FROM delivery_addresses WHERE user_id = ?
                            )
                            """)) {
                        promote.setInt(1, userId);
                        promote.setInt(2, userId);
                        promote.executeUpdate();
                    }
                }
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private void setNullableDouble(PreparedStatement statement, int index, Double value) throws SQLException {
        if (value == null) statement.setNull(index, Types.DECIMAL);
        else statement.setDouble(index, value);
    }

    private DeliveryAddress map(ResultSet result) throws SQLException {
        DeliveryAddress address = new DeliveryAddress();
        address.setId(result.getInt("id"));
        address.setUserId(result.getInt("user_id"));
        address.setLabel(result.getString("label"));
        address.setAddress(result.getString("address"));
        address.setCity(result.getString("city"));
        address.setState(result.getString("state"));
        address.setPincode(result.getString("pincode"));
        address.setLandmark(result.getString("landmark"));
        address.setLatitude(nullableDouble(result, "latitude"));
        address.setLongitude(nullableDouble(result, "longitude"));
        address.setAccuracy(nullableDouble(result, "accuracy"));
        address.setDefault(result.getBoolean("is_default"));
        return address;
    }

    private Double nullableDouble(ResultSet result, String column) throws SQLException {
        double value = result.getDouble(column);
        return result.wasNull() ? null : value;
    }
}
