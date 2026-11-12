package com.parksmart.service;

import com.parksmart.model.Vehicle;

/**
 * Service class responsible for computing parking fees.
 * 
 * ========================================================
 * BILLING FORMULA & BUSINESS RULES:
 * ========================================================
 * 1. Base Rates:
 *    - Bike:  Rs. 20.00 per hour
 *    - Car:   Rs. 40.00 per hour
 *    - Truck: Rs. 70.00 per hour
 * 
 * 2. Minimum Charge:
 *    - A minimum charge of 1 full hour applies to all parking sessions.
 * 
 * 3. Partial Hour Rounding:
 *    - Partial hours are rounded UP to the next full hour ceiling:
 *      billedHours = Math.max(1, (long) Math.ceil(durationMinutes / 60.0))
 * 
 * 4. Total Fee Calculation:
 *      totalFee = billedHours * ratePerHour
 * 
 * Examples:
 * - Bike parked for 12 minutes: billedHours = 1 -> Fee = 1 * 20 = Rs. 20.00
 * - Car parked for 65 minutes:  billedHours = 2 -> Fee = 2 * 40 = Rs. 80.00
 * - Truck parked for 190 minutes: billedHours = 4 -> Fee = 4 * 70 = Rs. 280.00
 */
public class ParkingFeeCalculator {
    public static final double DEFAULT_BIKE_RATE = 20.0;
    public static final double DEFAULT_CAR_RATE = 40.0;
    public static final double DEFAULT_TRUCK_RATE = 70.0;

    private double bikeRate;
    private double carRate;
    private double truckRate;

    public ParkingFeeCalculator() {
        this.bikeRate = DEFAULT_BIKE_RATE;
        this.carRate = DEFAULT_CAR_RATE;
        this.truckRate = DEFAULT_TRUCK_RATE;
    }

    public ParkingFeeCalculator(double bikeRate, double carRate, double truckRate) {
        this.bikeRate = bikeRate;
        this.carRate = carRate;
        this.truckRate = truckRate;
    }

    /**
     * Resolves the hourly billing rate for a vehicle category.
     */
    public double getRateForCategory(String vehicleType) {
        if (vehicleType == null) return carRate;
        switch (vehicleType.trim().toUpperCase()) {
            case "BIKE":
                return bikeRate;
            case "TRUCK":
                return truckRate;
            case "CAR":
            default:
                return carRate;
        }
    }

    /**
     * Calculates the billed hours from duration in minutes.
     * Always minimum 1 hour, and rounds up any partial hour.
     */
    public long calculateBilledHours(long durationMinutes) {
        if (durationMinutes <= 0) {
            return 1;
        }
        return Math.max(1, (long) Math.ceil((double) durationMinutes / 60.0));
    }

    /**
     * Computes the total parking fee.
     * Formula: billedHours * rate
     */
    public double calculateFee(String vehicleType, long durationMinutes) {
        double rate = getRateForCategory(vehicleType);
        long billedHours = calculateBilledHours(durationMinutes);
        return billedHours * rate;
    }

    /**
     * Polymorphic overload using the Vehicle model instance.
     */
    public double calculateFee(Vehicle vehicle, long durationMinutes) {
        if (vehicle == null) {
            return calculateFee("CAR", durationMinutes);
        }
        double rate = vehicle.getHourlyRate();
        long billedHours = calculateBilledHours(durationMinutes);
        return billedHours * rate;
    }

    // Getters and Setters for rates
    public double getBikeRate() {
        return bikeRate;
    }

    public void setBikeRate(double bikeRate) {
        this.bikeRate = bikeRate;
    }

    public double getCarRate() {
        return carRate;
    }

    public void setCarRate(double carRate) {
        this.carRate = carRate;
    }

    public double getTruckRate() {
        return truckRate;
    }

    public void setTruckRate(double truckRate) {
        this.truckRate = truckRate;
    }
}
