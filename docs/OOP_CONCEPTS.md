# ParkSmart - Object-Oriented Programming (OOP) Architecture & Concepts

This document provides an in-depth breakdown of how the four fundamental Object-Oriented Programming (OOP) principles are implemented in the **ParkSmart** application, complete with actual Java code examples directly taken from the codebase.

---

## 1. Abstraction

### Conceptual Definition
Abstraction is the process of exposing only the essential features of an entity while hiding unnecessary implementation complexities from the outside world. In Java, abstraction is achieved using **abstract classes** and **interfaces**.

### Implementation in ParkSmart: `com.parksmart.model.Vehicle`
In ParkSmart, `Vehicle` is declared as an `abstract class`. You cannot instantiate a generic vehicle (e.g., `new Vehicle()`), because in reality, any parked vehicle must be a concrete type (such as a Car, Bike, or Truck).

```java
package com.parksmart.model;

public abstract class Vehicle {
    private String vehicleId;
    private String registrationNumber;
    private String ownerName;
    private String contactNumber;
    private String vehicleType;

    public Vehicle(String vehicleId, String registrationNumber, String ownerName, String contactNumber, String vehicleType) {
        this.vehicleId = vehicleId;
        this.registrationNumber = registrationNumber != null ? registrationNumber.trim().toUpperCase() : "";
        this.ownerName = ownerName != null ? ownerName.trim() : "";
        this.contactNumber = contactNumber != null ? contactNumber.trim() : "";
        this.vehicleType = vehicleType;
    }

    // Abstract methods defining the behavioral contract
    public abstract SlotType getRequiredSlotType();
    public abstract String getVehicleCategory();
    public abstract double getHourlyRate();

    // Concrete getters and setters...
}
```

### Why this is effective:
- **Contract Enforcement:** Any developer adding a new vehicle type (e.g., `ElectricScooter` or `Bus`) is strictly forced by the Java compiler to implement `getRequiredSlotType()`, `getVehicleCategory()`, and `getHourlyRate()`.
- **System Decoupling:** The `ParkingLot` and `ParkingFeeCalculator` interact only with the abstract `Vehicle` type rather than knowing the internal details of specific subclasses.

---

## 2. Inheritance

### Conceptual Definition
Inheritance is the mechanism by which a child (derived) class acquires properties and behaviors from a parent (base) class. It facilitates code reusability and establishes an **IS-A** relationship.

### Implementation in ParkSmart: `Bike`, `Car`, and `Truck`
The subclasses `Bike`, `Car`, and `Truck` extend the `Vehicle` base class.

#### Example 1: `com.parksmart.model.Car`
```java
package com.parksmart.model;

public class Car extends Vehicle {

    public Car(String vehicleId, String registrationNumber, String ownerName, String contactNumber) {
        // Reuses the base class constructor through super()
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
```

#### Example 2: `com.parksmart.model.Bike`
```java
package com.parksmart.model;

public class Bike extends Vehicle {

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
```

#### Example 3: `com.parksmart.model.Truck`
```java
package com.parksmart.model;

public class Truck extends Vehicle {

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
```

### Benefits Demonstrated:
- **Code Reuse:** Fields such as `registrationNumber`, `ownerName`, `contactNumber`, and their validation logic are defined once in `Vehicle` and reused by all child classes without duplication.
- **Hierarchical Classification:** Establishes clear domain models: `Car IS-A Vehicle`, `Bike IS-A Vehicle`, `Truck IS-A Vehicle`.

---

## 3. Polymorphism

### Conceptual Definition
Polymorphism ("many forms") allows objects of different classes to be treated through a unified interface.
- **Compile-time Polymorphism (Static):** Method overloading.
- **Runtime Polymorphism (Dynamic):** Method overriding and dynamic method dispatch.

### Implementation in ParkSmart

#### A. Runtime Polymorphism in Slot Allocation
In `com.parksmart.model.ParkingLot`, the method `findSuitableSlot(Vehicle vehicle)` takes an abstract `Vehicle` reference:

```java
public synchronized Optional<ParkingSlot> findSuitableSlot(Vehicle vehicle) {
    if (vehicle == null) return Optional.empty();

    // Dynamic dispatch executes the overridden getRequiredSlotType()
    SlotType minRequired = vehicle.getRequiredSlotType();

    if (minRequired == SlotType.SMALL) {
        // Bike: Smallest suitable slot first (SMALL -> MEDIUM -> LARGE)
        Optional<ParkingSlot> small = findFirstAvailable(SlotType.SMALL);
        if (small.isPresent()) return small;
        Optional<ParkingSlot> med = findFirstAvailable(SlotType.MEDIUM);
        if (med.isPresent()) return med;
        return findFirstAvailable(SlotType.LARGE);

    } else if (minRequired == SlotType.MEDIUM) {
        // Car: MEDIUM -> LARGE
        Optional<ParkingSlot> med = findFirstAvailable(SlotType.MEDIUM);
        if (med.isPresent()) return med;
        return findFirstAvailable(SlotType.LARGE);

    } else if (minRequired == SlotType.LARGE) {
        // Truck: LARGE only
        return findFirstAvailable(SlotType.LARGE);
    }
    return Optional.empty();
}
```

#### B. Polymorphic Fee Calculation
In `com.parksmart.service.ParkingFeeCalculator`:
```java
public double calculateFee(Vehicle vehicle, long durationMinutes) {
    if (vehicle == null) {
        return calculateFee("CAR", durationMinutes);
    }
    // Dynamic dispatch to vehicle.getHourlyRate()
    double rate = vehicle.getHourlyRate();
    long billedHours = calculateBilledHours(durationMinutes);
    return billedHours * rate;
}
```

#### C. Compile-time Polymorphism (Method Overloading)
`ParkingFeeCalculator` provides multiple overloaded forms of `calculateFee()`:
```java
// Overload 1: Using Vehicle entity
public double calculateFee(Vehicle vehicle, long durationMinutes)

// Overload 2: Using String category identifier
public double calculateFee(String vehicleType, long durationMinutes)
```

---

## 4. Encapsulation

### Conceptual Definition
Encapsulation is the bundling of data (fields) and methods that manipulate that data into a single unit, while restricting direct access from outside the class using access modifiers (`private`, `protected`, `public`).

### Implementation in ParkSmart: `com.parksmart.model.ParkingSlot`
All instance variables are marked `private`. State modifications occur exclusively through synchronized, validated methods:

```java
package com.parksmart.model;

public class ParkingSlot {
    private String slotId;
    private int slotNumber;
    private SlotType slotType;
    private boolean occupied;
    private String vehicleRegistration;
    private String vehicleType;
    private String occupiedAt;

    // Direct access to fields is restricted.
    // Encapsulated state transition method:
    public void assignVehicle(String registration, String type, String timestamp) {
        this.occupied = true;
        this.vehicleRegistration = registration != null ? registration.trim().toUpperCase() : null;
        this.vehicleType = type;
        this.occupiedAt = timestamp;
    }

    public void vacate() {
        this.occupied = false;
        this.vehicleRegistration = null;
        this.vehicleType = null;
        this.occupiedAt = null;
    }

    public boolean isAvailable() {
        return !occupied;
    }

    // Controlled getters & setters...
    public String getSlotId() { return slotId; }
    public boolean isOccupied() { return occupied; }
}
```

### Benefits Demonstrated:
- **Data Protection:** External code cannot accidentally set `occupied = false` while leaving `vehicleRegistration` set to an existing vehicle. The `vacate()` method guarantees clean state transitions.
- **Input Sanitization:** Setters ensure registration numbers are trimmed and standardized to uppercase.

---

## Summary Matrix

| OOP Principle | Primary Class(es) Involved | Mechanism Demonstrated | Viva Talking Point |
|---|---|---|---|
| **Abstraction** | `Vehicle` | `abstract class`, abstract methods | Hides vehicle implementation; defines uniform contract |
| **Inheritance** | `Bike`, `Car`, `Truck` | `extends Vehicle`, `super()` | Reuses common attributes (`registration`, `owner`, `id`) |
| **Polymorphism** | `ParkingLot`, `ParkingFeeCalculator` | Overriding (`@Override`) & Overloading | Dynamic method dispatch chooses slot & rate at runtime |
| **Encapsulation** | `ParkingSlot`, `ParkingTicket`, `Vehicle` | `private` variables, getters/setters | Guarantees atomic state updates and data consistency |
