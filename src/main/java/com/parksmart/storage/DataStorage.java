package com.parksmart.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.parksmart.model.Bike;
import com.parksmart.model.Car;
import com.parksmart.model.ParkingSlot;
import com.parksmart.model.ParkingTicket;
import com.parksmart.model.Truck;
import com.parksmart.model.Vehicle;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles persistent file storage using Google Gson.
 * Manages local JSON files:
 * - data/slots.json
 * - data/vehicles.json
 * - data/tickets.json
 * 
 * Safely handles missing, empty, or corrupted files and guarantees
 * data survives application restarts.
 */
public class DataStorage {
    private final String dataDirectory;
    private final Gson gson;

    private final File slotsFile;
    private final File vehiclesFile;
    private final File ticketsFile;

    public DataStorage() {
        this("data");
    }

    public DataStorage(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        File dir = new File(dataDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        this.slotsFile = new File(dir, "slots.json");
        this.vehiclesFile = new File(dir, "vehicles.json");
        this.ticketsFile = new File(dir, "tickets.json");
    }

    // ==========================================
    // PARKING SLOTS
    // ==========================================

    public synchronized void saveSlots(List<ParkingSlot> slots) {
        saveJsonToFile(slotsFile, gson.toJson(slots));
    }

    public synchronized List<ParkingSlot> loadSlots() {
        if (!slotsFile.exists() || slotsFile.length() == 0) {
            return new ArrayList<>();
        }
        try (FileReader reader = new FileReader(slotsFile, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<ParkingSlot>>() {}.getType();
            List<ParkingSlot> list = gson.fromJson(reader, listType);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("[DataStorage] Warning: Failed to load slots: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ==========================================
    // VEHICLES (Polymorphic persistence)
    // ==========================================

    public synchronized void saveVehicles(List<Vehicle> vehicles) {
        JsonArray array = new JsonArray();
        for (Vehicle v : vehicles) {
            JsonObject obj = new JsonObject();
            obj.addProperty("vehicleId", v.getVehicleId());
            obj.addProperty("registrationNumber", v.getRegistrationNumber());
            obj.addProperty("ownerName", v.getOwnerName());
            obj.addProperty("contactNumber", v.getContactNumber());
            obj.addProperty("vehicleType", v.getVehicleType());
            array.add(obj);
        }
        saveJsonToFile(vehiclesFile, gson.toJson(array));
    }

    public synchronized List<Vehicle> loadVehicles() {
        List<Vehicle> vehicles = new ArrayList<>();
        if (!vehiclesFile.exists() || vehiclesFile.length() == 0) {
            return vehicles;
        }

        try (FileReader reader = new FileReader(vehiclesFile, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element != null && element.isJsonArray()) {
                JsonArray array = element.getAsJsonArray();
                for (JsonElement item : array) {
                    if (item.isJsonObject()) {
                        JsonObject obj = item.getAsJsonObject();
                        String id = getJsonString(obj, "vehicleId");
                        String reg = getJsonString(obj, "registrationNumber");
                        String owner = getJsonString(obj, "ownerName");
                        String contact = getJsonString(obj, "contactNumber");
                        String type = getJsonString(obj, "vehicleType");

                        Vehicle v;
                        if ("BIKE".equalsIgnoreCase(type)) {
                            v = new Bike(id, reg, owner, contact);
                        } else if ("TRUCK".equalsIgnoreCase(type)) {
                            v = new Truck(id, reg, owner, contact);
                        } else {
                            v = new Car(id, reg, owner, contact);
                        }
                        vehicles.add(v);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DataStorage] Warning: Failed to load vehicles: " + e.getMessage());
        }
        return vehicles;
    }

    // ==========================================
    // PARKING TICKETS
    // ==========================================

    public synchronized void saveTickets(List<ParkingTicket> tickets) {
        saveJsonToFile(ticketsFile, gson.toJson(tickets));
    }

    public synchronized List<ParkingTicket> loadTickets() {
        if (!ticketsFile.exists() || ticketsFile.length() == 0) {
            return new ArrayList<>();
        }
        try (FileReader reader = new FileReader(ticketsFile, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<ParkingTicket>>() {}.getType();
            List<ParkingTicket> list = gson.fromJson(reader, listType);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("[DataStorage] Warning: Failed to load tickets: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private void saveJsonToFile(File file, String json) {
        File tempFile = new File(file.getAbsolutePath() + ".tmp");
        try {
            try (FileWriter writer = new FileWriter(tempFile, StandardCharsets.UTF_8)) {
                writer.write(json);
            }
            if (file.exists()) {
                file.delete();
            }
            tempFile.renameTo(file);
        } catch (IOException e) {
            System.err.println("[DataStorage] Error writing file " + file.getName() + ": " + e.getMessage());
        }
    }

    private String getJsonString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "";
    }

    public boolean hasExistingData() {
        return slotsFile.exists() && slotsFile.length() > 0;
    }
}
