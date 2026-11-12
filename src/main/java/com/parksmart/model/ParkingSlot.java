package com.parksmart.model;

/**
 * Represents an individual physical parking space in the facility.
 * Maintains occupancy state, slot category, and the currently parked vehicle.
 */
public class ParkingSlot {
    private String slotId;            // e.g., "S-01", "M-05", "L-02"
    private int slotNumber;           // Numeric identifier: 1 to 30
    private SlotType slotType;        // SMALL, MEDIUM, LARGE
    private boolean occupied;         // true if a vehicle is parked
    private String vehicleRegistration; // Null when available
    private String vehicleType;       // BIKE, CAR, TRUCK (null when empty)
    private String occupiedAt;        // Timestamp when vehicle was parked

    public ParkingSlot() {
    }

    public ParkingSlot(String slotId, int slotNumber, SlotType slotType) {
        this.slotId = slotId;
        this.slotNumber = slotNumber;
        this.slotType = slotType;
        this.occupied = false;
        this.vehicleRegistration = null;
        this.vehicleType = null;
        this.occupiedAt = null;
    }

    /**
     * Checks if this slot is currently free.
     */
    public boolean isAvailable() {
        return !occupied;
    }

    /**
     * Determines whether a given vehicle can physically fit into this slot.
     * Hierarchy rule:
     * - SMALL slot: fits Bike only
     * - MEDIUM slot: fits Bike and Car
     * - LARGE slot: fits Bike, Car, and Truck
     */
    public boolean canFit(Vehicle vehicle) {
        if (vehicle == null) {
            return false;
        }
        SlotType req = vehicle.getRequiredSlotType();
        switch (req) {
            case SMALL:
                // Bike can fit anywhere
                return true;
            case MEDIUM:
                // Car needs at least MEDIUM
                return this.slotType == SlotType.MEDIUM || this.slotType == SlotType.LARGE;
            case LARGE:
                // Truck strictly requires LARGE
                return this.slotType == SlotType.LARGE;
            default:
                return false;
        }
    }

    /**
     * Assigns a vehicle to this slot and marks it as occupied.
     */
    public void assignVehicle(String registration, String type, String timestamp) {
        this.occupied = true;
        this.vehicleRegistration = registration != null ? registration.trim().toUpperCase() : null;
        this.vehicleType = type;
        this.occupiedAt = timestamp;
    }

    /**
     * Releases this slot and clears vehicle information.
     */
    public void vacate() {
        this.occupied = false;
        this.vehicleRegistration = null;
        this.vehicleType = null;
        this.occupiedAt = null;
    }

    // Getters and Setters (Encapsulation)
    public String getSlotId() {
        return slotId;
    }

    public void setSlotId(String slotId) {
        this.slotId = slotId;
    }

    public int getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(int slotNumber) {
        this.slotNumber = slotNumber;
    }

    public SlotType getSlotType() {
        return slotType;
    }

    public void setSlotType(SlotType slotType) {
        this.slotType = slotType;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public String getVehicleRegistration() {
        return vehicleRegistration;
    }

    public void setVehicleRegistration(String vehicleRegistration) {
        this.vehicleRegistration = vehicleRegistration;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getOccupiedAt() {
        return occupiedAt;
    }

    public void setOccupiedAt(String occupiedAt) {
        this.occupiedAt = occupiedAt;
    }

    @Override
    public String toString() {
        return "Slot " + slotId + " (" + slotType + ") - " + (occupied ? "OCCUPIED by " + vehicleRegistration : "AVAILABLE");
    }
}
