package tn.esprit.services;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import tn.esprit.Models.Message;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AiBridge — Connects the Java app to Ollama (http://localhost:11434).
 * <p>
 * This class mirrors the logic in chat_ai/core/ai_engine.py and
 * chat_ai/core/ollama_client.py, communicating directly with the
 * Ollama REST API from Java — no Python subprocess required at runtime.
 * <p>
 * A fallback method using ProcessBuilder + bridge.py is also available.
 */
public class AiBridge {

    // ── Ollama Configuration ─────────────────────────────────────
    private static final String OLLAMA_BASE_URL = "http://localhost:11434";
    private static final String OLLAMA_MODEL    = "llama3:latest";
    private static final int    TIMEOUT_MS      = 120_000;
    private static final int    NUM_SUGGESTIONS = 3;

    private static final Gson GSON = new Gson();

    // ══════════════════════════════════════════════════════════════
    //  PUBLIC API — Async wrappers (always run off the FX thread)
    // ══════════════════════════════════════════════════════════════

    /**
     * Generate a concise summary of the conversation.
     */
    public static CompletableFuture<String> summarizeAsync(List<Message> messages) {
        return CompletableFuture.supplyAsync(() -> summarize(messages));
    }

    /**
     * Generate 3 contextual reply suggestions.
     */
    public static CompletableFuture<List<String>> suggestAsync(List<Message> messages) {
        return CompletableFuture.supplyAsync(() -> suggestReplies(messages));
    }

    /**
     * Generate a full auto-reply based on the given persona.
     */
    public static CompletableFuture<String> autoReplyAsync(List<Message> messages, String persona) {
        return CompletableFuture.supplyAsync(() -> autoReply(messages, persona));
    }

    // ══════════════════════════════════════════════════════════════
    //  CORE METHODS (blocking — run on background threads)
    // ══════════════════════════════════════════════════════════════

    /**
     * 1. CHAT SUMMARY — mirrors ai_engine.py → summarize()
     */
    public static String summarize(List<Message> messages) {
        String formatted = formatConversation(messages);

        String system = "You are an expert conversation analyst. "
                      + "You produce clear, concise, professional summaries.";

        String prompt = "Analyze the following conversation and provide:\n"
                      + "1. A 3-sentence executive summary\n"
                      + "2. Key topics discussed (bullet points)\n"
                      + "3. Any decisions or action items mentioned\n\n"
                      + "CONVERSATION:\n" + formatted + "\n\n"
                      + "Respond in a structured, professional format. Do NOT use markdown symbols like ** or *. Provide plain text only.";

        return ollamaGenerate(prompt, system);
    }

    /**
     * 2. SMART REPLY SUGGESTIONS — mirrors ai_engine.py → suggest_replies()
     */
    public static List<String> suggestReplies(List<Message> messages) {
        String formatted = formatConversation(messages);
        String lastMsg   = messages.isEmpty() ? "" : messages.get(messages.size() - 1).getContent();

        String system = "You are a smart messaging assistant that suggests "
                      + "natural, context-aware replies.";

        String prompt = "Given this conversation, suggest exactly " + NUM_SUGGESTIONS
                      + " different reply options for the last message.\n\n"
                      + "CONVERSATION:\n" + formatted + "\n\n"
                      + "LAST MESSAGE: " + lastMsg + "\n\n"
                      + "Rules:\n"
                      + "- Each reply must be distinct in tone (e.g. formal, casual, brief)\n"
                      + "- Each reply must be on its own line\n"
                      + "- Prefix each with its number: 1. 2. 3.\n"
                      + "- No extra explanation, just the " + NUM_SUGGESTIONS + " replies.";

        String raw = ollamaGenerate(prompt, system);
        return parseNumberedList(raw, NUM_SUGGESTIONS);
    }

    /**
     * 3. AUTO-REPLY — mirrors ai_engine.py → auto_reply()
     */
    public static String autoReply(List<Message> messages, String persona) {
        String formatted = formatConversation(messages);
        Message lastMsg  = messages.isEmpty() ? null : messages.get(messages.size() - 1);

        String senderName = (lastMsg != null && lastMsg.getSenderUsername() != null)
                          ? lastMsg.getSenderUsername() : "User";
        String lastText   = (lastMsg != null) ? lastMsg.getContent() : "";

        String system = "You are an intelligent auto-reply bot acting as: " + persona + ". "
                      + "You write helpful, natural, context-aware replies.";

        String prompt = "CONVERSATION HISTORY:\n" + formatted + "\n\n"
                      + "LAST MESSAGE FROM " + senderName + ": " + lastText + "\n\n"
                      + "Write ONE professional and natural auto-reply. "
                      + "Do NOT add any explanation or prefix — just the reply message.";

        return ollamaGenerate(prompt, system);
    }

    // ══════════════════════════════════════════════════════════════
    //  OLLAMA HTTP CLIENT — mirrors ollama_client.py → generate()
    // ══════════════════════════════════════════════════════════════

    /**
     * Check whether the Ollama server is reachable.
     */
    public static boolean isOllamaAvailable() {
        try {
            URL url = URI.create(OLLAMA_BASE_URL + "/api/tags").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Send a prompt to Ollama and return the raw response text.
     */
    private static String ollamaGenerate(String prompt, String system) {
        try {
            URL url = URI.create(OLLAMA_BASE_URL + "/api/generate").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            // Build request body
            JsonObject body = new JsonObject();
            body.addProperty("model",  OLLAMA_MODEL);
            body.addProperty("prompt", prompt);
            body.addProperty("stream", false);
            if (system != null && !system.isEmpty()) {
                body.addProperty("system", system);
            }

            // Write body
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            // Read response
            int status = conn.getResponseCode();
            if (status != 200) {
                String errBody = readStream(conn.getErrorStream());
                throw new RuntimeException("Ollama returned HTTP " + status + ": " + errBody);
            }

            String responseBody = readStream(conn.getInputStream());
            conn.disconnect();

            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            return json.has("response") ? json.get("response").getAsString().trim() : "";

        } catch (java.net.ConnectException e) {
            throw new RuntimeException("Cannot reach Ollama. Make sure it is running: ollama serve", e);
        } catch (java.net.SocketTimeoutException e) {
            throw new RuntimeException("Ollama timed out after " + (TIMEOUT_MS / 1000) + "s. Try a faster model.", e);
        } catch (Exception e) {
            throw new RuntimeException("Ollama error: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  FALLBACK — ProcessBuilder bridge (calls chat_ai/bridge.py)
    // ══════════════════════════════════════════════════════════════

    /**
     * Fallback method: calls the Python bridge.py via ProcessBuilder.
     * Use this if you prefer the Python AI engine over the pure-Java approach.
     *
     * @param command  "summary", "suggest", or "autoreply"
     * @param messages list of Message objects to analyse
     * @param persona  persona description (only for autoreply)
     * @return raw JSON result string
     */
    public static String callPythonBridge(String command, List<Message> messages, String persona) {
        try {
            // Build the JSON request
            JsonObject request = new JsonObject();
            request.addProperty("command", command);

            JsonArray msgArray = new JsonArray();
            for (Message m : messages) {
                JsonObject obj = new JsonObject();
                obj.addProperty("sender", m.getSenderUsername() != null ? m.getSenderUsername() : "User");
                obj.addProperty("text", m.getContent());
                msgArray.add(obj);
            }
            request.add("messages", msgArray);
            if (persona != null) {
                request.addProperty("persona", persona);
            }

            // Locate the bridge script relative to the project root
            String projectRoot = System.getProperty("user.dir");
            String bridgePath  = projectRoot + File.separator + "chat_ai" + File.separator + "bridge.py";

            ProcessBuilder pb = new ProcessBuilder("python", bridgePath);
            pb.redirectErrorStream(false);
            pb.directory(new File(projectRoot + File.separator + "chat_ai"));
            Process process = pb.start();

            // Write JSON to stdin
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(request.toString().getBytes(StandardCharsets.UTF_8));
                stdin.flush();
            }

            // Read stdout
            String stdout = readStream(process.getInputStream());
            // Read stderr (for debugging)
            String stderr = readStream(process.getErrorStream());

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("⚠️ Python bridge stderr: " + stderr);
            }

            return stdout;

        } catch (Exception e) {
            return "{\"status\":\"error\",\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Format a list of Message objects into a conversation string
     * (mirrors ai_engine.py → _format_conversation)
     */
    private static String formatConversation(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (Message m : messages) {
            String sender = (m.getSenderUsername() != null) ? m.getSenderUsername() : "User";
            String text   = (m.getContent() != null) ? m.getContent() : "";
            
            // Add a prefix if this is a transcribed voice message so the AI knows it was spoken
            if (m.getAudioPath() != null && !m.getAudioPath().isEmpty()) {
                text = "(Voice Message Transcription): " + text;
            }
            
            String time   = (m.getCreatedAt() != null)
                          ? m.getCreatedAt().toLocalTime().toString() : "";
            if (!time.isEmpty()) {
                sb.append("[").append(time).append("] ");
            }
            sb.append(sender).append(": ").append(text).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Parse a numbered list from the AI response (e.g. "1. ...\n2. ...\n3. ...")
     * (mirrors ai_engine.py → _parse_numbered_list)
     */
    private static List<String> parseNumberedList(String text, int count) {
        List<String> results = new ArrayList<>();
        Pattern pattern = Pattern.compile("^\\d+[.)\\]]\\s*(.+)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            results.add(matcher.group(1).trim());
        }
        // Fallback: split by newline
        if (results.isEmpty()) {
            for (String line : text.split("\\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    results.add(trimmed);
                }
            }
        }
        return results.subList(0, Math.min(results.size(), count));
    }

    /**
     * Read an InputStream fully into a String.
     */
    private static String readStream(InputStream stream) {
        if (stream == null) return "";
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Extract the last N messages from a full list (for context window).
     */
    public static List<Message> lastN(List<Message> messages, int n) {
        if (messages.size() <= n) return messages;
        return messages.subList(messages.size() - n, messages.size());
    }
}
