package com.parksmart.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for date/time formatting and duration calculation.
 */
public class DateTimeUtils {
    public static final String STANDARD_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(STANDARD_FORMAT);

    public static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(FORMATTER);
    }

    public static LocalDateTime parse(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(dateTimeStr.trim(), FORMATTER);
        } catch (Exception e) {
            // Fallback for ISO format if present
            try {
                return LocalDateTime.parse(dateTimeStr.trim());
            } catch (Exception ex) {
                return LocalDateTime.now();
            }
        }
    }

    /**
     * Calculates the duration in minutes between two timestamps.
     */
    public static long calculateDurationMinutes(String startStr, String endStr) {
        LocalDateTime start = parse(startStr);
        LocalDateTime end = parse(endStr);
        Duration duration = Duration.between(start, end);
        long minutes = duration.toMinutes();
        return Math.max(0, minutes);
    }

    /**
     * Formats minutes into human-readable duration: e.g. "2 hrs 15 mins" or "45 mins".
     */
    public static String formatDuration(long minutes) {
        if (minutes <= 0) {
            return "< 1 min";
        }
        long hours = minutes / 60;
        long remainingMins = minutes % 60;

        if (hours == 0) {
            return remainingMins + " min" + (remainingMins == 1 ? "" : "s");
        } else if (remainingMins == 0) {
            return hours + " hr" + (hours == 1 ? "" : "s");
        } else {
            return hours + " hr" + (hours == 1 ? "" : "s") + " " + remainingMins + " min" + (remainingMins == 1 ? "" : "s");
        }
    }
}
