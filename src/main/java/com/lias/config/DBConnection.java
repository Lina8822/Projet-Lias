package com.lias.config;
import java.sql.*;

public class DBConnection {
    public static String setting(String name, String fallback) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
    public static Connection getConnection() throws SQLException {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException e) { throw new SQLException("Pilote MySQL indisponible", e); }
        return DriverManager.getConnection(setting("LIAS_DB_URL", "jdbc:mysql://localhost:3306/lias_data_base?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"), setting("LIAS_DB_USER", "root"), setting("LIAS_DB_PASSWORD", ""));
    }
}
