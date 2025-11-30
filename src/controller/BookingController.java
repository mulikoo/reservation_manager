package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Booking;
import model.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class BookingController {
    @FXML private TextField customerNameField;
    @FXML private TextField phoneField;
    @FXML private Spinner<Integer> guestsSpinner;
    @FXML private DatePicker datePicker;
    @FXML private Spinner<Integer> hourSpinner;
    @FXML private ComboBox<Table> tableComboBox;
    @FXML private TextArea specialRequestsArea;
    @FXML private Label titleLabel;

    private MainController mainController;
    private Booking editingBooking;
    private Stage stage;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setEditingBooking(Booking booking) {
        this.editingBooking = booking;
        if (booking != null) {
            titleLabel.setText("Редактирование бронирования");
            customerNameField.setText(booking.getCustomerName());
            phoneField.setText(booking.getPhone());
            guestsSpinner.getValueFactory().setValue(booking.getGuests());
            datePicker.setValue(booking.getBookingDateTime().toLocalDate());
            hourSpinner.getValueFactory().setValue(booking.getBookingDateTime().getHour());
            tableComboBox.setValue(booking.getTable());
            specialRequestsArea.setText(booking.getSpecialRequests());
        }
    }

    @FXML
    private void initialize() {
        // Настройка спиннеров
        guestsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 2));
        hourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 23, 18));

        // Слушатель для обновления доступных столов
        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> updateAvailableTables());
        hourSpinner.valueProperty().addListener((obs, oldHour, newHour) -> updateAvailableTables());
        guestsSpinner.valueProperty().addListener((obs, oldGuests, newGuests) -> updateAvailableTables());
    }

    private void updateAvailableTables() {
        if (datePicker.getValue() != null) {
            LocalDateTime selectedDateTime = LocalDateTime.of(
                    datePicker.getValue(),
                    LocalTime.of(hourSpinner.getValue(), 0)
            );
            tableComboBox.setItems(mainController.getAvailableTables(
                    guestsSpinner.getValue(), selectedDateTime
            ));
        }
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        try {
            LocalDateTime bookingDateTime = LocalDateTime.of(
                    datePicker.getValue(),
                    LocalTime.of(hourSpinner.getValue(), 0)
            );

            Booking booking = new Booking(
                    editingBooking != null ? editingBooking.getId() : mainController.getNextBookingId(),
                    customerNameField.getText(),
                    phoneField.getText(),
                    guestsSpinner.getValue(),
                    bookingDateTime,
                    tableComboBox.getValue(),
                    specialRequestsArea.getText()
            );

            if (editingBooking != null) {
                mainController.updateBooking(editingBooking, booking);
            } else {
                mainController.addBooking(booking);
            }

            stage.close();
        } catch (Exception e) {
            showAlert("Ошибка", "Произошла ошибка при сохранении: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        stage.close();
    }

    private boolean validateInput() {
        if (customerNameField.getText().trim().isEmpty()) {
            showAlert("Ошибка валидации", "Пожалуйста, введите имя клиента");
            return false;
        }
        if (phoneField.getText().trim().isEmpty()) {
            showAlert("Ошибка", "Пожалуйста, введите номер телефона");
            return false;
        }
        if (datePicker.getValue() == null) {
            showAlert("Ошибка", "Пожалуйста, выберите дату");
            return false;
        }
        if (tableComboBox.getValue() == null) {
            showAlert("Ошибка", "Пожалуйста, выберите стол");
            return false;
        }
        if (datePicker.getValue().isBefore(LocalDate.now())) {
            showAlert("Ошибка", "Нельзя выбрать прошедшую дату");
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