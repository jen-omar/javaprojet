package tn.esprit.mythoria.utils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class AiImageUtil {
    private static final String BASE_URL = "https://gen.pollinations.ai/image/";
    private static final String API_KEY = System.getenv().getOrDefault("POLLINATIONS_API_KEY", "");
    private static final Path DEFAULT_OUTPUT_DIRECTORY = Paths.get("generated-images", "events");
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private AiImageUtil() {
    }

    public static String generateEventImage(String prompt, int eventId) throws IOException, InterruptedException {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Le prompt image ne doit pas etre vide.");
        }

        Files.createDirectories(DEFAULT_OUTPUT_DIRECTORY);
        String fileName = "event-" + Math.max(eventId, 0) + "-" + LocalDateTime.now().format(FILE_TIMESTAMP) + ".jpg";
        Path outputFile = DEFAULT_OUTPUT_DIRECTORY.resolve(fileName).toAbsolutePath().normalize();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(buildImageUri(prompt))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "image/*")
                .GET();

        String apiKey = normalizedApiKey();
        if (!apiKey.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }

        HttpResponse<byte[]> response = HTTP_CLIENT.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(buildHttpErrorMessage(response.statusCode()));
        }

        String contentType = response.headers().firstValue("content-type").orElse("");
        if (!contentType.toLowerCase().startsWith("image/")) {
            throw new IOException("Le service IA n'a pas retourne une image.");
        }

        Files.write(outputFile, response.body());
        return outputFile.toString();
    }

    private static URI buildImageUri(String prompt) {
        String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8).replace("+", "%20");
        StringBuilder url = new StringBuilder(BASE_URL)
                .append(encodedPrompt)
                .append("?width=1024&height=768&model=flux&nologo=true&enhance=true");

        String apiKey = normalizedApiKey();
        if (!apiKey.isBlank()) {
            url.append("&key=").append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
        }

        return URI.create(url.toString());
    }

    private static String buildHttpErrorMessage(int statusCode) {
        if (statusCode == 401 && normalizedApiKey().isBlank()) {
            return "Image AI request failed: HTTP 401. La cle Pollinations est vide.";
        }
        if (statusCode == 401) {
            return "Image AI request failed: HTTP 401. La cle Pollinations est refusee. Verifiez la cle et son modele autorise.";
        }
        return "Image AI request failed: HTTP " + statusCode;
    }

    private static String normalizedApiKey() {
        return stripWrappingQuotes(API_KEY.trim());
    }

    private static String stripWrappingQuotes(String value) {
        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1).trim();
        }
        return value;
    }
}
