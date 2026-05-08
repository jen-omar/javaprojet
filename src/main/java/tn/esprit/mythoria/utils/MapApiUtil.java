package tn.esprit.mythoria.utils;

import tn.esprit.mythoria.entity.Local;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MapApiUtil {
    private static final Map<String, Coordinate> GEOCODE_CACHE = new ConcurrentHashMap<>();

    private MapApiUtil() {
    }

    public static String buildLocalsMapHtml(List<Local> locals) throws IOException, InterruptedException {
        List<MapPoint> points = geocodeLocals(locals);
        return buildHtml(points);
    }

    public static List<MapPoint> geocodeLocals(List<Local> locals) throws IOException, InterruptedException {
        List<MapPoint> points = new ArrayList<>();
        if (locals == null || locals.isEmpty()) {
            return points;
        }

        for (Local local : locals) {
            try {
                MapPoint point = geocodeLocal(local);
                if (point != null) {
                    points.add(point);
                }
            } catch (IOException ignored) {
                // Keep the map usable even if one address cannot be resolved.
            }
        }
        return points;
    }

    private static MapPoint geocodeLocal(Local local) throws IOException, InterruptedException {
        if (local == null) {
            return null;
        }

        String query = buildSearchQuery(local);
        if (query.isBlank()) {
            return null;
        }

        Coordinate coordinate = GEOCODE_CACHE.get(query.toLowerCase(Locale.ROOT));
        if (coordinate == null) {
            coordinate = geocodeWithOpenMeteo(query);
            if (coordinate != null) {
                GEOCODE_CACHE.put(query.toLowerCase(Locale.ROOT), coordinate);
            }
        }

        return coordinate == null ? null : buildMapPoint(local, offsetCoordinate(coordinate, local.getId()));
    }

    private static Coordinate geocodeWithOpenMeteo(String query) throws IOException, InterruptedException {
        MeteoApiUtil.GeoLocation location = MeteoApiUtil.findLocationCoordinates(query);
        return new Coordinate(location.latitude(), location.longitude());
    }

    private static MapPoint buildMapPoint(Local local, Coordinate coordinate) {
        return new MapPoint(
                local.getId(),
                fallback(local.getName(), "Local sans nom"),
                fallback(local.getAddress(), "Adresse non precisee"),
                fallback(local.getDescription(), "Aucune description."),
                local.getPrice(),
                local.getCapacity(),
                fallback(local.getStatus(), "Statut N/A"),
                coordinate.latitude(),
                coordinate.longitude()
        );
    }

    private static Coordinate offsetCoordinate(Coordinate coordinate, int localId) {
        double offset = ((localId % 7) - 3) * 0.0025;
        return new Coordinate(coordinate.latitude() + offset, coordinate.longitude() + offset);
    }

    private static String buildHtml(List<MapPoint> points) {
        double centerLat = points.isEmpty()
                ? 34.0
                : points.stream().mapToDouble(MapPoint::latitude).average().orElse(34.0);
        double centerLon = points.isEmpty()
                ? 9.0
                : points.stream().mapToDouble(MapPoint::longitude).average().orElse(9.0);
        int zoom = points.isEmpty() ? 6 : 7;

        StringBuilder markers = new StringBuilder();
        for (MapPoint point : points) {
            markers.append("addMarker(")
                    .append(point.latitude()).append(',')
                    .append(point.longitude()).append(',')
                    .append(toJsString(point.name())).append(',')
                    .append(toJsString(buildPopupHtml(point)))
                    .append(");\n");
        }

        return """
                <!doctype html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
                    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                    <style>
                        html, body { width: 100%%; height: 100%%; margin: 0; overflow: hidden; }
                        #map { position: absolute; inset: 0; width: 100%%; height: 100%%; }
                        body { font-family: Arial, sans-serif; background: #151518; }
                        .popup-title { font-weight: 700; font-size: 15px; margin-bottom: 6px; }
                        .popup-line { margin: 3px 0; }
                        .empty { color: #e8e8e8; padding: 18px; }
                    </style>
                </head>
                <body>
                    <div id="map"></div>
                    <script>
                        L.Browser.any3d = false;
                        L.Browser.webkit3d = false;
                        const map = L.map('map', {
                            zoomAnimation: false,
                            fadeAnimation: false,
                            markerZoomAnimation: false,
                            inertia: false,
                            preferCanvas: true
                        }).setView([%s, %s], %d);
                        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                            maxZoom: 19,
                            keepBuffer: 6,
                            updateWhenIdle: false,
                            updateWhenZooming: false,
                            detectRetina: false,
                            crossOrigin: true,
                            attribution: '&copy; OpenStreetMap contributors'
                        }).addTo(map);
                        const bounds = [];
                        function addMarker(lat, lon, title, popupHtml) {
                            const marker = L.marker([lat, lon]).addTo(map);
                            marker.bindPopup(popupHtml);
                            marker.bindTooltip(title);
                            bounds.push([lat, lon]);
                        }
                        %s
                        if (bounds.length > 1) {
                            map.fitBounds(bounds, { padding: [42, 42] });
                        }
                        if (bounds.length === 0) {
                            document.body.insertAdjacentHTML('beforeend',
                                '<div class="empty">Aucun local geocode. Verifiez les adresses des locaux.</div>');
                        }
                        window.refreshMapSize = function() {
                            map.invalidateSize(true);
                            if (bounds.length > 1) {
                                map.fitBounds(bounds, { padding: [42, 42] });
                            }
                        };
                        setTimeout(window.refreshMapSize, 250);
                        setTimeout(window.refreshMapSize, 800);
                    </script>
                </body>
                </html>
                """.formatted(formatDouble(centerLat), formatDouble(centerLon), zoom, markers);
    }

    private static String buildPopupHtml(MapPoint point) {
        return "<div class=\"popup-title\">" + escapeHtml(point.name()) + "</div>"
                + "<div class=\"popup-line\"><b>Adresse:</b> " + escapeHtml(point.address()) + "</div>"
                + "<div class=\"popup-line\"><b>Capacite:</b> " + point.capacity() + " places</div>"
                + "<div class=\"popup-line\"><b>Prix:</b> " + point.price() + " DT</div>"
                + "<div class=\"popup-line\"><b>Statut:</b> " + escapeHtml(point.status()) + "</div>"
                + "<div class=\"popup-line\"><b>Description:</b> " + escapeHtml(limitText(point.description(), 150)) + "</div>";
    }

    private static String buildSearchQuery(Local local) {
        String address = local.getAddress();
        if (address != null && !address.isBlank()) {
            return address.trim();
        }
        return fallback(local.getName(), "").trim();
    }

    private static String toJsString(String value) {
        String escaped = fallback(value, "")
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "")
                .replace("\n", "\\n");
        return "'" + escaped + "'";
    }

    private static String escapeHtml(String value) {
        return fallback(value, "")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String limitText(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "Aucune description.";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private static String formatDouble(double value) {
        return String.format(Locale.US, "%.6f", value);
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record MapPoint(
            int localId,
            String name,
            String address,
            String description,
            double price,
            int capacity,
            String status,
            double latitude,
            double longitude
    ) {
    }

    private record Coordinate(double latitude, double longitude) {
    }
}
