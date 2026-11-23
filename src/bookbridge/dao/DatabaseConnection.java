
package bookbridge.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL =
            "jdbc:mariadb://localhost:3306/bookbridge_tsa4";

    private static final String USER =
            System.getenv().getOrDefault("BOOKBRIDGE_DB_USER", "root");

    private static final String PASSWORD =
            System.getenv().getOrDefault("BOOKBRIDGE_DB_PASSWORD", "");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void main(String[] args) {

        System.out.println("=== BOOKBRIDGE TSA4 DATABASE TEST ===");

        try (Connection connection = getConnection()) {

            if (connection.isValid(5)) {
                System.out.println("Database Connection: SUCCESS");
                System.out.println("Database: bookbridge_tsa4");
            } else {
                System.out.println("Database Connection: FAILED");
            }

        } catch (SQLException e) {
            System.out.println("Database Connection: FAILED");
            System.out.println("Error: " + e.getMessage());
        }
    }
}
