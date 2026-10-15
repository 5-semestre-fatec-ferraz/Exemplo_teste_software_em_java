package com.gwj.model.dataAccessObject;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;

import com.gwj.AppConfig;

public class ConnectionPostGreSQL {

    private static ConnectionPostGreSQL instance;

    private ConnectionPostGreSQL() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Erro ao carregar Driver PostgreSQL", e);
        }
    }

    public static synchronized ConnectionPostGreSQL getInstance() {
        if (instance == null) {
            instance = new ConnectionPostGreSQL();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(AppConfig.DB_URL, AppConfig.DB_USER, AppConfig.DB_PASS);
    }
}
