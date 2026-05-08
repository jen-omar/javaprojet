package tn.esprit.mythoria.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;

public final class MeteoApiUtil {
    private static final String GEOCODING_ENDPOINT = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_ENDPOINT = "https://api.open-meteo.com/v1/forecast";
    private static final String ARCHIVE_ENDPOINT = "https://archive-api.open-meteo.com/v1/archive";
    private static final String DAILY_VARIABLES = "weather_code,temperature_2m_max,temperature_2m_min,"
            + "precipitation_sum,wind_speed_10m_max";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MeteoApiUtil() {
    }

    public static MeteoForecast getMeteo(String locationName, String date) throws IOException, InterruptedException {
        if (date == null || date.isBlank()) {
            throw new IllegalArgumentException("Date must not be empty. Use yyyy-MM-dd format.");
        }

        return getMeteo(locationName, LocalDate.parse(date.trim()));
    }

    public static MeteoForecast getMeteo(String locationName, LocalDate date) throws IOException, InterruptedException {
        if (locationName == null || locationName.isBlank()) {
            throw new IllegalArgumentException("Location name must not be empty.");
        }
        if (date == null) {
            throw new IllegalArgumentException("Date must not be null.");
        }

        Location location = findLocation(locationName.trim());
        JsonNode forecastRoot = getJson(buildWeatherUri(location, date));
        JsonNode daily = forecastRoot.path("daily");
        String source = date.isBefore(LocalDate.now()) ? "Open-Meteo Historical Weather API" : "Open-Meteo Forecast API";
        int weatherCode = firstInt(daily, "weather_code");

        return new MeteoForecast(
                locationName.trim(),
                location.name(),
                location.country(),
                location.latitude(),
                location.longitude(),
                date,
                weatherCode,
                describeWeatherCode(weatherCode),
                firstDouble(daily, "temperature_2m_max"),
                firstDouble(daily, "temperature_2m_min"),
                firstDouble(daily, "precipitation_sum"),
                firstDouble(daily, "wind_speed_10m_max"),
                unit(forecastRoot, "temperature_2m_max"),
                unit(forecastRoot, "precipitation_sum"),
                unit(forecastRoot, "wind_speed_10m_max"),
                source
        );
    }

    public static GeoLocation findLocationCoordinates(String locationName) throws IOException, InterruptedException {
        Location location = findLocation(locationName);
        return new GeoLocation(
                location.name(),
                location.country(),
                location.latitude(),
                location.longitude()
        );
    }

    public static String describeWeatherCode(int weatherCode) {
        return switch (weatherCode) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45 -> "Fog";
            case 48 -> "Depositing rime fog";
            case 51 -> "Light drizzle";
            case 53 -> "Moderate drizzle";
            case 55 -> "Dense drizzle";
            case 56 -> "Light freezing drizzle";
            case 57 -> "Dense freezing drizzle";
            case 61 -> "Slight rain";
            case 63 -> "Moderate rain";
            case 65 -> "Heavy rain";
            case 66 -> "Light freezing rain";
            case 67 -> "Heavy freezing rain";
            case 71 -> "Slight snow fall";
            case 73 -> "Moderate snow fall";
            case 75 -> "Heavy snow fall";
            case 77 -> "Snow grains";
            case 80 -> "Slight rain showers";
            case 81 -> "Moderate rain showers";
            case 82 -> "Violent rain showers";
            case 85 -> "Slight snow showers";
            case 86 -> "Heavy snow showers";
            case 95 -> "Thunderstorm";
            case 96 -> "Thunderstorm with slight hail";
            case 99 -> "Thunderstorm with heavy hail";
            default -> "Unknown weather code";
        };
    }

    private static Location findLocation(String locationName) throws IOException, InterruptedException {
        String query = "name=" + encode(locationName)
                + "&count=1&language=en&format=json";
        JsonNode root = getJson(URI.create(GEOCODING_ENDPOINT + "?" + query));
        JsonNode results = root.path("results");

        if (!results.isArray() || results.isEmpty()) {
            throw new IOException("No location found for: " + locationName);
        }

        JsonNode firstResult = results.get(0);
        return new Location(
                firstResult.path("name").asText(locationName),
                firstResult.path("country").asText(""),
                firstResult.path("latitude").asDouble(),
                firstResult.path("longitude").asDouble()
        );
    }

    private static URI buildWeatherUri(Location location, LocalDate date) {
        String endpoint = date.isBefore(LocalDate.now()) ? ARCHIVE_ENDPOINT : FORECAST_ENDPOINT;
        String query = "latitude=" + location.latitude()
                + "&longitude=" + location.longitude()
                + "&start_date=" + date
                + "&end_date=" + date
                + "&daily=" + DAILY_VARIABLES
                + "&timezone=auto"
                + "&temperature_unit=celsius"
                + "&wind_speed_unit=kmh"
                + "&precipitation_unit=mm";

        return URI.create(endpoint + "?" + query);
    }

    private static JsonNode getJson(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonNode root = OBJECT_MAPPER.readTree(response.body());
        if (response.statusCode() < 200 || response.statusCode() >= 300 || root.path("error").asBoolean(false)) {
            String reason = root.path("reason").asText("HTTP " + response.statusCode());
            throw new IOException("Open-Meteo request failed: " + reason);
        }

        return root;
    }

    private static double firstDouble(JsonNode parent, String fieldName) throws IOException {
        JsonNode value = firstValue(parent, fieldName);
        if (!value.isNumber()) {
            throw new IOException("Missing numeric weather value: " + fieldName);
        }
        return value.asDouble();
    }

    private static int firstInt(JsonNode parent, String fieldName) throws IOException {
        JsonNode value = firstValue(parent, fieldName);
        if (!value.isNumber()) {
            throw new IOException("Missing numeric weather value: " + fieldName);
        }
        return value.asInt();
    }

    private static JsonNode firstValue(JsonNode parent, String fieldName) throws IOException {
        JsonNode values = parent.path(fieldName);
        if (!values.isArray() || values.isEmpty() || values.get(0).isNull()) {
            throw new IOException("Missing weather value: " + fieldName);
        }
        return values.get(0);
    }

    private static String unit(JsonNode root, String fieldName) {
        return root.path("daily_units").path(fieldName).asText("");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record MeteoForecast(
            String requestedLocation,
            String resolvedLocation,
            String country,
            double latitude,
            double longitude,
            LocalDate date,
            int weatherCode,
            String weatherDescription,
            double temperatureMax,
            double temperatureMin,
            double precipitationSum,
            double windSpeedMax,
            String temperatureUnit,
            String precipitationUnit,
            String windSpeedUnit,
            String source
    ) {
    }

    public record GeoLocation(String name, String country, double latitude, double longitude) {
    }

    private record Location(String name, String country, double latitude, double longitude) {
    }
}
