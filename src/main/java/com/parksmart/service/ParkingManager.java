package com.parksmart.service;

import com.parksmart.model.Bike;
import com.parksmart.model.Car;
import com.parksmart.model.ParkingLot;
import com.parksmart.model.ParkingSlot;
import com.parksmart.model.ParkingTicket;
import com.parksmart.model.SlotType;
import com.parksmart.model.TicketStatus;
import com.parksmart.model.Truck;
import com.parksmart.model.Vehicle;
import com.parksmart.storage.DataStorage;
import com.parksmart.util.DateTimeUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Core business logic controller for ParkSmart.
 * Coordinates slot allocation, ticket lifecycle, persistence, and reporting.
 * Thread-safe to guarantee race-free slot reservations.
 */
public class ParkingManager {
    private final ParkingLot parkingLot;
    private final DataStorage dataStorage;
    private final ParkingFeeCalculator feeCalculator;

    private final List<Vehicle> registeredVehicles;
    private final List<ParkingTicket> tickets;
    private final AtomicLong ticketSequence;

    public ParkingManager(DataStorage dataStorage) {
        this.dataStorage = dataStorage;
        this.feeCalculator = new ParkingFeeCalculator();
        this.registeredVehicles = new ArrayList<>();
        this.tickets = new ArrayList<>();

        // Load persisted data if available
        List<ParkingSlot> loadedSlots = dataStorage.loadSlots();
        if (loadedSlots.isEmpty()) {
            this.parkingLot = new ParkingLot();
            this.dataStorage.saveSlots(parkingLot.getAllSlots());
        } else {
            this.parkingLot = new ParkingLot(loadedSlots);
        }

        List<Vehicle> loadedVehicles = dataStorage.loadVehicles();
        this.registeredVehicles.addAll(loadedVehicles);

        List<ParkingTicket> loadedTickets = dataStorage.loadTickets();
        this.tickets.addAll(loadedTickets);

        // Synchronize slot occupancy state with active tickets
        syncSlotsWithActiveTickets();

        // Initialize ticket sequence based on existing tickets
        long maxSeq = tickets.stream()
                .mapToLong(t -> {
                    try {
                        String id = t.getTicketId();
                        if (id != null && id.contains("-")) {
                            String num = id.substring(id.lastIndexOf("-") + 1);
                            return Long.parseLong(num);
                        }
                    } catch (Exception ignored) {}
                    return 0L;
                }).max().orElse(1000L);
        this.ticketSequence = new AtomicLong(Math.max(1000L, maxSeq));

        // Save synchronized state
        persistAll();
    }

    /**
     * Ensures slots correctly mirror the active tickets in case of system recovery.
     */
    private void syncSlotsWithActiveTickets() {
        Map<String, ParkingTicket> activeMap = tickets.stream()
                .filter(t -> t.getTicketStatus() == TicketStatus.ACTIVE)
                .collect(Collectors.toMap(
                        ParkingTicket::getSlotNumber,
                        t -> t,
                        (existing, replacement) -> existing
                ));

        for (ParkingSlot slot : parkingLot.getAllSlots()) {
            ParkingTicket activeTicket = activeMap.get(slot.getSlotId());
            if (activeTicket != null) {
                slot.setOccupied(true);
                slot.setVehicleRegistration(activeTicket.getVehicleRegistration());
                slot.setVehicleType(activeTicket.getVehicleType());
                slot.setOccupiedAt(activeTicket.getEntryTime());
            } else if (slot.isOccupied() && (slot.getVehicleRegistration() == null || slot.getVehicleRegistration().isEmpty())) {
                slot.vacate();
            }
        }
    }

    /**
     * Registers a new incoming vehicle, assigns the smallest suitable available slot,
     * and generates an active parking ticket.
     * 
     * @param registrationNumber Plate number (e.g. "DL-01-AB-1234")
     * @param ownerName          Name of vehicle owner
     * @param vehicleTypeStr     "BIKE", "CAR", or "TRUCK"
     * @param contactNumber      Optional phone number
     * @return The newly issued active ParkingTicket
     */
    public synchronized ParkingTicket registerAndParkVehicle(String registrationNumber,
                                                             String ownerName,
                                                             String vehicleTypeStr,
                                                             String contactNumber) {
        // 1. Validation
        if (registrationNumber == null || registrationNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Vehicle registration number is required.");
        }
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner name is required.");
        }
        if (vehicleTypeStr == null || vehicleTypeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Vehicle type is required (BIKE, CAR, or TRUCK).");
        }

        String reg = registrationNumber.trim().toUpperCase();
        String owner = ownerName.trim();
        String contact = contactNumber != null ? contactNumber.trim() : "";
        String type = vehicleTypeStr.trim().toUpperCase();

        if (!type.equals("BIKE") && !type.equals("CAR") && !type.equals("TRUCK")) {
            throw new IllegalArgumentException("Invalid vehicle type: " + type + ". Must be BIKE, CAR, or TRUCK.");
        }

        // 2. Prevent duplicate active parking
        boolean alreadyParked = tickets.stream().anyMatch(t -> 
                t.getTicketStatus() == TicketStatus.ACTIVE &&
                reg.equalsIgnoreCase(t.getVehicleRegistration())
        );
        if (alreadyParked) {
            Optional<ParkingSlot> currentSlot = parkingLot.getSlotByVehicleRegistration(reg);
            String slotInfo = currentSlot.map(ParkingSlot::getSlotId).orElse("an active slot");
            throw new IllegalStateException("Vehicle '" + reg + "' is already actively parked in slot " + slotInfo + ".");
        }

        // 3. Create polymorphic Vehicle instance
        String vehicleId = "VEH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Vehicle vehicle;
        switch (type) {
            case "BIKE":
                vehicle = new Bike(vehicleId, reg, owner, contact);
                break;
            case "TRUCK":
                vehicle = new Truck(vehicleId, reg, owner, contact);
                break;
            case "CAR":
            default:
                vehicle = new Car(vehicleId, reg, owner, contact);
                break;
        }

        // 4. Find suitable available parking slot (smallest suitable first)
        Optional<ParkingSlot> suitableSlot = parkingLot.findSuitableSlot(vehicle);
        if (suitableSlot.isEmpty()) {
            throw new IllegalStateException("No suitable parking slot is currently available for " + type + ".");
        }

        ParkingSlot slot = suitableSlot.get();
        String entryTime = DateTimeUtils.now();

        // 5. Occupy slot
        slot.assignVehicle(reg, type, entryTime);

        // 6. Generate ticket
        String ticketId = "TKT-" + (ticketSequence.incrementAndGet());
        double rate = vehicle.getHourlyRate();

        ParkingTicket ticket = new ParkingTicket(
                ticketId,
                reg,
                type,
                owner,
                contact,
                slot.getSlotId(),
                slot.getSlotType(),
                entryTime,
                rate
        );

        // 7. Store and persist
        registeredVehicles.removeIf(v -> reg.equalsIgnoreCase(v.getRegistrationNumber()));
        registeredVehicles.add(vehicle);
        tickets.add(ticket);

        persistAll();

        return ticket;
    }

    /**
     * Processes vehicle departure, computes elapsed time and billing fee,
     * marks ticket as COMPLETED, and vacates the parking slot.
     * 
     * @param searchIdentifier Ticket ID or Vehicle Registration Number
     * @return The completed ParkingTicket acting as a digital receipt
     */
    public synchronized ParkingTicket processVehicleExit(String searchIdentifier) {
        if (searchIdentifier == null || searchIdentifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticket ID or Vehicle Registration is required.");
        }

        String search = searchIdentifier.trim();

        // Find active ticket
        Optional<ParkingTicket> ticketOpt = tickets.stream()
                .filter(t -> t.getTicketStatus() == TicketStatus.ACTIVE &&
                        (search.equalsIgnoreCase(t.getTicketId()) ||
                         search.equalsIgnoreCase(t.getVehicleRegistration())))
                .findFirst();

        if (ticketOpt.isEmpty()) {
            throw new IllegalArgumentException("No active parking session found matching '" + search + "'.");
        }

        ParkingTicket ticket = ticketOpt.get();
        String exitTime = DateTimeUtils.now();
        long durationMinutes = DateTimeUtils.calculateDurationMinutes(ticket.getEntryTime(), exitTime);
        long billedHours = feeCalculator.calculateBilledHours(durationMinutes);
        double fee = feeCalculator.calculateFee(ticket.getVehicleType(), durationMinutes);
        String formattedDuration = DateTimeUtils.formatDuration(durationMinutes);

        // Update ticket
        ticket.setExitTime(exitTime);
        ticket.setDurationMinutes(durationMinutes);
        ticket.setParkingDuration(formattedDuration);
        ticket.setBilledHours(billedHours);
        ticket.setParkingFee(fee);
        ticket.setTicketStatus(TicketStatus.COMPLETED);

        // Vacate parking slot
        Optional<ParkingSlot> slotOpt = parkingLot.getSlotById(ticket.getSlotNumber());
        slotOpt.ifPresent(ParkingSlot::vacate);

        // Also check if slot was assigned to this reg number
        Optional<ParkingSlot> slotByReg = parkingLot.getSlotByVehicleRegistration(ticket.getVehicleRegistration());
        slotByReg.ifPresent(ParkingSlot::vacate);

        persistAll();

        return ticket;
    }

    /**
     * Searches active vehicle or historical ticket.
     */
    public synchronized Optional<ParkingTicket> findActiveTicketByVehicleOrId(String identifier) {
        if (identifier == null) return Optional.empty();
        String id = identifier.trim();
        return tickets.stream()
                .filter(t -> t.getTicketStatus() == TicketStatus.ACTIVE &&
                        (id.equalsIgnoreCase(t.getTicketId()) || id.equalsIgnoreCase(t.getVehicleRegistration())))
                .findFirst();
    }

    public synchronized List<ParkingTicket> searchTickets(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getParkingHistory();
        }
        String q = query.trim().toLowerCase();
        return tickets.stream()
                .filter(t -> (t.getTicketId() != null && t.getTicketId().toLowerCase().contains(q)) ||
                             (t.getVehicleRegistration() != null && t.getVehicleRegistration().toLowerCase().contains(q)) ||
                             (t.getOwnerName() != null && t.getOwnerName().toLowerCase().contains(q)) ||
                             (t.getSlotNumber() != null && t.getSlotNumber().toLowerCase().contains(q)) ||
                             (t.getVehicleType() != null && t.getVehicleType().toLowerCase().contains(q)))
                .collect(Collectors.toList());
    }

    public synchronized List<ParkingTicket> getActiveParkingSessions() {
        return tickets.stream()
                .filter(t -> t.getTicketStatus() == TicketStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    public synchronized List<ParkingTicket> getParkingHistory() {
        List<ParkingTicket> copy = new ArrayList<>(tickets);
        Collections.reverse(copy); // Latest first
        return copy;
    }

    public synchronized List<Vehicle> getRegisteredVehicles() {
        return Collections.unmodifiableList(new ArrayList<>(registeredVehicles));
    }

    public synchronized ParkingLot getParkingLot() {
        return parkingLot;
    }

    public synchronized ParkingFeeCalculator getFeeCalculator() {
        return feeCalculator;
    }

    /**
     * Gathers all dynamic dashboard statistics.
     */
    public synchronized Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        int totalSlots = parkingLot.getTotalSlotsCount();
        int occupiedSlots = parkingLot.getOccupiedSlotsCount();
        int availableSlots = parkingLot.getAvailableSlotsCount();
        double occupancyPercentage = parkingLot.getOccupancyPercentage();

        List<ParkingTicket> activeTickets = getActiveParkingSessions();
        int activeCount = activeTickets.size();

        String todayDate = LocalDate.now().toString();
        double todayRevenue = tickets.stream()
                .filter(t -> t.getTicketStatus() == TicketStatus.COMPLETED &&
                             t.getExitTime() != null &&
                             t.getExitTime().startsWith(todayDate))
                .mapToDouble(ParkingTicket::getParkingFee)
                .sum();

        double totalRevenue = tickets.stream()
                .filter(t -> t.getTicketStatus() == TicketStatus.COMPLETED)
                .mapToDouble(ParkingTicket::getParkingFee)
                .sum();

        long completedSessions = tickets.stream()
                .filter(t -> t.getTicketStatus() == TicketStatus.COMPLETED)
                .count();

        // Slots breakdown
        Map<String, Object> slotsByType = new LinkedHashMap<>();
        for (SlotType type : SlotType.values()) {
            Map<String, Object> typeMap = new HashMap<>();
            int total = (int) parkingLot.getAllSlots().stream().filter(s -> s.getSlotType() == type).count();
            int occupied = parkingLot.getOccupiedCountByType(type);
            int available = parkingLot.getAvailableCountByType(type);
            typeMap.put("total", total);
            typeMap.put("occupied", occupied);
            typeMap.put("available", available);
            slotsByType.put(type.name(), typeMap);
        }

        // Vehicle category stats
        Map<String, Object> categoryStats = new LinkedHashMap<>();
        String[] categories = {"BIKE", "CAR", "TRUCK"};
        for (String cat : categories) {
            Map<String, Object> catMap = new HashMap<>();
            long active = activeTickets.stream().filter(t -> cat.equalsIgnoreCase(t.getVehicleType())).count();
            long completed = tickets.stream().filter(t -> t.getTicketStatus() == TicketStatus.COMPLETED && cat.equalsIgnoreCase(t.getVehicleType())).count();
            double rev = tickets.stream().filter(t -> t.getTicketStatus() == TicketStatus.COMPLETED && cat.equalsIgnoreCase(t.getVehicleType())).mapToDouble(ParkingTicket::getParkingFee).sum();
            catMap.put("active", active);
            catMap.put("completed", completed);
            catMap.put("revenue", rev);
            catMap.put("hourlyRate", feeCalculator.getRateForCategory(cat));
            categoryStats.put(cat, catMap);
        }

        // Recent 5 entries (from all tickets)
        List<ParkingTicket> recentEntries = new ArrayList<>(tickets);
        Collections.reverse(recentEntries);
        if (recentEntries.size() > 5) {
            recentEntries = recentEntries.subList(0, 5);
        }

        // Recent 5 exits
        List<ParkingTicket> recentExits = tickets.stream()
                .filter(t -> t.getTicketStatus() == TicketStatus.COMPLETED)
                .collect(Collectors.toList());
        Collections.reverse(recentExits);
        if (recentExits.size() > 5) {
            recentExits = recentExits.subList(0, 5);
        }

        stats.put("totalSlots", totalSlots);
        stats.put("occupiedSlots", occupiedSlots);
        stats.put("availableSlots", availableSlots);
        stats.put("occupancyPercentage", occupancyPercentage);
        stats.put("activeVehicles", activeCount);
        stats.put("completedSessions", completedSessions);
        stats.put("todayRevenue", todayRevenue);
        stats.put("totalRevenue", totalRevenue);
        stats.put("slotsByType", slotsByType);
        stats.put("categoryStats", categoryStats);
        stats.put("recentEntries", recentEntries);
        stats.put("recentExits", recentExits);

        return stats;
    }

    /**
     * Compiles detailed revenue reports.
     */
    public synchronized Map<String, Object> getRevenueReport() {
        Map<String, Object> report = new LinkedHashMap<>();

        String todayDate = LocalDate.now().toString();
        List<ParkingTicket> completed = tickets.stream()
                .filter(t -> t.getTicketStatus() == TicketStatus.COMPLETED)
                .collect(Collectors.toList());

        double todayRevenue = completed.stream()
                .filter(t -> t.getExitTime() != null && t.getExitTime().startsWith(todayDate))
                .mapToDouble(ParkingTicket::getParkingFee)
                .sum();

        double totalRevenue = completed.stream()
                .mapToDouble(ParkingTicket::getParkingFee)
                .sum();

        long totalBilledHours = completed.stream()
                .mapToLong(ParkingTicket::getBilledHours)
                .sum();

        double avgFee = completed.isEmpty() ? 0.0 : Math.round((totalRevenue / completed.size()) * 100.0) / 100.0;

        Map<String, Double> revenueByType = new LinkedHashMap<>();
        revenueByType.put("BIKE", completed.stream().filter(t -> "BIKE".equalsIgnoreCase(t.getVehicleType())).mapToDouble(ParkingTicket::getParkingFee).sum());
        revenueByType.put("CAR", completed.stream().filter(t -> "CAR".equalsIgnoreCase(t.getVehicleType())).mapToDouble(ParkingTicket::getParkingFee).sum());
        revenueByType.put("TRUCK", completed.stream().filter(t -> "TRUCK".equalsIgnoreCase(t.getVehicleType())).mapToDouble(ParkingTicket::getParkingFee).sum());

        report.put("todayRevenue", todayRevenue);
        report.put("totalRevenue", totalRevenue);
        report.put("totalCompletedSessions", completed.size());
        report.put("totalBilledHours", totalBilledHours);
        report.put("averageFeePerSession", avgFee);
        report.put("revenueByType", revenueByType);

        return report;
    }

    private void persistAll() {
        dataStorage.saveSlots(parkingLot.getAllSlots());
        dataStorage.saveVehicles(registeredVehicles);
        dataStorage.saveTickets(tickets);
    }
}
