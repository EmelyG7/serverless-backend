package org.pucmm.practica.serverleslambdaaws.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);

    //contrasena de sql user
    // Md3vIHmSjvOuG_CMaK--1Q
    // These should be stored in AWS Secrets Manager in a production environment
    private static final String DB_URL = System.getenv("COCKROACH_URL");
    private static final String DB_USER = System.getenv("COCKROACH_USER");
    private static final String DB_PASSWORD = System.getenv("COCKROACH_PASSWORD");

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                logger.info("Database connection established successfully");

                // Create table if not exists
                createTablesIfNotExist();
            } catch (ClassNotFoundException e) {
                logger.error("PostgreSQL JDBC driver not found", e);
                throw new SQLException("PostgreSQL JDBC driver not found", e);
            }
        }
        return connection;
    }

    private static void createTablesIfNotExist() throws SQLException {
        String createReservationsTableSQL =
                "CREATE TABLE IF NOT EXISTS reservacion (" +
                        "id VARCHAR(36) PRIMARY KEY, " +
                        "email VARCHAR(100) NOT NULL, " +
                        "name VARCHAR(100) NOT NULL, " +
                        "student_id VARCHAR(20) NOT NULL, " +
                        "laboratory VARCHAR(50) NOT NULL, " +
                        "reservation_time TIMESTAMP NOT NULL, " +
                        "active BOOLEAN DEFAULT TRUE" +
                        ")";

        try (var statement = connection.createStatement()) {
            statement.execute(createReservationsTableSQL);
            logger.info("Reservacion table created or already exists");
        }
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Database connection closed successfully");
            } catch (SQLException e) {
                logger.error("Error closing database connection", e);
            }
        }
    }
}