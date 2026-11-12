package com.parksmart.model;

/**
 * Represents the size category of a parking slot.
 * Different vehicle types require different minimum slot sizes:
 * - Bike: Can fit in SMALL, MEDIUM, or LARGE (prefers SMALL)
 * - Car: Can fit in MEDIUM or LARGE (prefers MEDIUM)
 * - Truck: Can fit ONLY in LARGE
 */
public enum SlotType {
    SMALL("Small Slot", "Suitable for Bikes"),
    MEDIUM("Medium Slot", "Suitable for Cars and Bikes"),
    LARGE("Large Slot", "Suitable for Trucks, Cars, and Bikes");

    private final String displayName;
    private final String description;

    SlotType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
