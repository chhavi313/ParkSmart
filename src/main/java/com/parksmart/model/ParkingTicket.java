package com.parksmart.model;

/**
 * Represents a parking receipt and audit ticket issued upon vehicle entry.
 * Tracks entry/exit timestamps, slot assignment, duration, and calculated billing fee.
 */
public class ParkingTicket {
    private String ticketId;
    private String vehicleRegistration;
    private String vehicleType;
    private String ownerName;
    private String contactNumber;
    private String slotNumber;
    private SlotType slotType;
    private String entryTime;
    private String exitTime;
    private long durationMinutes;
    private String parkingDuration;
    private long billedHours;
    private double hourlyRate;
    private double parkingFee;
    private TicketStatus ticketStatus;

    public ParkingTicket() {
    }

    public ParkingTicket(String ticketId, String vehicleRegistration, String vehicleType,
                         String ownerName, String contactNumber, String slotNumber,
                         SlotType slotType, String entryTime, double hourlyRate) {
        this.ticketId = ticketId;
        this.vehicleRegistration = vehicleRegistration;
        this.vehicleType = vehicleType;
        this.ownerName = ownerName;
        this.contactNumber = contactNumber;
        this.slotNumber = slotNumber;
        this.slotType = slotType;
        this.entryTime = entryTime;
        this.hourlyRate = hourlyRate;
        this.ticketStatus = TicketStatus.ACTIVE;
        this.exitTime = null;
        this.durationMinutes = 0;
        this.parkingDuration = "Active";
        this.billedHours = 0;
        this.parkingFee = 0.0;
    }

    // Getters and Setters
    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
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

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(String slotNumber) {
        this.slotNumber = slotNumber;
    }

    public SlotType getSlotType() {
        return slotType;
    }

    public void setSlotType(SlotType slotType) {
        this.slotType = slotType;
    }

    public String getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(String entryTime) {
        this.entryTime = entryTime;
    }

    public String getExitTime() {
        return exitTime;
    }

    public void setExitTime(String exitTime) {
        this.exitTime = exitTime;
    }

    public long getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(long durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getParkingDuration() {
        return parkingDuration;
    }

    public void setParkingDuration(String parkingDuration) {
        this.parkingDuration = parkingDuration;
    }

    public long getBilledHours() {
        return billedHours;
    }

    public void setBilledHours(long billedHours) {
        this.billedHours = billedHours;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public double getParkingFee() {
        return parkingFee;
    }

    public void setParkingFee(double parkingFee) {
        this.parkingFee = parkingFee;
    }

    public TicketStatus getTicketStatus() {
        return ticketStatus;
    }

    public void setTicketStatus(TicketStatus ticketStatus) {
        this.ticketStatus = ticketStatus;
    }
}
