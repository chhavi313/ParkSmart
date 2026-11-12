package com.parksmart;

import com.parksmart.server.SmartParkingServer;
import com.parksmart.service.ParkingManager;
import com.parksmart.storage.DataStorage;

/**
 * Main application entry point for the ParkSmart Smart Parking and Vehicle Management System.
 * 
 * College Second-Year Java OOP Project
 */
public class SmartParkingApplication {
    public static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        int port = resolvePort(args);

        printAsciiBanner(port);

        try {
            // 1. Initialize file-based data persistence
            DataStorage dataStorage = new DataStorage("data");

            // 2. Initialize business logic manager with persistence
            ParkingManager parkingManager = new ParkingManager(dataStorage);

            // 3. Initialize and start HTTP server
            SmartParkingServer server = new SmartParkingServer(port, parkingManager);
            server.start();

            // 4. Register graceful shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[ParkSmart] Gracefully shutting down server...");
                server.stop();
                System.out.println("[ParkSmart] Goodbye!");
            }));

        } catch (Exception e) {
            System.err.println("[ParkSmart] FATAL ERROR: Failed to start application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static int resolvePort(String[] args) {
        // 1. Check command line arguments
        if (args != null && args.length > 0) {
            for (String arg : args) {
                if (arg.startsWith("--port=")) {
                    try {
                        return Integer.parseInt(arg.substring("--port=".length()));
                    } catch (NumberFormatException ignored) {}
                } else {
                    try {
                        return Integer.parseInt(arg);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        // 2. Check System Property
        String propPort = System.getProperty("server.port");
        if (propPort != null && !propPort.isEmpty()) {
            try {
                return Integer.parseInt(propPort);
            } catch (NumberFormatException ignored) {}
        }

        // 3. Check Environment Variable
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isEmpty()) {
            try {
                return Integer.parseInt(envPort);
            } catch (NumberFormatException ignored) {}
        }

        return DEFAULT_PORT;
    }

    private static void printAsciiBanner(int port) {
        System.out.println("  ____            _     ____                      _   ");
        System.out.println(" |  _ \\ __ _ _ __| | __/ ___| _ __ ___   __ _ _ __| |_ ");
        System.out.println(" | |_) / _` | '__| |/ /\\___ \\| '_ ` _ \\ / _` | '__| __|");
        System.out.println(" |  __/ (_| | |  |   <  ___) | | | | | | (_| | |  | |_ ");
        System.out.println(" |_|   \\__,_|_|  |_|\\_\\|____/|_| |_| |_|\\__,_|_|   \\__|");
        System.out.println("  Smart Parking & Vehicle Management System (Java OOP)   ");
        System.out.println("==========================================================");
        System.out.println("  Version:    1.0.0 (Java 17 + Built-in HttpServer)");
        System.out.println("  Local URL:  http://localhost:" + port);
        System.out.println("  Endpoints:  /api/dashboard | /api/slots | /api/parking/active");
        System.out.println("  Data Path:  data/ (slots.json, vehicles.json, tickets.json)");
        System.out.println("==========================================================");
    }
}
