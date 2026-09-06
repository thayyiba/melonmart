package com.melon.melonmart.util;

import com.melon.melonmart.listener.DbContextListener;

import java.sql.Connection;
import java.sql.SQLException;

public class DBUtil {

    public static Connection getConnection() throws SQLException {
        if (DbContextListener.getDataSource() != null) {
            return DbContextListener.getDataSource().getConnection();
        }
        throw new SQLException("DataSource has not been initialized by DbContextListener.");
    }
}