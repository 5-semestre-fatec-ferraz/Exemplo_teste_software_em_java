package com.gwj.model.dataAccessObject;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;

import com.gwj.AppConfig;

public class ConnectionDB {

    private static ConnectionDB instance;

    private ConnectionDB() {
        if (!AppConfig.isPostgreSql()) {
            try {
                Class.forName("org.mariadb.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException ex) {
                    throw new RuntimeException("Erro ao carregar Driver JDBC MariaDB/MySQL", ex);
                }
            }
        }
    }

    public static synchronized ConnectionDB getInstance() {
        if (instance == null) {
            instance = new ConnectionDB();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (AppConfig.isPostgreSql()) {
            return ConnectionPostGreSQL.getInstance().getConnection();
        }
        return DriverManager.getConnection(AppConfig.DB_URL, AppConfig.DB_USER, AppConfig.DB_PASS);
    }
}
