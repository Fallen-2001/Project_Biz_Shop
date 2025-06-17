package org.example.model;

public enum OrderStatus {
    PENDING("Oczekujące"),
    CONFIRMED("Potwierdzone"),
    SHIPPED("Wysłane"),
    DELIVERED("Dostarczone"),
    CANCELLED("Anulowane");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}