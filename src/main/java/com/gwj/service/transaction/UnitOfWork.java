package com.gwj.service.transaction;

import com.gwj.model.dataAccessObject.ConnectionDB;
import java.sql.Connection;
import java.sql.SQLException;

public class UnitOfWork implements AutoCloseable {
    private static final ThreadLocal<Connection> currentConnection = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> inTransaction = ThreadLocal.withInitial(() -> false);

    public static Connection getConnection() throws SQLException {
        Connection conn = currentConnection.get();
        if (conn == null || conn.isClosed()) {
            conn = ConnectionDB.getInstance().getConnection();
            currentConnection.set(conn);
        }
        return conn;
    }

    public static void beginTransaction() throws SQLException {
        Connection conn = getConnection();
        conn.setAutoCommit(false);
        inTransaction.set(true);
    }

    public static void commit() throws SQLException {
        Connection conn = currentConnection.get();
        if (conn != null && !conn.isClosed() && inTransaction.get()) {
            conn.commit();
            inTransaction.set(false);
        }
    }

    public static void rollback() {
        Connection conn = currentConnection.get();
        try {
            if (conn != null && !conn.isClosed() && inTransaction.get()) {
                conn.rollback();
            }
        } catch (SQLException e) {
            System.err.println("Erro ao efetuar rollback: " + e.getMessage());
        } finally {
            inTransaction.set(false);
        }
    }

    @Override
    public void close() {
        Connection conn = currentConnection.get();
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Erro ao fechar conexão no UoW: " + e.getMessage());
            } finally {
                currentConnection.remove();
                inTransaction.remove();
            }
        }
    }
}
