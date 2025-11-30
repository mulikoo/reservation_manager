package view;

import controller.MainController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Booking;
import model.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class BookingFormView extends VBox {
    private MainController controller;
    private Booking editingBooking;
    private Stage stage;

    private TextField customerNameField;
    private TextField phoneField;
    private Spinner<Integer> guestsSpinner;
    private DatePicker datePicker;
    private Spinner<Integer> hourSpinner;
    private ComboBox<Table> tableComboBox;
    private TextArea specialRequestsArea;

    public BookingFormView(MainController controller, Booking editingBooking, Stage stage) {
        this.controller = controller;
        this.editingBooking = editingBooking;
        this.stage = stage;

        initializeUI();
        if (editingBooking != null) {
            populateForm();
        }
    }

    private void initializeUI() {
        setSpacing(15);
        setPadding(new Insets(20));
        setAlignment(Pos.TOP_CENTER);

        Label titleLabel = new Label(editingBooking == null ? "Новое бронирование" : "Редактирование бронирования");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        formGrid.setPadding(new Insets(10));

        // Поля формы
        customerNameField = new TextField();
        phoneField = new TextField();
        guestsSpinner = new Spinner<>(1, 20, 2);
        datePicker = new DatePicker();
        hourSpinner = new Spinner<>(8, 23, 18);
        tableComboBox = new ComboBox<>();
        specialRequestsArea = new TextArea();
        specialRequestsArea.setPrefRowCount(3);

        // Настройка ComboBox для столов
        tableComboBox.setCellFactory(param -> new ListCell<Table>() {
            @Override
            protected void updateItem(Table item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                }
            }
        });

        tableComboBox.setButtonCell(new ListCell<Table>() {
            @Override
            protected void updateItem(Table item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                }
            }
        });

        // Добавление полей в сетку
        formGrid.add(new Label("Имя клиента:"), 0, 0);
        formGrid.add(customerNameField, 1, 0);
        formGrid.add(new Label("Телефон:"), 0, 1);
        formGrid.add(phoneField, 1, 1);
        formGrid.add(new Label("Количество гостей:"), 0, 2);
        formGrid.add(guestsSpinner, 1, 2);
        formGrid.add(new Label("Дата:"), 0, 3);
        formGrid.add(datePicker, 1, 3);
        formGrid.add(new Label("Время (час):"), 0, 4);
        formGrid.add(hourSpinner, 1, 4);
        formGrid.add(new Label("Стол:"), 0, 5);
        formGrid.add(tableComboBox, 1, 5);
        formGrid.add(new Label("Особые пожелания:"), 0, 6);
        formGrid.add(specialRequestsArea, 1, 6);

        // Кнопки
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button saveButton = new Button("Сохранить");
        saveButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveButton.setOnAction(e -> saveBooking());

        Button cancelButton = new Button("Отмена");
        cancelButton.setOnAction(e -> stage.close());

        buttonBox.getChildren().addAll(saveButton, cancelButton);

        // Слушатели для обновления доступных столов
        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> updateAvailableTables());
        hourSpinner.valueProperty().addListener((obs, oldHour, newHour) -> updateAvailableTables());
        guestsSpinner.valueProperty().addListener((obs, oldGuests, newGuests) -> updateAvailableTables());

        getChildren().addAll(titleLabel, formGrid, buttonBox);
    }

    private void populateForm() {
        customerNameField.setText(editingBooking.getCustomerName());
        phoneField.setText(editingBooking.getPhone());
        guestsSpinner.getValueFactory().setValue(editingBooking.getGuests());
        datePicker.setValue(editingBooking.getBookingDateTime().toLocalDate());
        hourSpinner.getValueFactory().setValue(editingBooking.getBookingDateTime().getHour());
        tableComboBox.setValue(editingBooking.getTable());
        specialRequestsArea.setText(editingBooking.getSpecialRequests());
    }

    private void updateAvailableTables() {
        if (datePicker.getValue() != null) {
            LocalDateTime selectedDateTime = LocalDateTime.of(
                    datePicker.getValue(),
                    LocalTime.of(hourSpinner.getValue(), 0)
            );
            tableComboBox.setItems(controller.getAvailableTables(
                    guestsSpinner.getValue(), selectedDateTime
            ));
        }
    }

    private void saveBooking() {
        if (!validateInput()) {
            return;
        }

        try {
            LocalDateTime bookingDateTime = LocalDateTime.of(
                    datePicker.getValue(),
                    LocalTime.of(hourSpinner.getValue(), 0)
            );

            Booking booking = new Booking(
                    editingBooking != null ? editingBooking.getId() : controller.getNextBookingId(),
                    customerNameField.getText().trim(),
                    phoneField.getText().trim(),
                    guestsSpinner.getValue(),
                    bookingDateTime,
                    tableComboBox.getValue(),
                    specialRequestsArea.getText().trim()
            );

            // Если редактируем, сохраняем старый статус
            if (editingBooking != null) {
                booking.setStatus(editingBooking.getStatus());
                controller.updateBooking(editingBooking, booking);
            } else {
                controller.addBooking(booking);
            }

            stage.close();
        } catch (Exception e) {
            showAlert("Ошибка", "Произошла ошибка при сохранении: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        if (customerNameField.getText().trim().isEmpty()) {
            errors.append("• Пожалуйста, введите имя клиента\n");
        }

        if (phoneField.getText().trim().isEmpty()) {
            errors.append("• Пожалуйста, введите номер телефона\n");
        }

        if (datePicker.getValue() == null) {
            errors.append("• Пожалуйста, выберите дату\n");
        } else if (datePicker.getValue().isBefore(LocalDate.now())) {
            errors.append("• Нельзя выбрать прошедшую дату\n");
        }

        if (tableComboBox.getValue() == null) {
            errors.append("• Пожалуйста, выберите стол\n");
        }

        if (errors.length() > 0) {
            showAlert("Ошибка валидации", errors.toString());
            return false;
        }

        return true;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}