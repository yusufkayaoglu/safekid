package com.safekid.ai.service;

import com.safekid.child.entity.CocukKonumEntity;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class LocationDataCollector {

    private static final DateTimeFormatter FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Europe/Istanbul"));

    public String formatLocationsForPrompt(List<CocukKonumEntity> locations) {
        if (locations.isEmpty()) return "Konum verisi yok.";

        StringBuilder sb = new StringBuilder();
        sb.append("Zaman | Enlem | Boylam | Hız(km/h) | Mesafe(m)\n");
        sb.append("------|-------|--------|-----------|----------\n");

        CocukKonumEntity prev = null;
        for (CocukKonumEntity loc : locations) {
            double speed = 0;
            double distance = 0;
            if (prev != null) {
                distance = haversine(prev.getLat(), prev.getLng(), loc.getLat(), loc.getLng());
                long seconds = loc.getRecordedAt().getEpochSecond() - prev.getRecordedAt().getEpochSecond();
                if (seconds > 0) {
                    speed = (distance / 1000.0) / (seconds / 3600.0);
                }
            }
            sb.append(String.format("%s | %.6f | %.6f | %.1f | %.0f%n",
                    FMT.format(loc.getRecordedAt()),
                    loc.getLat(), loc.getLng(), speed, distance));
            prev = loc;
        }
        return sb.toString();
    }

    public double calculateTotalDistanceKm(List<CocukKonumEntity> locations) {
        double total = 0;
        for (int i = 1; i < locations.size(); i++) {
            total += haversine(
                    locations.get(i - 1).getLat(), locations.get(i - 1).getLng(),
                    locations.get(i).getLat(), locations.get(i).getLng());
        }
        return total / 1000.0;
    }

    public double calculateSpeedKmh(CocukKonumEntity a, CocukKonumEntity b) {
        double distanceM = haversine(a.getLat(), a.getLng(), b.getLat(), b.getLng());
        long seconds = b.getRecordedAt().getEpochSecond() - a.getRecordedAt().getEpochSecond();
        if (seconds <= 0) return 0;
        return (distanceM / 1000.0) / (seconds / 3600.0);
    }

    /**
     * Haversine formula: distance between two GPS coordinates in metres.
     */
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6_371_000; // Earth radius in metres
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
