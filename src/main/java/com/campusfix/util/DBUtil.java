package com.campusfix.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * JDBC Connection Utility supporting MySQL and configurable test/Docker databases.
 */
public class DBUtil {

    private static String jdbcUrl;
    private static String jdbcUser;
    private static String jdbcPassword;
    private static String jdbcDriver;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try {
            Properties props = new Properties();
            InputStream in = DBUtil.class.getClassLoader().getResourceAsStream("db.properties");
            if (in != null) {
                props.load(in);
            }

            // Read with environment variables as high-priority overrides (e.g. in Docker/CI)
            jdbcDriver = System.getenv().getOrDefault("DB_DRIVER", props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
            jdbcUrl = System.getenv().getOrDefault("DB_URL", props.getProperty("db.url", "jdbc:mysql://localhost:3306/campusfix_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"));
            jdbcUser = System.getenv().getOrDefault("DB_USER", props.getProperty("db.user", "root"));
            jdbcPassword = System.getenv().getOrDefault("DB_PASSWORD", props.getProperty("db.password", "root"));

            Class.forName(jdbcDriver);
        } catch (Exception e) {
            System.err.println("DBUtil init warning: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
    }

    /**
     * Helper to set custom datasource parameters dynamically (e.g. during JUnit tests with H2)
     */
    public static void setCustomConnection(String driver, String url, String user, String password) {
        try {
            jdbcDriver = driver;
            jdbcUrl = url;
            jdbcUser = user;
            jdbcPassword = password;
            Class.forName(jdbcDriver);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register custom DB driver: " + driver, e);
        }
    }
}
