package com.parksmart.model;

/**
 * Abstract base class representing a generic Vehicle.
 * Demonstrates OOP Abstraction and Encapsulation:
 * - Hides the common details of vehicles.
 * - Enforces concrete subclasses to define required slot type, category, and hourly rate.
 * - Private fields with public getters and setters ensure data encapsulation.
 */
public abstract class Vehicle {
    private String vehicleId;
    private String registrationNumber;
    private String ownerName;
    private String contactNumber;
    private String vehicleType;

    // Default constructor for serialization/deserialization
    public Vehicle() {
    }

    public Vehicle(String vehicleId, String registrationNumber, String ownerName, String contactNumber, String vehicleType) {
        this.vehicleId = vehicleId;
        this.registrationNumber = registrationNumber != null ? registrationNumber.trim().toUpperCase() : "";
        this.ownerName = ownerName != null ? ownerName.trim() : "";
        this.contactNumber = contactNumber != null ? contactNumber.trim() : "";
        this.vehicleType = vehicleType;
    }

    // Abstract methods demonstrating Polymorphism
    public abstract SlotType getRequiredSlotType();
    public abstract String getVehicleCategory();
    public abstract double getHourlyRate();

    // Getters and Setters (Encapsulation)
    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber != null ? registrationNumber.trim().toUpperCase() : "";
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName != null ? ownerName.trim() : "";
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber != null ? contactNumber.trim() : "";
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    @Override
    public String toString() {
        return getVehicleCategory() + " [Reg=" + registrationNumber + ", Owner=" + ownerName + "]";
    }
}
