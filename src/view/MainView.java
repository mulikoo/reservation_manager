package view;

import controller.MainController;
import javafx.application.Application;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import model.Booking;
import model.BookingStatus;
import model.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public class MainView extends Application {
    private MainController controller;
    private TableView<Booking> bookingsTable;
    private TableView<Table> tablesTable;
    private Label totalBookingsLabel;
    private Label confirmedBookingsLabel;
    private Label pendingBookingsLabel;
    private DatePicker filterDatePicker;

    @Override
    public void start(Stage primaryStage) {
        controller = new MainController();

        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
                createBookingsTab(),
                createTablesTab(),
                createStatisticsTab()
        );

        Scene scene = new Scene(tabPane, 1000, 700);

        // Загружаем CSS из classpath
        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS файл не найден, приложение запустится без стилей");
        }

        primaryStage.setTitle("Менеджер бронирования кафе");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Tab createBookingsTab() {
        Tab tab = new Tab("Бронирования");
        tab.setClosable(false);

        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(15));

        // Заголовок и кнопки
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Управление бронированиями");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Button addButton = new Button("Добавить бронирование");
        addButton.getStyleClass().add("button-success");
        addButton.setOnAction(e -> showBookingForm(null));

        Button editButton = new Button("Редактировать");
        editButton.setOnAction(e -> {
            Booking selected = bookingsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showBookingForm(selected);
            } else {
                showAlert("Внимание", "Пожалуйста, выберите бронирование для редактирования");
            }
        });

        Button deleteButton = new Button("Удалить");
        deleteButton.getStyleClass().add("button-danger");
        deleteButton.setOnAction(e -> deleteSelectedBooking());

        Button confirmButton = new Button("Подтвердить");
        confirmButton.getStyleClass().add("button-success");
        confirmButton.setOnAction(e -> changeBookingStatus(BookingStatus.CONFIRMED));

        Button cancelButton = new Button("Отменить");
        cancelButton.getStyleClass().add("button-danger");
        cancelButton.setOnAction(e -> changeBookingStatus(BookingStatus.CANCELLED));

        Button completeButton = new Button("Завершить");
        completeButton.setOnAction(e -> changeBookingStatus(BookingStatus.COMPLETED));

        Button detailsButton = new Button("Подробнее");
        detailsButton.setOnAction(e -> showBookingDetails());

        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        headerBox.getChildren().addAll(titleLabel, addButton, editButton, confirmButton,
                cancelButton, completeButton, deleteButton, detailsButton);

        // Панель фильтрации
        HBox filterBox = new HBox(10);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        Label filterLabel = new Label("Фильтр по дате:");
        filterLabel.getStyleClass().add("form-label");

        filterDatePicker = new DatePicker();
        filterDatePicker.setValue(LocalDate.now());

        Button applyFilterButton = new Button("Применить");
        applyFilterButton.setOnAction(e -> applyFilter());

        Button clearFilterButton = new Button("Сбросить");
        clearFilterButton.setOnAction(e -> clearFilter());

        filterBox.getChildren().addAll(filterLabel, filterDatePicker, applyFilterButton, clearFilterButton);

        // Таблица бронирований
        bookingsTable = createBookingsTable();
        VBox.setVgrow(bookingsTable, Priority.ALWAYS);

        mainLayout.getChildren().addAll(headerBox, filterBox, bookingsTable);
        tab.setContent(mainLayout);

        return tab;
    }

    private TableView<Booking> createBookingsTable() {
        TableView<Booking> table = new TableView<>();
        table.setItems(controller.getFilteredBookings());

        TableColumn<Booking, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());

        TableColumn<Booking, String> nameCol = new TableColumn<>("Клиент");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().customerNameProperty());

        TableColumn<Booking, String> phoneCol = new TableColumn<>("Телефон");
        phoneCol.setCellValueFactory(cellData -> cellData.getValue().phoneProperty());

        TableColumn<Booking, Integer> guestsCol = new TableColumn<>("Гости");
        guestsCol.setCellValueFactory(cellData -> cellData.getValue().guestsProperty().asObject());

        TableColumn<Booking, LocalDateTime> dateCol = new TableColumn<>("Дата и время");
        dateCol.setCellValueFactory(cellData -> cellData.getValue().bookingDateTimeProperty());
        dateCol.setCellFactory(col -> new TableCell<Booking, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toLocalDate() + " " + item.toLocalTime().getHour() + ":00");
                }
            }
        });

        TableColumn<Booking, Integer> tableCol = new TableColumn<>("Стол");
        tableCol.setCellValueFactory(cellData -> cellData.getValue().getTable().tableNumberProperty().asObject());

        TableColumn<Booking, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty().asString());
        statusCol.setCellFactory(col -> new TableCell<Booking, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Стилизация статусов
                    if ("Подтверждено".equals(item)) {
                        getStyleClass().add("status-confirmed");
                    } else if ("Ожидание".equals(item)) {
                        getStyleClass().add("status-pending");
                    } else if ("Отменено".equals(item)) {
                        getStyleClass().add("status-cancelled");
                    } else if ("Завершено".equals(item)) {
                        getStyleClass().add("status-completed");
                    }
                }
            }
        });

        table.getColumns().addAll(idCol, nameCol, phoneCol, guestsCol, dateCol, tableCol, statusCol);
        return table;
    }

    private Tab createTablesTab() {
        Tab tab = new Tab("Столы");
        tab.setClosable(false);

        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(15));

        Label titleLabel = new Label("Доступные столы");
        titleLabel.getStyleClass().add("title");

        // Кнопка для просмотра бронирований стола
        Button viewBookingsButton = new Button("Просмотреть бронирования стола");
        viewBookingsButton.setOnAction(e -> showTableBookings());

        tablesTable = createTablesTable();
        VBox.setVgrow(tablesTable, Priority.ALWAYS);

        mainLayout.getChildren().addAll(titleLabel, viewBookingsButton, tablesTable);
        tab.setContent(mainLayout);

        return tab;
    }

    private TableView<Table> createTablesTable() {
        TableView<Table> table = new TableView<>();
        table.setItems(controller.getTables());

        TableColumn<Table, Integer> numberCol = new TableColumn<>("№ стола");
        numberCol.setCellValueFactory(cellData -> cellData.getValue().tableNumberProperty().asObject());

        TableColumn<Table, Integer> capacityCol = new TableColumn<>("Вместимость");
        capacityCol.setCellValueFactory(cellData -> cellData.getValue().capacityProperty().asObject());

        TableColumn<Table, String> locationCol = new TableColumn<>("Расположение");
        locationCol.setCellValueFactory(cellData -> cellData.getValue().locationProperty());

        TableColumn<Table, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(cellData -> {
            LocalDateTime now = LocalDateTime.now();
            boolean isAvailable = controller.getBookings().stream()
                    .filter(booking -> booking.getTable().equals(cellData.getValue()))
                    .filter(booking -> booking.getStatus() != BookingStatus.CANCELLED)
                    .noneMatch(booking -> booking.getBookingDateTime().toLocalDate().equals(now.toLocalDate()) &&
                            Math.abs(booking.getBookingDateTime().getHour() - now.getHour()) < 2);
            return new javafx.beans.property.SimpleStringProperty(isAvailable ? "Свободен" : "Занят");
        });
        statusCol.setCellFactory(col -> new TableCell<Table, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("Свободен".equals(item)) {
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    }
                }
            }
        });

        table.getColumns().addAll(numberCol, capacityCol, locationCol, statusCol);
        return table;
    }

    private Tab createStatisticsTab() {
        Tab tab = new Tab("Статистика");
        tab.setClosable(false);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);

        // Карточки статистики
        VBox totalCard = createStatCard("Всего бронирований", "0", "#3498db");
        VBox confirmedCard = createStatCard("Подтвержденных", "0", "#27ae60");
        VBox pendingCard = createStatCard("Ожидающих", "0", "#f39c12");

        grid.add(totalCard, 0, 0);
        grid.add(confirmedCard, 1, 0);
        grid.add(pendingCard, 2, 0);

        // Обновление статистики
        totalBookingsLabel = (Label) totalCard.getChildren().get(0);
        confirmedBookingsLabel = (Label) confirmedCard.getChildren().get(0);
        pendingBookingsLabel = (Label) pendingCard.getChildren().get(0);

        updateStatistics();

        // Слушатель для автоматического обновления статистики
        controller.getBookings().addListener((ListChangeListener<Booking>) change -> updateStatistics());

        tab.setContent(grid);
        return tab;
    }

    private VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("stat-card");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");
        valueLabel.setStyle("-fx-text-fill: " + color + ";");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-label");

        card.getChildren().addAll(valueLabel, titleLabel);
        return card;
    }

    private void updateStatistics() {
        totalBookingsLabel.setText(String.valueOf(controller.getTotalBookings()));
        confirmedBookingsLabel.setText(String.valueOf(controller.getConfirmedBookings()));
        pendingBookingsLabel.setText(String.valueOf(controller.getPendingBookings()));
    }

    private void showBookingForm(Booking booking) {
        try {
            Stage stage = new Stage();
            BookingFormView formView = new BookingFormView(controller, booking, stage);
            Scene scene = new Scene(formView, 500, 600);

            // Загружаем CSS
            try {
                scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            } catch (Exception e) {
                System.out.println("CSS не загружен для формы");
            }

            stage.setScene(scene);
            stage.setTitle(booking == null ? "Новое бронирование" : "Редактирование бронирования");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // Обновляем таблицу после закрытия формы
            bookingsTable.refresh();
            tablesTable.refresh();
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось открыть форму: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showBookingDetails() {
        Booking selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Детали бронирования");
            alert.setHeaderText("Бронирование №" + selected.getId());

            String content = String.format(
                    "Клиент: %s\nТелефон: %s\nКоличество гостей: %d\nДата и время: %s\nСтол: %s\nСтатус: %s\nОсобые пожелания: %s",
                    selected.getCustomerName(),
                    selected.getPhone(),
                    selected.getGuests(),
                    selected.getBookingDateTime().toString(),
                    selected.getTable().toString(),
                    selected.getStatus().toString(),
                    selected.getSpecialRequests()
            );

            alert.setContentText(content);
            alert.showAndWait();
        } else {
            showAlert("Внимание", "Пожалуйста, выберите бронирование для просмотра деталей");
        }
    }

    private void showTableBookings() {
        Table selectedTable = tablesTable.getSelectionModel().getSelectedItem();
        if (selectedTable != null) {
            StringBuilder bookingsInfo = new StringBuilder();
            bookingsInfo.append("Бронирования для стола №").append(selectedTable.getTableNumber()).append(":\n\n");

            controller.getBookings().stream()
                    .filter(booking -> booking.getTable().equals(selectedTable))
                    .filter(booking -> booking.getStatus() != BookingStatus.CANCELLED)
                    .forEach(booking -> {
                        bookingsInfo.append("Дата: ").append(booking.getBookingDateTime().toLocalDate())
                                .append(" Время: ").append(booking.getBookingDateTime().getHour()).append(":00")
                                .append(" - ").append(booking.getCustomerName())
                                .append(" (").append(booking.getStatus().toString()).append(")\n");
                    });

            if (bookingsInfo.toString().equals("Бронирования для стола №" + selectedTable.getTableNumber() + ":\n\n")) {
                bookingsInfo.append("Нет активных бронирований");
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Бронирования стола");
            alert.setHeaderText("Стол №" + selectedTable.getTableNumber());
            alert.setContentText(bookingsInfo.toString());
            alert.showAndWait();
        } else {
            showAlert("Внимание", "Пожалуйста, выберите стол для просмотра бронирований");
        }
    }

    private void changeBookingStatus(BookingStatus newStatus) {
        Booking selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            String statusName = newStatus.toString();
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Изменение статуса");
            confirmation.setHeaderText("Вы уверены, что хотите изменить статус бронирования на \"" + statusName + "\"?");
            confirmation.setContentText("Клиент: " + selected.getCustomerName() + "\nДата: " + selected.getBookingDateTime().toLocalDate());

            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                selected.setStatus(newStatus);
                bookingsTable.refresh();
                tablesTable.refresh();
                updateStatistics();

                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Статус изменен");
                info.setHeaderText(null);
                info.setContentText("Статус бронирования успешно изменен на \"" + statusName + "\"");
                info.showAndWait();
            }
        } else {
            showAlert("Внимание", "Пожалуйста, выберите бронирование для изменения статуса");
        }
    }

    private void deleteSelectedBooking() {
        Booking selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Подтверждение удаления");
            confirmation.setHeaderText("Вы уверены, что хотите удалить бронирование?");
            confirmation.setContentText("Клиент: " + selected.getCustomerName() + "\nДата: " + selected.getBookingDateTime().toLocalDate());

            if (confirmation.showAndWait().get() == ButtonType.OK) {
                controller.deleteBooking(selected);
                tablesTable.refresh();
            }
        } else {
            showAlert("Внимание", "Пожалуйста, выберите бронирование для удаления");
        }
    }

    private void applyFilter() {
        if (filterDatePicker.getValue() != null) {
            controller.applyFilter(filterDatePicker.getValue().atStartOfDay());
        }
    }

    private void clearFilter() {
        controller.clearFilter();
        filterDatePicker.setValue(null);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}