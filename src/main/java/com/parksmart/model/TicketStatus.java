package com.parksmart.model;

/**
 * Status of a parking ticket.
 * - ACTIVE: The vehicle is currently parked inside the lot.
 * - COMPLETED: The vehicle has exited and the parking fee has been billed.
 */
public enum TicketStatus {
    ACTIVE,
    COMPLETED
}
