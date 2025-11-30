package controller;

import database.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import model.Booking;
import model.Table;
import model.BookingStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainController {
    private ObservableList<Booking> bookings;
    private ObservableList<Table> tables;
    private FilteredList<Booking> filteredBookings;
    private int nextBookingId = 1;
    private static final Logger logger = Logger.getLogger(MainController.class.getName());

    public MainController() {
        this.bookings = FXCollections.observableArrayList();
        this.tables = FXCollections.observableArrayList();
        this.filteredBookings = new FilteredList<>(bookings);

        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            DatabaseConnection.testConnection();
            loadTablesFromDatabase();
            loadBookingsFromDatabase();
            logger.info("База данных инициализирована успешно");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка инициализации БД", e);
            initializeSampleData();
        }
    }

    private void loadTablesFromDatabase() {
        String sql = "SELECT id, table_number, capacity, location FROM tables WHERE is_active = true ORDER BY table_number";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            tables.clear();
            while (rs.next()) {
                Table table = new Table(
                        rs.getInt("table_number"),
                        rs.getInt("capacity"),
                        rs.getString("location")
                );
                tables.add(table);
            }
            logger.info("Загружено столов: " + tables.size());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка загрузки столов из БД", e);
            throw new RuntimeException("Не удалось загрузить столы из БД", e);
        }
    }

    private void loadBookingsFromDatabase() {
        String sql = "SELECT b.id, c.name, c.phone, b.guests, b.booking_date_time, " +
                "t.id as table_id, t.table_number, t.capacity, t.location, " +
                "bs.name as status, b.special_requests " +
                "FROM bookings b " +
                "JOIN customers c ON b.customer_id = c.id " +
                "JOIN tables t ON b.table_id = t.id " +
                "JOIN booking_status bs ON b.status_id = bs.id " +
                "ORDER BY b.booking_date_time DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            bookings.clear();
            int maxId = 0;

            while (rs.next()) {
                // Создаем объект стола
                Table table = findTableById(rs.getInt("table_id"));
                if (table == null) {
                    table = new Table(
                            rs.getInt("table_number"),
                            rs.getInt("capacity"),
                            rs.getString("location")
                    );
                }

                // Создаем бронирование
                Booking booking = new Booking(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getInt("guests"),
                        rs.getTimestamp("booking_date_time").toLocalDateTime(),
                        table,
                        rs.getString("special_requests")
                );

                // Установка статуса
                String status = rs.getString("status");
                booking.setStatus(convertToBookingStatus(status));

                bookings.add(booking);
                maxId = Math.max(maxId, rs.getInt("id"));
            }

            nextBookingId = maxId + 1;
            logger.info("Загружено бронирований: " + bookings.size());

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка загрузки бронирований из БД", e);
            throw new RuntimeException("Не удалось загрузить бронирования из БД", e);
        }
    }

    private Table findTableById(int tableId) {
        String sql = "SELECT table_number, capacity, location FROM tables WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, tableId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Table(
                            rs.getInt("table_number"),
                            rs.getInt("capacity"),
                            rs.getString("location")
                    );
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Не удалось найти стол по ID: " + tableId, e);
        }
        return null;
    }

    private BookingStatus convertToBookingStatus(String status) {
        switch (status.toUpperCase()) {
            case "CONFIRMED":
                return BookingStatus.CONFIRMED;
            case "CANCELLED":
                return BookingStatus.CANCELLED;
            case "COMPLETED":
                return BookingStatus.COMPLETED;
            default:
                return BookingStatus.PENDING;
        }
    }

    private int convertToStatusId(BookingStatus status) {
        switch (status) {
            case CONFIRMED:
                return 2;
            case CANCELLED:
                return 3;
            case COMPLETED:
                return 4;
            default:
                return 1;
        }
    }

    // Методы для работы с бронированиями
    public ObservableList<Booking> getBookings() {
        return bookings;
    }

    public FilteredList<Booking> getFilteredBookings() {
        return filteredBookings;
    }

    public void addBooking(Booking booking) {
        try {
            int bookingId = saveBookingToDatabase(booking);
            booking.idProperty().set(bookingId);

            bookings.add(booking);
            nextBookingId = Math.max(nextBookingId, bookingId + 1);

            logger.info("Добавлено новое бронирование ID: " + bookingId);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при добавлении бронирования", e);
            throw new RuntimeException("Не удалось сохранить бронирование в БД", e);
        }
    }

    private int saveBookingToDatabase(Booking booking) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. сохранение клиента
            int customerId = saveOrGetCustomer(conn, booking.getCustomerName(), booking.getPhone());

            // 2. получение ID стола
            int tableId = getTableId(conn, booking.getTable().getTableNumber());

            // 3. Сохраняем бронирование
            String sql = "INSERT INTO bookings (customer_id, table_id, status_id, guests, booking_date_time, special_requests) " +
                    "VALUES (?, ?, ?, ?, ?, ?) RETURNING id";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, customerId);
            stmt.setInt(2, tableId);
            stmt.setInt(3, convertToStatusId(booking.getStatus()));
            stmt.setInt(4, booking.getGuests());
            stmt.setTimestamp(5, Timestamp.valueOf(booking.getBookingDateTime()));
            stmt.setString(6, booking.getSpecialRequests());

            rs = stmt.executeQuery();

            int bookingId;
            if (rs.next()) {
                bookingId = rs.getInt(1);
            } else {
                throw new SQLException("Не удалось получить ID созданного бронирования");
            }

            conn.commit();
            return bookingId;

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    private int saveOrGetCustomer(Connection conn, String name, String phone) throws SQLException {
        //поиск данных
        String findSql = "SELECT id FROM customers WHERE phone = ?";
        try (PreparedStatement findStmt = conn.prepareStatement(findSql)) {
            findStmt.setString(1, phone);
            try (ResultSet rs = findStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        // Если не найден, создаем нового
        String insertSql = "INSERT INTO customers (name, phone) VALUES (?, ?) RETURNING id";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setString(1, name);
            insertStmt.setString(2, phone);
            try (ResultSet rs = insertStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    throw new SQLException("Не удалось получить ID созданного клиента");
                }
            }
        }
    }

    private int getTableId(Connection conn, int tableNumber) throws SQLException {
        String sql = "SELECT id FROM tables WHERE table_number = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, tableNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    throw new SQLException("Стол с номером " + tableNumber + " не найден");
                }
            }
        }
    }

    public void updateBooking(Booking oldBooking, Booking newBooking) {
        try {
            updateBookingInDatabase(newBooking);

            // Обновляем в локальном списке
            int index = bookings.indexOf(oldBooking);
            if (index != -1) {
                bookings.set(index, newBooking);
            }

            logger.info("Обновлено бронирование ID: " + newBooking.getId());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при обновлении бронирования", e);
            throw new RuntimeException("Не удалось обновить бронирование в БД", e);
        }
    }

    //обновление данных в бд
    private void updateBookingInDatabase(Booking booking) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Обновляем клиента
            int customerId = saveOrGetCustomer(conn, booking.getCustomerName(), booking.getPhone());

            // Получаем ID стола
            int tableId = getTableId(conn, booking.getTable().getTableNumber());

            // Обновляем бронирование
            String sql = "UPDATE bookings SET customer_id = ?, table_id = ?, status_id = ?, " +
                    "guests = ?, booking_date_time = ?, special_requests = ?, updated_at = CURRENT_TIMESTAMP " +
                    "WHERE id = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, customerId);
            stmt.setInt(2, tableId);
            stmt.setInt(3, convertToStatusId(booking.getStatus()));
            stmt.setInt(4, booking.getGuests());
            stmt.setTimestamp(5, Timestamp.valueOf(booking.getBookingDateTime()));
            stmt.setString(6, booking.getSpecialRequests());
            stmt.setInt(7, booking.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Бронирование с ID " + booking.getId() + " не найдено");
            }

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    public void deleteBooking(Booking booking) {
        try {
            deleteBookingFromDatabase(booking.getId());
            bookings.remove(booking);
            logger.info("Удалено бронирование ID: " + booking.getId());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при удалении бронирования", e);
            throw new RuntimeException("Не удалось удалить бронирование из БД", e);
        }
    }

    private void deleteBookingFromDatabase(int bookingId) throws SQLException {
        String sql = "DELETE FROM bookings WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bookingId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Бронирование с ID " + bookingId + " не найдено");
            }
        }
    }

    public ObservableList<Table> getTables() {
        return tables;
    }

    public ObservableList<Table> getAvailableTables(int guests, LocalDateTime dateTime) {
        ObservableList<Table> availableTables = FXCollections.observableArrayList();

        for (Table table : tables) {
            if (table.getCapacity() >= guests && isTableAvailable(table, dateTime)) {
                availableTables.add(table);
            }
        }
        return availableTables;
    }

    private boolean isTableAvailable(Table table, LocalDateTime dateTime) {
        for (Booking booking : bookings) {
            if (booking.getTable().equals(table) &&
                    booking.getBookingDateTime().toLocalDate().equals(dateTime.toLocalDate()) &&
                    Math.abs(booking.getBookingDateTime().getHour() - dateTime.getHour()) < 2 &&
                    booking.getStatus() != BookingStatus.CANCELLED) {
                return false;
            }
        }
        return true;
    }

    // Фильтрация
    public void applyFilter(LocalDateTime date) {
        filteredBookings.setPredicate(booking -> {
            if (date == null) return true;
            return booking.getBookingDateTime().toLocalDate().equals(date.toLocalDate());
        });
    }

    public void clearFilter() {
        filteredBookings.setPredicate(null);
    }

    // Статистика
    public int getTotalBookings() {
        return bookings.size();
    }

    public int getConfirmedBookings() {
        return (int) bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .count();
    }

    public int getPendingBookings() {
        return (int) bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING)
                .count();
    }

    public int getNextBookingId() {
        return nextBookingId;
    }

    public ObservableList<Booking> getBookingsForTable(Table table) {
        ObservableList<Booking> tableBookings = FXCollections.observableArrayList();
        for (Booking booking : bookings) {
            if (booking.getTable().equals(table)) {
                tableBookings.add(booking);
            }
        }
        return tableBookings;
    }

    private void initializeSampleData() {
        tables.addAll(
                new Table(1, 2, "У окна"),
                new Table(2, 4, "Центр зала"),
                new Table(3, 6, "VIP зона"),
                new Table(4, 2, "Терраса"),
                new Table(5, 8, "Банкетный зал"),
                new Table(6, 4, "Барная стойка")
        );

//        bookings.addAll(
//                new Booking(nextBookingId++, "Иван Иванов", "+79161234567", 4,
//                        LocalDateTime.now().plusDays(1), tables.get(1), "День рождения"),
//                new Booking(nextBookingId++, "Мария Петрова", "+79167654321", 2,
//                        LocalDateTime.now().plusDays(2), tables.get(0), "Романтический ужин"),
//                new Booking(nextBookingId++, "Алексей Сидоров", "+79169998877", 6,
//                        LocalDateTime.now().plusHours(5), tables.get(2), "Корпоратив")
//        );

        bookings.get(0).setStatus(BookingStatus.CONFIRMED);
        bookings.get(1).setStatus(BookingStatus.PENDING);
        bookings.get(2).setStatus(BookingStatus.COMPLETED);
    }

}