package com.parksmart.model;

/**
 * Concrete class representing a standard four-wheeled passenger vehicle (Car / Sedan / SUV).
 * Demonstrates OOP Inheritance and Polymorphic method overrides.
 * 
 * Rules:
 * - Minimum required slot size: MEDIUM
 * - Can be allocated into: MEDIUM (preferred) or LARGE
 * - Standard billing rate: Rs. 40.0 per hour
 */
public class Car extends Vehicle {

    public Car() {
        super();
        setVehicleType("CAR");
    }

    public Car(String vehicleId, String registrationNumber, String ownerName, String contactNumber) {
        super(vehicleId, registrationNumber, ownerName, contactNumber, "CAR");
    }

    @Override
    public SlotType getRequiredSlotType() {
        return SlotType.MEDIUM;
    }

    @Override
    public String getVehicleCategory() {
        return "CAR";
    }

    @Override
    public double getHourlyRate() {
        return 40.0;
    }
}
