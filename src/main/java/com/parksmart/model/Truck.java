package com.parksmart.model;

/**
 * Concrete class representing a heavy commercial vehicle (Truck / Bus / Van).
 * Demonstrates OOP Inheritance and Polymorphic method overrides.
 * 
 * Rules:
 * - Minimum required slot size: LARGE
 * - Can be allocated into: LARGE ONLY
 * - Standard billing rate: Rs. 70.0 per hour
 */
public class Truck extends Vehicle {

    public Truck() {
        super();
        setVehicleType("TRUCK");
    }

    public Truck(String vehicleId, String registrationNumber, String ownerName, String contactNumber) {
        super(vehicleId, registrationNumber, ownerName, contactNumber, "TRUCK");
    }

    @Override
    public SlotType getRequiredSlotType() {
        return SlotType.LARGE;
    }

    @Override
    public String getVehicleCategory() {
        return "TRUCK";
    }

    @Override
    public double getHourlyRate() {
        return 70.0;
    }
}
