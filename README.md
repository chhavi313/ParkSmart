# ParkSmart - Smart Parking & Vehicle Management System

[![Java Version](https://img.shields.io/badge/Java-17%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Server](https://img.shields.io/badge/Server-Java%20Built--in%20HttpServer-blue.svg)](https://docs.oracle.com/en/java/javase/17/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html)
[![Storage](https://img.shields.io/badge/Persistence-Local%20JSON%20(Gson)-green.svg)](https://github.com/google/gson)
[![Frontend](https://img.shields.io/badge/UI-Vanilla%20JS%20%7C%20CSS3%20%7C%20HTML5-purple.svg)](/)

> A complete, fully functional, presentation-ready Java Object-Oriented Programming (OOP) web application. Designed for college-level second-year OOP evaluations, featuring a modern dark-themed SaaS dashboard and clean, explainable internal Java architecture.

---

## 📌 Table of Contents
1. [Project Overview](#-project-run.batoverview)
2. [Problem Statement & Objectives](#-problem-statement--objectives)
3. [Key Features](#-key-features)
4. [Technology Stack](#-technology-stack)
5. [OOP Concepts & Architecture](#-oop-concepts--architecture)
6. [Parking Slot Allocation Rules](#-parking-slot-allocation-rules)
7. [Tariff & Billing Formula](#-tariff--billing-formula)
8. [Data Persistence & Storage](#-data-persistence--storage)
9. [REST API Documentation](#-rest-api-documentation)
10. [How to Compile & Run](#-how-to-compile--run)
11. [Project Structure](#-project-structure)
12. [Screenshots](#-screenshots)
13. [Academic Documentation](#-academic-documentation)

---

## 🚗 Project Overview
**ParkSmart** is an automated smart parking facility management system. It models physical parking spaces, classifies vehicles hierarchically, applies an intelligent smallest-slot-fit-first allocation algorithm, tracks duration elapsed, and computes exact billing upon departure.

Unlike typical monolithic or bloated web applications, ParkSmart is:
- **Zero Heavy Frameworks:** No Spring Boot, No React, No Node.js.
- **Pure Java 17:** Powered by Java's native `com.sun.net.httpserver.HttpServer`.
- **100% Real Backend Data:** No hardcoded statistics or fake dashboard counters.
- **Presentation-Ready:** Premium dark-themed dashboard with live SVG icons, status indicators, and printable digital receipts.

---

## 🎯 Problem Statement & Objectives

### Problem
Commercial garages and parking lots struggle with:
- Heavy commercial vehicles occupying compact spaces or bikes hogging large slots.
- Manual ticketing errors, lost paper slips, and arbitrary fee estimation.
- Inaccurate financial records and lack of real-time occupancy visibility.

### Objectives
- Automate slot allocation based on strict vehicle requirements (Bike, Car, Truck).
- Calculate parking duration and fees using documented ceiling billing algorithms.
- Maintain permanent audit logs of all active and historical sessions.
- Provide a single-URL web dashboard running directly from the Java server.

---

## ✨ Key Features
- **Live Interactive Dashboard:** Real-time metrics for capacity, occupied slots, available slots, occupancy percentage, and today's revenue.
- **Visual Parking Grid:** Color-coded 30-slot interactive layout (Green = Available, Red = Occupied) with vehicle tags.
- **Smart Entry & Check-In:** Form with immediate capacity feedback and automatic smallest-fit slot assignment.
- **Departure & Billing:** One-click checkout with itemized duration breakdown, hourly rates, and total fee.
- **Official Digital Receipts:** Formatted printable receipt supporting browser print-to-PDF (`window.print()`).
- **Comprehensive Audit History:** Searchable by license plate or ticket ID, with filters for vehicle category and status.
- **Financial Analytics:** Revenue distribution charts broken down by Two-Wheelers, Four-Wheelers, and Commercial Vehicles.
- **Persistent Storage:** Local file persistence in `data/` using Google Gson; state is preserved across restarts.

---

## 🛠 Technology Stack
- **Backend:** Java 17 LTS
- **HTTP Server:** Java built-in `com.sun.net.httpserver.HttpServer`
- **JSON Processing:** Google Gson 2.10.1
- **Build System:** Apache Maven & Standalone Batch Scripts
- **Frontend:** HTML5, CSS3 (Modern Dark Theme), Vanilla JavaScript (ES6+ Fetch API)
- **Typography:** Plus Jakarta Sans & JetBrains Mono

---

## 🏛 OOP Concepts & Architecture

ParkSmart demonstrates all four foundational pillars of Object-Oriented Programming:

### 1. Abstraction (`Vehicle.java`)
`Vehicle` is an abstract class defining core attributes (`vehicleId`, `registrationNumber`, `ownerName`) and enforcing abstract contracts:
```java
public abstract SlotType getRequiredSlotType();
public abstract String getVehicleCategory();
public abstract double getHourlyRate();
```

### 2. Inheritance (`Car.java`, `Bike.java`, `Truck.java`)
Child classes extend `Vehicle`, reusing common code while providing specific characteristics:
- `Bike extends Vehicle` &rarr; Requires `SMALL` slot, rate ₹20/hr
- `Car extends Vehicle` &rarr; Requires `MEDIUM` slot, rate ₹40/hr
- `Truck extends Vehicle` &rarr; Requires `LARGE` slot, rate ₹70/hr

### 3. Polymorphism (Runtime & Compile-Time)
- **Runtime Polymorphism:** `ParkingLot.findSuitableSlot(Vehicle vehicle)` operates on abstract `Vehicle` references. Dynamic method dispatch invokes the overridden methods at runtime.
- **Compile-Time Polymorphism:** Method overloading in `ParkingFeeCalculator.calculateFee(...)`.

### 4. Encapsulation (`ParkingSlot.java`, `ParkingTicket.java`)
All entity variables are `private`. State changes occur through synchronized, validated methods (`assignVehicle`, `vacate`).

---

## 🅿 Parking Slot Allocation Rules

The facility contains **30 configured parking slots**:
- **10 SMALL Slots:** `S-01` to `S-10`
- **15 MEDIUM Slots:** `M-01` to `M-15`
- **5 LARGE Slots:** `L-01` to `L-05`

### Allocation Hierarchy (Smallest-Suitable-First)
- **Bike:** Can fit in SMALL, MEDIUM, or LARGE. Allocates SMALL first &rarr; then MEDIUM &rarr; then LARGE.
- **Car:** Can fit in MEDIUM or LARGE. Allocates MEDIUM first &rarr; then LARGE. (Cannot fit in SMALL).
- **Truck:** Strictly requires LARGE. Allocates LARGE only.

---

## 💰 Tariff & Billing Formula

Implemented in `com.parksmart.service.ParkingFeeCalculator`:

```
durationMinutes = differenceInMinutes(entryTime, exitTime);
billedHours     = Math.max(1, (long) Math.ceil(durationMinutes / 60.0));
totalParkingFee = billedHours * vehicleHourlyRate;
```

### Default Hourly Rates
| Vehicle Category | Minimum Slot | Hourly Rate | Minimum Charge |
|---|---|---|---|
| **Two-Wheeler (Bike)** | SMALL | **₹20.00 / hr** | ₹20.00 |
| **Four-Wheeler (Car)** | MEDIUM | **₹40.00 / hr** | ₹40.00 |
| **Commercial (Truck)** | LARGE | **₹70.00 / hr** | ₹70.00 |

*Rules:*
1. Minimum 1 hour charge applies to all vehicles.
2. Partial hours are rounded UP to the next full billing hour (e.g., 65 mins &rarr; 2 hrs).

---

## 💾 Data Persistence & Storage

Data is stored locally in the `data/` folder as structured JSON files:
- `data/slots.json` &rarr; Status and occupancy of all 30 slots.
- `data/vehicles.json` &rarr; Registered vehicle profiles.
- `data/tickets.json` &rarr; Active and historical parking tickets.

Files are loaded on server startup and saved automatically after every vehicle check-in and checkout.

---

## 📡 REST API Documentation

| Method | Endpoint | Description | Status Codes |
|---|---|---|---|
| `GET` | `/api/health` | Server health status and total slots | `200` |
| `GET` | `/api/dashboard` | Aggregated dashboard statistics | `200` |
| `GET` | `/api/slots` | Array of all 30 slots and occupancy status | `200` |
| `POST` | `/api/vehicles/entry` | Register vehicle and allocate smallest slot | `201`, `400`, `409` |
| `POST` | `/api/vehicles/exit` | Checkout vehicle, release slot, and bill fee | `200`, `404` |
| `GET` | `/api/parking/active` | List of currently parked active sessions | `200` |
| `GET` | `/api/history` | Historical audit log of all tickets | `200` |
| `GET` | `/api/revenue` | Revenue breakdown by vehicle category | `200` |

---

## 🚀 How to Compile & Run

### Prerequisites
- Java 17 LTS installed and on system PATH (`java -version`, `javac -version`).

### Method 1: One-Click Windows Script (Easiest)
Simply double click or run:
```cmd
run.bat
```
This automatically compiles the project and launches the server.

### Method 2: Standalone Compilation & Run
```cmd
compile.bat
java -jar target/parksmart-1.0.0.jar
```

### Method 3: Using Apache Maven
```cmd
mvn clean package
mvn exec:java
```

### Accessing the Web Application
Open your browser and navigate to:
```
http://localhost:8080
```

---

## 📂 Project Structure
```
ParkSmart/
├── pom.xml                               # Maven project configuration
├── run.bat                               # Windows 1-click startup script
├── compile.bat                           # Standalone compilation script
├── README.md                             # Project overview and instructions
├── .gitignore                            # Git ignore rules
│
├── data/                                 # Persistent JSON files (auto-generated)
│   ├── slots.json
│   ├── vehicles.json
│   └── tickets.json
│
├── docs/                                 # Academic project documentation
│   ├── OOP_CONCEPTS.md                   # In-depth OOP analysis with code examples
│   ├── PROJECT_REPORT.md                 # Complete formal project report
│   └── VIVA_QUESTIONS.md                 # 28 Viva questions & answers
│
├── src/
│   └── main/
│       ├── java/
│       │   └── com/parksmart/
│       │       ├── SmartParkingApplication.java   # Main application entry point
│       │       ├── model/
│       │       │   ├── SlotType.java              # Enum: SMALL, MEDIUM, LARGE
│       │       │   ├── TicketStatus.java          # Enum: ACTIVE, COMPLETED
│       │       │   ├── Vehicle.java               # Abstract base class
│       │       │   ├── Bike.java                  # Concrete class (extends Vehicle)
│       │       │   ├── Car.java                   # Concrete class (extends Vehicle)
│       │       │   ├── Truck.java                 # Concrete class (extends Vehicle)
│       │       │   ├── ParkingSlot.java           # Slot model & state transitions
│       │       │   ├── ParkingLot.java            # 30-slot grid & allocation logic
│       │       │   └── ParkingTicket.java         # Ticket & receipt model
│       │       ├── service/
│       │       │   ├── ParkingFeeCalculator.java  # Billing business logic
│       │       │   └── ParkingManager.java        # Core business coordinator
│       │       ├── storage/
│       │       │   └── DataStorage.java           # Gson local file persistence
│       │       ├── server/
│       │       │   ├── SmartParkingServer.java    # Embedded Java HttpServer
│       │       │   └── HttpUtils.java             # Request/response helper
│       │       └── util/
│       │           └── DateTimeUtils.java         # Duration & formatting utility
│       │
│       └── resources/
│           └── static/
│               ├── index.html                     # Single-Page web dashboard
│               ├── css/
│               │   └── style.css                  # Dark-themed responsive styles
│               └── js/
│                   └── app.js                     # REST API frontend controller
└── target/
    └── parksmart-1.0.0.jar                       # Standalone executable JAR
```

---

## 📸 Screenshots

| Section | Description |
|---|---|
| **Dashboard** | Real-time statistics, occupancy progress, and recent entries |
| **Slot Matrix** | 30 color-coded visual bays (Green = Available, Red = Occupied) |
| **Check-In Form** | Validated vehicle registration with live slot availability notice |
| **Active Parking** | Live table of parked vehicles with elapsed timer and 1-click checkout |
| **Billing & Receipt** | Itemized fee breakdown and official printable digital receipt |
| **History & Reports** | Searchable audit trail and revenue distribution by vehicle type |

---

## 🎓 Academic Documentation
For viva preparation and academic project submission, refer to the files in the `docs/` folder:
- [docs/OOP_CONCEPTS.md](docs/OOP_CONCEPTS.md): Detailed code breakdown of Abstraction, Inheritance, Polymorphism, and Encapsulation.
- [docs/PROJECT_REPORT.md](docs/PROJECT_REPORT.md): Formal college submission report with abstract, architecture diagrams, and test tables.