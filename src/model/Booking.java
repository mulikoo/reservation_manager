package model;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class Booking {
    private final IntegerProperty id;
    private final StringProperty customerName;
    private final StringProperty phone;
    private final IntegerProperty guests;
    private final ObjectProperty<LocalDateTime> bookingDateTime;
    private final ObjectProperty<Table> table;
    private final ObjectProperty<BookingStatus> status;
    private final StringProperty specialRequests;

    public Booking(int id, String customerName, String phone, int guests,
                   LocalDateTime bookingDateTime, Table table, String specialRequests) {
        this.id = new SimpleIntegerProperty(id);
        this.customerName = new SimpleStringProperty(customerName);
        this.phone = new SimpleStringProperty(phone);
        this.guests = new SimpleIntegerProperty(guests);
        this.bookingDateTime = new SimpleObjectProperty<>(bookingDateTime);
        this.table = new SimpleObjectProperty<>(table);
        this.status = new SimpleObjectProperty<>(BookingStatus.PENDING);
        this.specialRequests = new SimpleStringProperty(specialRequests);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public String getCustomerName() { return customerName.get(); }
    public StringProperty customerNameProperty() { return customerName; }

    public String getPhone() { return phone.get(); }
    public StringProperty phoneProperty() { return phone; }

    public int getGuests() { return guests.get(); }
    public IntegerProperty guestsProperty() { return guests; }

    public LocalDateTime getBookingDateTime() { return bookingDateTime.get(); }
    public ObjectProperty<LocalDateTime> bookingDateTimeProperty() { return bookingDateTime; }

    public Table getTable() { return table.get(); }
    public ObjectProperty<Table> tableProperty() { return table; }

    public BookingStatus getStatus() { return status.get(); }
    public ObjectProperty<BookingStatus> statusProperty() { return status; }

    public String getSpecialRequests() { return specialRequests.get(); }
    public StringProperty specialRequestsProperty() { return specialRequests; }

    public void setStatus(BookingStatus status) { this.status.set(status); }

    @Override
    public String toString() {
        return customerName.get() + " - " + bookingDateTime.get().toLocalDate() + " " +
                bookingDateTime.get().toLocalTime() + " (" + table.get().getTableNumber() + ")";
    }
}