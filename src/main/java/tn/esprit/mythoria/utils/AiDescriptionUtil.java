package tn.esprit.mythoria.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class AiDescriptionUtil {
    private static final String DEFAULT_ENDPOINT = "http://localhost:11434/api/generate";
    private static final String DEFAULT_MODEL = "llama3.2";
    private static final String ENDPOINT_KEY = "MYTHORIA_AI_ENDPOINT";
    private static final String MODEL_KEY = "MYTHORIA_AI_MODEL";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(45);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AiDescriptionUtil() {
    }

    public static String improveDescription(String description, String context) throws IOException, InterruptedException {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Veuillez saisir une description avant de lancer l'IA.");
        }

        return cleanResponse(generateText(buildDescriptionPrompt(description.trim(), context)));
    }

    public static LocalDraft generateLocalDraft(String localPrompt) throws IOException, InterruptedException {
        if (localPrompt == null || localPrompt.isBlank()) {
            throw new IllegalArgumentException("Veuillez decrire le local avant de lancer l'IA.");
        }

        String response = generateText(buildLocalDraftPrompt(localPrompt.trim()));
        JsonNode root = OBJECT_MAPPER.readTree(extractJsonObject(response));

        return new LocalDraft(
                textValue(root, "name"),
                textValue(root, "description"),
                doubleValue(root, "price"),
                textValue(root, "address"),
                intValue(root, "capacity"),
                textValue(root, "image"),
                normalizeStatus(textValue(root, "status"))
        );
    }

    private static String generateText(String prompt) throws IOException, InterruptedException {
        ObjectNode payload = OBJECT_MAPPER.createObjectNode();
        payload.put("model", readConfig(MODEL_KEY, DEFAULT_MODEL));
        payload.put("prompt", prompt);
        payload.put("stream", false);

        HttpRequest request = HttpRequest.newBuilder(URI.create(readConfig(ENDPOINT_KEY, DEFAULT_ENDPOINT)))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Ollama request failed: HTTP " + response.statusCode());
        }

        JsonNode root = OBJECT_MAPPER.readTree(response.body());
        String improvedDescription = root.path("response").asText("").trim();
        if (improvedDescription.isBlank()) {
            throw new IOException("Le modele IA n'a pas retourne de description.");
        }

        return improvedDescription;
    }

    private static String buildDescriptionPrompt(String description, String context) {
        String safeContext = context == null || context.isBlank() ? "description" : context.trim();
        return """
                Tu es un assistant de redaction pour une application de gestion d'evenements et de locaux.
                Reecris la description suivante en francais professionnel, clair et attractif.
                Garde le sens original, n'invente pas de details importants, et retourne seulement la description finale.
                Type: %s
                Description brute: %s
                """.formatted(safeContext, description);
    }

    private static String buildLocalDraftPrompt(String localPrompt) {
        return """
                Tu es un assistant de saisie pour un formulaire de local.
                A partir de la demande admin, retourne uniquement un objet JSON valide, sans markdown et sans texte autour.
                Champs obligatoires du JSON:
                {
                  "name": "nom court du local",
                  "description": "description francaise professionnelle et claire",
                  "price": nombre en dinars tunisiens,
                  "address": "adresse ou ville",
                  "capacity": nombre entier de places,
                  "image": "",
                  "status": "DISPONIBLE"
                }
                Regles:
                - status doit etre exactement DISPONIBLE, INDISPONIBLE ou EN_MAINTENANCE.
                - si une valeur est inconnue, propose une valeur raisonnable sauf pour image qui reste vide.
                - la description doit rester realiste et ne pas inventer de details trop specifiques.
                Demande admin: %s
                """.formatted(localPrompt);
    }

    private static String cleanResponse(String value) {
        return value
                .replaceAll("(?i)^description finale\\s*:\\s*", "")
                .replaceAll("^\"|\"$", "")
                .trim();
    }

    private static String extractJsonObject(String value) throws IOException {
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IOException("Le modele IA n'a pas retourne un JSON valide.");
        }
        return value.substring(start, end + 1);
    }

    private static String textValue(JsonNode root, String fieldName) {
        JsonNode value = root.path(fieldName);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("").trim();
    }

    private static Double doubleValue(JsonNode root, String fieldName) {
        JsonNode value = root.path(fieldName);
        if (value.isNumber()) {
            return value.asDouble();
        }
        String text = value.asText("").replace(',', '.').trim();
        if (text.isEmpty()) {
            return null;
        }

        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer intValue(JsonNode root, String fieldName) {
        JsonNode value = root.path(fieldName);
        if (value.isInt() || value.isLong()) {
            return value.asInt();
        }

        String text = value.asText("").replaceAll("[^0-9]", "").trim();
        if (text.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "DISPONIBLE";
        }

        String normalized = status.trim().toUpperCase();
        return switch (normalized) {
            case "DISPONIBLE", "INDISPONIBLE", "EN_MAINTENANCE" -> normalized;
            default -> "DISPONIBLE";
        };
    }

    private static String readConfig(String key, String defaultValue) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }

        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        return defaultValue;
    }

    public record LocalDraft(
            String name,
            String description,
            Double price,
            String address,
            Integer capacity,
            String image,
            String status
    ) {
    }
}
