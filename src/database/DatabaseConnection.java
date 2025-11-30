package database;

import java.sql.*;
import java.util.Properties;

public class DatabaseConnection {
    private static final String URL = "jdbc:postgresql://localhost:5432/cafe_booking";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "muliko08m";

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            Properties props = new Properties();
            props.setProperty("user", USERNAME);
            props.setProperty("password", PASSWORD);
            props.setProperty("ssl", "false");

            connection = DriverManager.getConnection(URL, props);
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("Подключение к PostgreSQL установлено успешно!");
        } catch (SQLException e) {
            System.out.println("Ошибка подключения к PostgreSQL: " + e.getMessage());
        }
    }

//    public static class TestApp {
//        public static void main(String[] args) {
//            try {
//                // Тест подключения к БД
//                database.DatabaseConnection.testConnection();
//
//                // Тест контроллера
//                controller.MainController controller = new controller.MainController();
//                System.out.println(" Контроллер инициализирован успешно!");
//                System.out.println(" Столов загружено: " + controller.getTables().size());
//                System.out.println(" Бронирований загружено: " + controller.getBookings().size());
//
//            } catch (Exception e) {
//                System.out.println(" Ошибка при тестировании: " + e.getMessage());
//                e.printStackTrace();
//            }
//        }
//    }
}