package com.parksmart.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.parksmart.model.ParkingSlot;
import com.parksmart.model.ParkingTicket;
import com.parksmart.model.Vehicle;
import com.parksmart.service.ParkingManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Built-in lightweight HTTP server hosting REST APIs and serving the static frontend.
 * Provides all REST endpoints required by the ParkSmart dashboard.
 */
public class SmartParkingServer {
    private final int port;
    private final ParkingManager parkingManager;
    private final Gson gson;
    private HttpServer server;

    public SmartParkingServer(int port, ParkingManager parkingManager) {
        this.port = port;
        this.parkingManager = parkingManager;
        this.gson = new Gson();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(10));

        // Register API Handlers
        server.createContext("/api/health", new HealthHandler());
        server.createContext("/api/dashboard", new DashboardHandler());
        server.createContext("/api/slots", new SlotsHandler());
        server.createContext("/api/vehicles/entry", new VehicleEntryHandler());
        server.createContext("/api/vehicles/exit", new VehicleExitHandler());
        server.createContext("/api/vehicles/search", new VehicleSearchHandler());
        server.createContext("/api/vehicles", new VehiclesListHandler());
        server.createContext("/api/parking/active", new ActiveParkingHandler());
        server.createContext("/api/history", new HistoryHandler());
        server.createContext("/api/revenue", new RevenueHandler());

        // Static Web Content Handler for root and assets
        server.createContext("/", new StaticFileHandler());

        server.start();
        System.out.println("==================================================================");
        System.out.println("  ParkSmart Server is running successfully!");
        System.out.println("  Local Web UI URL: http://localhost:" + port);
        System.out.println("  Health API:       http://localhost:" + port + "/api/health");
        System.out.println("==================================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            System.out.println("[ParkSmart Server] Server stopped gracefully.");
        }
    }

    public int getPort() {
        return port;
    }

    // =========================================================================
    // API HANDLERS
    // =========================================================================

    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.handleCorsPreflight(exchange);
                return;
            }
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("service", "ParkSmart Smart Parking Backend");
            health.put("timestamp", LocalDateTime.now().toString());
            health.put("totalSlots", parkingManager.getParkingLot().getTotalSlotsCount());
            health.put("occupiedSlots", parkingManager.getParkingLot().getOccupiedSlotsCount());
            HttpUtils.sendJsonResponse(exchange, 200, health);
        }
    }

    private class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.handleCorsPreflight(exchange);
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.sendErrorResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            try {
                Map<String, Object> stats = parkingManager.getDashboardStatistics();
                HttpUtils.sendJsonResponse(exchange, 200, stats);
            } catch (Exception e) {
                HttpUtils.sendErrorResponse(exchange, 500, "Failed to load dashboard: " + e.getMessage());
            }
        }
    }

    private class SlotsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.handleCorsPreflight(exchange);
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.sendErrorResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            List<ParkingSlot> slots = parkingManager.getParkingLot().getAllSlots();
            HttpUtils.sendJsonResponse(exchange, 200, slots);
        }
    }

    private class VehicleEntryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.handleCorsPreflight(exchange);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.sendErrorResponse(exchange, 405, "Method Not Allowed. Use POST.");
                return;
            }

            try {
                String body = HttpUtils.readRequestBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();

                String reg = json.has("registrationNumber") ? json.get("registrationNumber").getAsString() : "";
                String owner = json.has("ownerName") ? json.get("ownerName").getAsString() : "";
                String type = json.has("vehicleType") ? json.get("vehicleType").getAsString() : "";
                String contact = json.has("contactNumber") ? json.get("contactNumber").getAsString() : "";

                ParkingTicket ticket = parkingManager.registerAndParkVehicle(reg, owner, type, contact);
                HttpUtils.sendJsonResponse(exchange, 201, ticket);
            } catch (IllegalArgumentException e) {
                HttpUtils.sendErrorResponse(exchange, 400, e.getMessage());
            } catch (IllegalStateException e) {
                HttpUtils.sendErrorResponse(exchange, 409, e.getMessage());
            } catch (Exception e) {
                HttpUtils.sendErrorResponse(exchange, 500, "Vehicle entry failed: " + e.getMessage());
            }
        }
    }

    private class VehicleExitHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.handleCorsPreflight(exchange);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.sendErrorResponse(exchange, 405, "Method Not Allowed. Use POST.");
                return;
            }

            try {
                String body = HttpUtils.readRequestBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();

                String identifier = "";
                if (json.has("identifier")) {
                    identifier = json.get("identifier").getAsString();
                } else if (json.has("ticketId")) {
                    identifier = json.get("ticketId").getAsString();
                } else if (json.has("registrationNumber")) {
                    identifier = json.get("registrationNumber").getAsString();
                }

                ParkingTicket receipt = parkingManager.processVehicleExit(identifier);
                HttpUtils.sendJsonResponse(exchange, 200, receipt);
            } catch (IllegalArgumentException e) {
                HttpUtils.sendErrorResponse(exchange, 404, e.getMessage());
            } catch (Exception e) {
                HttpUtils.sendErrorResponse(exchange, 500, "Vehicle exit failed: " + e.getMessage());
            }
        }
    }

    private class VehicleSearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.handleCorsPreflight(exchange);
                return;
            }
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = HttpUtils.parseQueryParams(query);
            String q = params.getOrDefault("q", params.getOrDefault("registration", ""));

            List<ParkingTicket> results = parkingManager.searchTickets(q);
            HttpUtils.sendJsonResponse(exchange, 200, results);
        }
    }

    private class VehiclesListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.handleCorsPreflight(exchange);
                return;
            }
            List<Vehicle> vehicles = parkingManager.getRegisteredVehicles();
            HttpUtils.sendJsonResponse(exchange, 200, vehicles);
        }
    }

    private class ActiveParkingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.handleCorsPreflight(exchange);
                return;
            }
            List<ParkingTicket> active = parkingManager.getActiveParkingSessions();
            HttpUtils.sendJsonResponse(exchange, 200, active);
        }
    }

    private class HistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.handleCorsPreflight(exchange);
                return;
            }
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = HttpUtils.parseQueryParams(query);
            String search = params.getOrDefault("search", params.getOrDefault("q", ""));

            List<ParkingTicket> history;
            if (!search.isEmpty()) {
                history = parkingManager.searchTickets(search);
            } else {
                history = parkingManager.getParkingHistory();
            }
            HttpUtils.sendJsonResponse(exchange, 200, history);
        }
    }

    private class RevenueHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.handleCorsPreflight(exchange);
                return;
            }
            Map<String, Object> revenue = parkingManager.getRevenueReport();
            HttpUtils.sendJsonResponse(exchange, 200, revenue);
        }
    }

    // =========================================================================
    // STATIC FILE HANDLER
    // =========================================================================

    private static class StaticFileHandler implements HttpHandler {
        private final File staticDir;

        public StaticFileHandler() {
            // First check src/main/resources/static
            File dir = new File("src/main/resources/static");
            if (!dir.exists()) {
                dir = new File("classes/static");
            }
            this.staticDir = dir;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Clean path to prevent path traversal
            path = path.replace("\\", "/");
            if (path.contains("..")) {
                HttpUtils.sendErrorResponse(exchange, 403, "Access Denied");
                return;
            }

            // Remove leading slash for relative file lookup
            String relativePath = path.startsWith("/") ? path.substring(1) : path;

            // Try reading from file system
            File targetFile = new File(staticDir, relativePath);
            if (targetFile.exists() && !targetFile.isDirectory()) {
                serveFile(exchange, targetFile);
                return;
            }

            // Fallback: Check classpath resource
            String resourcePath = "/static/" + relativePath;
            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is != null) {
                    byte[] bytes = is.readAllBytes();
                    String contentType = getMimeType(relativePath);
                    exchange.getResponseHeaders().set("Content-Type", contentType);
                    exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                    return;
                }
            }

            // 404 Not Found
            String error = "<html><body><h2>404 Not Found</h2><p>Resource " + path + " not found.</p></body></html>";
            byte[] errorBytes = error.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(404, errorBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorBytes);
            }
        }

        private void serveFile(HttpExchange exchange, File file) throws IOException {
            String mimeType = getMimeType(file.getName());
            exchange.getResponseHeaders().set("Content-Type", mimeType);
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");

            byte[] bytes = Files.readAllBytes(file.toPath());
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String getMimeType(String filename) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
            if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
            if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
            if (lower.endsWith(".svg")) return "image/svg+xml";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".ico")) return "image/x-icon";
            return "text/plain; charset=UTF-8";
        }
    }
}
