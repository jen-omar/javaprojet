package com.marketplace.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class ChatbotService {

    public String[] askChatbot(String message, String context, String userName) {
        try {
            // Find the script regardless of current working directory
            String scriptPath = new File("ChatBotAI/predict_assistant.py").getAbsolutePath();
            if (!new File(scriptPath).exists()) {
                // Fallback for execution path issues
                scriptPath = new File("../ChatBotAI/predict_assistant.py").getAbsolutePath();
            }

            ProcessBuilder pb = new ProcessBuilder("python", scriptPath, message, context, userName);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            p.waitFor();
            
            String json = output.toString();
            
            // extract error if success=false
            if (json.contains("\"success\": false")) {
                 if (json.contains("\"error\":")) {
                     int start = json.indexOf("\"error\":") + 8;
                     int strStart = json.indexOf("\"", start) + 1;
                     int strEnd = json.indexOf("\"", strStart);
                     return new String[] {"Erreur: " + json.substring(strStart, strEnd), "error"};
                 }
                 return new String[] {"Erreur inconnue du chatbot.", "error"};
            }

            String parsedIntent = null;
            if (json.contains("\"intent\":")) {
                int start = json.indexOf("\"intent\":") + 9;
                int strStart = json.indexOf("\"", start) + 1;
                int strEnd = json.indexOf("\"", strStart);
                parsedIntent = json.substring(strStart, strEnd);
            }

            // Extract response
            if (json.contains("\"response\":")) {
                int start = json.indexOf("\"response\":") + 11;
                int strStart = json.indexOf("\"", start) + 1;
                // Handle possible escaped quotes inside response
                int strEnd = strStart;
                while (strEnd < json.length()) {
                    if (json.charAt(strEnd) == '"' && json.charAt(strEnd - 1) != '\\') {
                        break;
                    }
                    strEnd++;
                }
                String resp = json.substring(strStart, strEnd);
                resp = unescapeJavaString(resp);
                resp = resp.replaceAll("(?i)<br\\s*/?>", "\n");
                resp = resp.replaceAll("<[^>]*>", "");
                resp = resp.replace("&nbsp;", " ");
                return new String[] {resp.trim(), parsedIntent};
            } 
            // Extract intent for dynamic fallback
            else if (parsedIntent != null) {
                return new String[] {getDynamicResponseForIntent(parsedIntent), parsedIntent};
            }
            
            return new String[] {"Je n'ai pas compris la réponse du bot.", "unknown"};

        } catch (Exception e) {
            e.printStackTrace();
            return new String[] {"Erreur de connexion au chatbot : " + e.getMessage(), "error"};
        }
    }

    private String getDynamicResponseForIntent(String intent) {
        switch (intent) {
            case "search_paintings": return "Je vous montre les peintures...";
            case "search_sculptures": return "Je vous montre les sculptures...";
            case "search_digital": return "Voici l'art numérique disponible...";
            case "search_photography": return "Voici nos photographies...";
            case "search_newest": return "Voici les nouveautés !";
            case "view_auctions": return "Je vous emmène voir les enchères...";
            case "order_history": return "Vous pouvez consulter vos commandes dans l'onglet 'Commandes'.";
            case "wishlist_view": return "Vos favoris sont dans l'onglet 'Liste de souhaits'.";
            case "cart_view": return "Vous pouvez finaliser vos achats dans votre panier.";
            default: return "Je peux vous aider à naviguer vers cette section !";
        }
    }

    private String unescapeJavaString(String st) {
        if (st == null) return null;
        StringBuilder sb = new StringBuilder(st.length());
        for (int i = 0; i < st.length(); i++) {
            char ch = st.charAt(i);
            if (ch == '\\' && i + 1 < st.length()) {
                char nextChar = st.charAt(i + 1);
                if (nextChar == 'u' && i + 5 < st.length()) {
                    String hex = st.substring(i + 2, i + 6);
                    try {
                        sb.append((char) Integer.parseInt(hex, 16));
                        i += 5;
                        continue;
                    } catch (NumberFormatException e) {
                        // ignore and fall back
                    }
                } else if (nextChar == 'n') {
                    sb.append('\n');
                    i++;
                    continue;
                } else if (nextChar == '"') {
                    sb.append('"');
                    i++;
                    continue;
                } else if (nextChar == '\\') {
                    sb.append('\\');
                    i++;
                    continue;
                }
            }
            sb.append(ch);
        }
        return sb.toString();
    }
}
