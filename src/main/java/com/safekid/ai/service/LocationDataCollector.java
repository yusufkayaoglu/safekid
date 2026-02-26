package com.safekid.ai.service;

import com.safekid.child.entity.CocukKonumEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class LocationDataCollector {

    private static final DateTimeFormatter FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Europe/Istanbul"));

    /**
     * Konum listesini Claude'a gönderilecek formata çevirir.
     * <10 saniyelik aralıklar için hız hesaplanmaz (GPS gürültüsü filtresi).
     */
    public String formatLocationsForPrompt(List<CocukKonumEntity> locations) {
        if (locations.isEmpty()) return "Konum verisi yok.";

        StringBuilder sb = new StringBuilder();
        sb.append("Zaman | Enlem | Boylam | Ort.Hız(km/h) | Mesafe(m)\n");
        sb.append("------|-------|--------|--------------|----------\n");

        CocukKonumEntity prev = null;
        for (int i = 0; i < locations.size(); i++) {
            CocukKonumEntity loc = locations.get(i);
            double speed = 0;
            double distance = 0;
            if (prev != null) {
                distance = haversine(prev.getLat(), prev.getLng(), loc.getLat(), loc.getLng());
                long seconds = loc.getRecordedAt().getEpochSecond() - prev.getRecordedAt().getEpochSecond();
                if (seconds >= 10) {
                    speed = (distance / 1000.0) / (seconds / 3600.0);
                }
                // <10 sn → speed=0 bırak, GPS gürültüsü
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

    /**
     * İki ardışık nokta arası anlık hız. <10 sn ise 0 döner (GPS gürültüsü).
     */
    public double calculateSpeedKmh(CocukKonumEntity a, CocukKonumEntity b) {
        double distanceM = haversine(a.getLat(), a.getLng(), b.getLat(), b.getLng());
        long seconds = b.getRecordedAt().getEpochSecond() - a.getRecordedAt().getEpochSecond();
        if (seconds < 10) return 0;
        return (distanceM / 1000.0) / (seconds / 3600.0);
    }

    /**
     * Kayan pencere ortalama hızı.
     * endIndex noktasından geriye doğru windowSeconds kadar gidip
     * toplam mesafe / toplam süre hesabı yapar.
     * Pencerede en az 30 saniyelik veri yoksa 0 döner.
     */
    public double calculateWindowAverageSpeedKmh(List<CocukKonumEntity> locations,
                                                  int endIndex,
                                                  long windowSeconds) {
        if (endIndex <= 0) return 0;

        CocukKonumEntity end = locations.get(endIndex);
        Instant windowStart = end.getRecordedAt().minusSeconds(windowSeconds);

        int startIndex = endIndex;
        for (int i = endIndex - 1; i >= 0; i--) {
            if (!locations.get(i).getRecordedAt().isBefore(windowStart)) {
                startIndex = i;
            } else {
                break;
            }
        }

        if (startIndex == endIndex) return 0;

        long totalSeconds = end.getRecordedAt().getEpochSecond()
                - locations.get(startIndex).getRecordedAt().getEpochSecond();
        if (totalSeconds < 30) return 0; // Pencere çok dar, güvenilmez

        double totalDistanceM = 0;
        for (int i = startIndex + 1; i <= endIndex; i++) {
            totalDistanceM += haversine(
                    locations.get(i - 1).getLat(), locations.get(i - 1).getLng(),
                    locations.get(i).getLat(), locations.get(i).getLng());
        }

        return (totalDistanceM / 1000.0) / (totalSeconds / 3600.0);
    }

    /**
     * Tüm konumların centroid'ini hesaplayıp her noktanın centroid'e uzaklığının maksimumunu döner.
     * maxDist < thresholdM ise çocuk büyük ihtimalle sabittir (GPS sürüklenmesi).
     */
    public boolean isStationary(List<CocukKonumEntity> locations, double thresholdM) {
        if (locations.size() < 2) return true;
        double avgLat = locations.stream().mapToDouble(CocukKonumEntity::getLat).average().orElse(0);
        double avgLng = locations.stream().mapToDouble(CocukKonumEntity::getLng).average().orElse(0);
        double maxDist = locations.stream()
                .mapToDouble(l -> haversine(avgLat, avgLng, l.getLat(), l.getLng()))
                .max().orElse(0);
        return maxDist < thresholdM;
    }

    /**
     * Haversine formula: iki GPS koordinatı arasındaki mesafeyi metre cinsinden döner.
     */
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
