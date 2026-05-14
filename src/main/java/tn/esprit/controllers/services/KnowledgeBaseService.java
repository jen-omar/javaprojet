package tn.esprit.controllers.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

public final class KnowledgeBaseService {
    private static final String KNOWLEDGE_PATH = "knowledge/mythoria_knowledge.txt";

    private final String knowledgeBase;

    public KnowledgeBaseService() {
        this.knowledgeBase = loadKnowledgeBase();
    }

    public String findRelevantKnowledge(String userMessage) {
        String query = Optional.ofNullable(userMessage).orElse("").toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();

        for (String block : knowledgeBase.split("\\n\\n+")) {
            String normalizedBlock = block.toLowerCase(Locale.ROOT);
            if (matchesAny(query, normalizedBlock)) {
                if (!result.isEmpty()) {
                    result.append("\n\n");
                }
                result.append(block.trim());
            }
        }

        if (result.isEmpty()) {
            return firstBlocks(3);
        }
        return result.toString();
    }

    public String summary() {
        return firstBlocks(2);
    }

    private boolean matchesAny(String query, String block) {
        for (String token : query.split("[^\\p{IsAlphabetic}\\p{IsDigit}]+")) {
            if (token.length() >= 4 && block.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String firstBlocks(int count) {
        String[] blocks = knowledgeBase.split("\\n\\n+");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < blocks.length && i < count; i++) {
            if (!builder.isEmpty()) {
                builder.append("\n\n");
            }
            builder.append(blocks[i].trim());
        }
        return builder.toString();
    }

    private static String loadKnowledgeBase() {
        try (InputStream stream = KnowledgeBaseService.class.getClassLoader().getResourceAsStream(KNOWLEDGE_PATH)) {
            if (stream == null) {
                return "MYTHORIA is a fantasy desktop application for books, worlds, lores, wallet, identity verification, and user management.";
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "MYTHORIA knowledge base is temporarily unavailable.";
        }
    }
}
