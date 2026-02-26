package com.safekid.geofence.util;

import java.util.List;

/**
 * Polygon içinde nokta kontrolü — Ray Casting (Crossing Number) algoritması.
 *
 * GeoJSON formatı: [[lng, lat], ...]
 */
public final class PointInPolygon {

    private PointInPolygon() {}

    /**
     * @param lat         noktanın latitude değeri
     * @param lng         noktanın longitude değeri
     * @param coordinates GeoJSON dış halka [[lng, lat], ...]
     * @return true → içeride veya sınır üzerinde
     */
    public static boolean isInside(double lat, double lng, List<List<Double>> coordinates) {

        int n = coordinates.size();
        if (n < 3) return false;

        // 🔥 EDGE CHECK — sınırdaysa içeride kabul et
        if (isPointOnEdge(lat, lng, coordinates)) {
            return true;
        }

        boolean inside = false;
        int j = n - 1;

        for (int i = 0; i < n; i++) {

            double lngI = coordinates.get(i).get(0);
            double latI = coordinates.get(i).get(1);
            double lngJ = coordinates.get(j).get(0);
            double latJ = coordinates.get(j).get(1);

            boolean latCrossing = (latI > lat) != (latJ > lat);

            if (latCrossing) {
                double intersectLng =
                        (lngJ - lngI) * (lat - latI) / (latJ - latI) + lngI;

                // 🔥 <= fix (edge bug fix)
                if (lng <= intersectLng) {
                    inside = !inside;
                }
            }

            j = i;
        }

        return inside;
    }

    /**
     * Nokta polygon kenarının üstünde mi kontrol eder.
     */
    private static boolean isPointOnEdge(double lat, double lng,
                                         List<List<Double>> poly) {

        final double EPS = 1e-6;

        for (int i = 0; i < poly.size() - 1; i++) {

            double x1 = poly.get(i).get(0);
            double y1 = poly.get(i).get(1);
            double x2 = poly.get(i + 1).get(0);
            double y2 = poly.get(i + 1).get(1);

            if (pointOnSegment(lng, lat, x1, y1, x2, y2, EPS)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Nokta bir doğru parçası üzerinde mi?
     */
    private static boolean pointOnSegment(
            double px, double py,
            double x1, double y1,
            double x2, double y2,
            double eps) {

        double cross =
                (px - x1) * (y2 - y1) -
                        (py - y1) * (x2 - x1);

        if (Math.abs(cross) > eps) return false;

        double dot =
                (px - x1) * (px - x2) +
                        (py - y1) * (py - y2);

        return dot <= eps;
    }
}