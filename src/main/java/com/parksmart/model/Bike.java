package com.parksmart.model;

/**
 * Concrete class representing a two-wheeled vehicle (Bike / Motorcycle / Scooter).
 * Demonstrates OOP Inheritance and Polymorphic method overrides.
 * 
 * Rules:
 * - Minimum required slot size: SMALL
 * - Can be allocated into: SMALL (preferred), MEDIUM, or LARGE
 * - Standard billing rate: Rs. 20.0 per hour
 */
public class Bike extends Vehicle {

    public Bike() {
        super();
        setVehicleType("BIKE");
    }

    public Bike(String vehicleId, String registrationNumber, String ownerName, String contactNumber) {
        super(vehicleId, registrationNumber, ownerName, contactNumber, "BIKE");
    }

    @Override
    public SlotType getRequiredSlotType() {
        return SlotType.SMALL;
    }

    @Override
    public String getVehicleCategory() {
        return "BIKE";
    }

    @Override
    public double getHourlyRate() {
        return 20.0;
    }
}
