package model;

import javafx.beans.property.*;

public class Table {
    private final IntegerProperty tableNumber;
    private final IntegerProperty capacity;
    private final StringProperty location;
    private final BooleanProperty isAvailable;

    public Table(int tableNumber, int capacity, String location) {
        this.tableNumber = new SimpleIntegerProperty(tableNumber);
        this.capacity = new SimpleIntegerProperty(capacity);
        this.location = new SimpleStringProperty(location);
        this.isAvailable = new SimpleBooleanProperty(true);
    }

    public int getTableNumber() { return tableNumber.get(); }
    public IntegerProperty tableNumberProperty() { return tableNumber; }

    public int getCapacity() { return capacity.get(); }
    public IntegerProperty capacityProperty() { return capacity; }

    public String getLocation() { return location.get(); }
    public StringProperty locationProperty() { return location; }

    public boolean isIsAvailable() { return isAvailable.get(); }
    public BooleanProperty isAvailableProperty() { return isAvailable; }

    public void setIsAvailable(boolean isAvailable) { this.isAvailable.set(isAvailable); }

    @Override
    public String toString() {
        return "Стол №" + tableNumber.get() + " (" + capacity.get() + " персон) - " + location.get();
    }
}