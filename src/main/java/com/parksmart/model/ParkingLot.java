package com.parksmart.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents the entire parking facility.
 * Maintains 30 parking slots (10 SMALL, 15 MEDIUM, 5 LARGE).
 * Implements intelligent slot allocation strategies based on vehicle size requirements.
 */
public class ParkingLot {
    public static final int TOTAL_SMALL_SLOTS = 10;
    public static final int TOTAL_MEDIUM_SLOTS = 15;
    public static final int TOTAL_LARGE_SLOTS = 5;
    public static final int TOTAL_SLOTS = TOTAL_SMALL_SLOTS + TOTAL_MEDIUM_SLOTS + TOTAL_LARGE_SLOTS;

    private final List<ParkingSlot> slots;

    public ParkingLot() {
        this.slots = new ArrayList<>();
        initializeDefaultSlots();
    }

    public ParkingLot(List<ParkingSlot> existingSlots) {
        this.slots = new ArrayList<>();
        if (existingSlots != null && !existingSlots.isEmpty()) {
            this.slots.addAll(existingSlots);
        } else {
            initializeDefaultSlots();
        }
    }

    /**
     * Initializes the standard 30 parking slots:
     * - Slots 1-10:   S-01 to S-10 (SMALL)
     * - Slots 11-25:  M-01 to M-15 (MEDIUM)
     * - Slots 26-30:  L-01 to L-05 (LARGE)
     */
    public final void initializeDefaultSlots() {
        slots.clear();
        int slotNumber = 1;

        // 10 SMALL slots
        for (int i = 1; i <= TOTAL_SMALL_SLOTS; i++) {
            String slotId = String.format("S-%02d", i);
            slots.add(new ParkingSlot(slotId, slotNumber++, SlotType.SMALL));
        }

        // 15 MEDIUM slots
        for (int i = 1; i <= TOTAL_MEDIUM_SLOTS; i++) {
            String slotId = String.format("M-%02d", i);
            slots.add(new ParkingSlot(slotId, slotNumber++, SlotType.MEDIUM));
        }

        // 5 LARGE slots
        for (int i = 1; i <= TOTAL_LARGE_SLOTS; i++) {
            String slotId = String.format("L-%02d", i);
            slots.add(new ParkingSlot(slotId, slotNumber++, SlotType.LARGE));
        }
    }

    /**
     * Finds the most suitable available parking slot for the given vehicle.
     * Rule: Always allocate the smallest suitable available slot first!
     * 
     * Priority:
     * - Bike: SMALL -> MEDIUM -> LARGE
     * - Car:  MEDIUM -> LARGE
     * - Truck: LARGE only
     * 
     * @param vehicle The vehicle requesting a parking spot.
     * @return Optional containing the assigned slot, or empty if lot is full for this vehicle.
     */
    public synchronized Optional<ParkingSlot> findSuitableSlot(Vehicle vehicle) {
        if (vehicle == null) {
            return Optional.empty();
        }

        SlotType minRequired = vehicle.getRequiredSlotType();

        if (minRequired == SlotType.SMALL) {
            // Bike: try SMALL first, then MEDIUM, then LARGE
            Optional<ParkingSlot> smallSlot = findFirstAvailable(SlotType.SMALL);
            if (smallSlot.isPresent()) return smallSlot;

            Optional<ParkingSlot> mediumSlot = findFirstAvailable(SlotType.MEDIUM);
            if (mediumSlot.isPresent()) return mediumSlot;

            return findFirstAvailable(SlotType.LARGE);

        } else if (minRequired == SlotType.MEDIUM) {
            // Car: try MEDIUM first, then LARGE
            Optional<ParkingSlot> mediumSlot = findFirstAvailable(SlotType.MEDIUM);
            if (mediumSlot.isPresent()) return mediumSlot;

            return findFirstAvailable(SlotType.LARGE);

        } else if (minRequired == SlotType.LARGE) {
            // Truck: LARGE only
            return findFirstAvailable(SlotType.LARGE);
        }

        return Optional.empty();
    }

    private Optional<ParkingSlot> findFirstAvailable(SlotType type) {
        return slots.stream()
                .filter(s -> s.getSlotType() == type && s.isAvailable())
                .findFirst();
    }

    public synchronized Optional<ParkingSlot> getSlotById(String slotId) {
        if (slotId == null) return Optional.empty();
        return slots.stream()
                .filter(s -> s.getSlotId().equalsIgnoreCase(slotId.trim()))
                .findFirst();
    }

    public synchronized Optional<ParkingSlot> getSlotByVehicleRegistration(String registrationNumber) {
        if (registrationNumber == null) return Optional.empty();
        String reg = registrationNumber.trim().toUpperCase();
        return slots.stream()
                .filter(s -> s.isOccupied() && reg.equalsIgnoreCase(s.getVehicleRegistration()))
                .findFirst();
    }

    public synchronized List<ParkingSlot> getAllSlots() {
        return Collections.unmodifiableList(new ArrayList<>(slots));
    }

    public synchronized int getTotalSlotsCount() {
        return slots.size();
    }

    public synchronized int getOccupiedSlotsCount() {
        return (int) slots.stream().filter(ParkingSlot::isOccupied).count();
    }

    public synchronized int getAvailableSlotsCount() {
        return (int) slots.stream().filter(ParkingSlot::isAvailable).count();
    }

    public synchronized double getOccupancyPercentage() {
        if (slots.isEmpty()) return 0.0;
        double percentage = ((double) getOccupiedSlotsCount() / slots.size()) * 100.0;
        return Math.round(percentage * 100.0) / 100.0;
    }

    public synchronized int getAvailableCountByType(SlotType type) {
        return (int) slots.stream()
                .filter(s -> s.getSlotType() == type && s.isAvailable())
                .count();
    }

    public synchronized int getOccupiedCountByType(SlotType type) {
        return (int) slots.stream()
                .filter(s -> s.getSlotType() == type && s.isOccupied())
                .count();
    }
}
