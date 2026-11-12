package com.parksmart.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility helper methods for HTTP request parsing and response handling.
 */
public class HttpUtils {
    private static final Gson gson = new Gson();

    /**
     * Reads the full string body from an incoming HTTP request.
     */
    public static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toString(StandardCharsets.UTF_8);
    }

    /**
     * Parses query string parameters into a key-value Map.
     */
    public static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.trim().isEmpty()) {
            return params;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] idx = pair.split("=", 2);
            if (idx.length == 2) {
                try {
                    String key = URLDecoder.decode(idx[0], StandardCharsets.UTF_8);
                    String val = URLDecoder.decode(idx[1], StandardCharsets.UTF_8);
                    params.put(key, val);
                } catch (Exception ignored) {}
            } else if (idx.length == 1) {
                try {
                    String key = URLDecoder.decode(idx[0], StandardCharsets.UTF_8);
                    params.put(key, "");
                } catch (Exception ignored) {}
            }
        }
        return params;
    }

    /**
     * Sends a successful JSON response with HTTP 200/201.
     */
    public static void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        Map<String, Object> responseMap = new LinkedHashMap<>();
        responseMap.put("success", statusCode >= 200 && statusCode < 300);
        responseMap.put("data", data);

        String json = gson.toJson(responseMap);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Sends an error JSON response with descriptive message.
     */
    public static void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> responseMap = new LinkedHashMap<>();
        responseMap.put("success", false);
        responseMap.put("error", message);

        String json = gson.toJson(responseMap);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Handles CORS preflight OPTIONS requests.
     */
    public static void handleCorsPreflight(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(204, -1);
    }
}
