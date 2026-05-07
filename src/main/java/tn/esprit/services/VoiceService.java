package tn.esprit.services;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;

public class VoiceService {
    // Note: Free AssemblyAI API Key placeholder
    // In a real scenario, this should be injected from environment variables.
    private static final String API_KEY = "b62b7fed646549cf918a0dc0f8e9656e"; // Insert your actual free tier API key here
    private static final String UPLOAD_URL = "https://api.assemblyai.com/v2/upload";
    private static final String TRANSCRIPT_URL = "https://api.assemblyai.com/v2/transcript";

    public static String transcribeAudio(String filePath) {
        try {
            File audioFile = new File(filePath);
            if (!audioFile.exists()) return "[Transcription failed: File not found]";

            byte[] audioBytes = Files.readAllBytes(audioFile.toPath());
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

            // 1. Upload the audio file
            HttpRequest uploadRequest = HttpRequest.newBuilder()
                    .uri(URI.create(UPLOAD_URL))
                    .header("Authorization", API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(audioBytes))
                    .build();

            HttpResponse<String> uploadResponse = client.send(uploadRequest, HttpResponse.BodyHandlers.ofString());
            if (uploadResponse.statusCode() != 200 && uploadResponse.statusCode() != 201) {
                System.err.println("❌ AssemblyAI Upload Error: HTTP " + uploadResponse.statusCode() + " - " + uploadResponse.body());
                return "[Transcription failed: Authentication or Upload error]";
            }
            String uploadUrl = extractJsonValue(uploadResponse.body(), "upload_url");

            if (uploadUrl == null) return "[Transcription failed: Upload error]";

            // 2. Request transcription
            String transcriptJson = "{\"audio_url\": \"" + uploadUrl + "\", \"speech_models\": [\"universal-2\"], \"language_code\": \"en_us\"}";
            HttpRequest transcriptRequest = HttpRequest.newBuilder()
                    .uri(URI.create(TRANSCRIPT_URL))
                    .header("Authorization", API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(transcriptJson))
                    .build();

            HttpResponse<String> transcriptResponse = client.send(transcriptRequest, HttpResponse.BodyHandlers.ofString());
            if (transcriptResponse.statusCode() != 200 && transcriptResponse.statusCode() != 201) {
                System.err.println("❌ AssemblyAI Transcript Error: HTTP " + transcriptResponse.statusCode() + " - " + transcriptResponse.body());
                return "[Transcription failed: API Request error]";
            }
            String transcriptId = extractJsonValue(transcriptResponse.body(), "id");

            if (transcriptId == null) return "[Transcription failed: Request error]";

            // 3. Polling for the transcription result
            String pollingUrl = TRANSCRIPT_URL + "/" + transcriptId;
            while (true) {
                HttpRequest pollingRequest = HttpRequest.newBuilder()
                        .uri(URI.create(pollingUrl))
                        .header("Authorization", API_KEY)
                        .GET()
                        .build();

                HttpResponse<String> pollingResponse = client.send(pollingRequest, HttpResponse.BodyHandlers.ofString());
                String status = extractJsonValue(pollingResponse.body(), "status");
                System.out.println("⏳ AssemblyAI Polling Status: " + status);

                if ("completed".equals(status)) {
                    String text = extractJsonValue(pollingResponse.body(), "text");
                    System.out.println("✅ Transcription Success: " + text);
                    return text != null && !text.trim().isEmpty() ? text : "[Transcription was completely silent or empty]";
                } else if ("error".equals(status)) {
                    System.err.println("❌ AssemblyAI Processing Error: " + pollingResponse.body());
                    return "[Transcription failed: Processing error]";
                }

                Thread.sleep(3000); // Wait 3 seconds before polling again
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "[Transcription failed: " + e.getMessage() + "]";
        }
    }

    private static String extractJsonValue(String json, String key) {
        if (json == null || json.trim().isEmpty() || !json.trim().startsWith("{")) {
            return null;
        }
        try {
            com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            if (jsonObject.has(key) && !jsonObject.get(key).isJsonNull()) {
                return jsonObject.get(key).getAsString();
            }
        } catch (Exception e) {
            System.err.println("JSON Parse error: " + e.getMessage());
        }
        return null;
    }
}
