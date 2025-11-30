package model;

public enum BookingStatus {
    CONFIRMED("Подтверждено"),
    PENDING("Ожидание"),
    CANCELLED("Отменено"),
    COMPLETED("Завершено");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}