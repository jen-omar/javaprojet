package tn.esprit.controllers.services;

import tn.esprit.data.ChatHistoryDAO;
import tn.esprit.data.WalletRepository;
import tn.esprit.Models.User;
import tn.esprit.Models.Wallet;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class AiAssistantService {
    private static final int MEMORY_MESSAGE_LIMIT = 12;

    private final KnowledgeBaseService knowledgeBaseService = new KnowledgeBaseService();
    private final WalletRepository walletRepository = new WalletRepository();
    private final ChatHistoryDAO chatHistoryDAO = new ChatHistoryDAO();

    public String generateResponse(String userMessage, int userId) {
        return generateResponse(userMessage, userId, false);
    }

    public String generateResponse(String userMessage, User user) {
        int userId = parseUserId(user);
        boolean admin = user != null && user.isAdmin();
        return generateResponse(userMessage, userId, admin);
    }

    private String generateResponse(String userMessage, int userId, boolean admin) {
        String message = Optional.ofNullable(userMessage).orElse("").trim();
        if (message.isBlank()) {
            return "Tell me what you want to do in Mythoria, and I will help you with a clear next step.";
        }

        Language language = detectLanguage(message);
        ConversationMemory memory = loadMemory(userId, language);
        language = memory.preferredLanguage();
        String response;
        String lower = message.toLowerCase(Locale.ROOT);

        if (containsAny(lower, "hello", "hi", "hey", "bonjour", "salut", "bonsoir")) {
            response = greetingResponse(language, memory);
        } else if (containsAny(lower, "thank", "thanks", "merci")) {
            response = thanksResponse(language);
        } else if (containsAny(lower, "help", "aide", "what can you do", "que peux-tu faire", "que peut tu faire")) {
            response = helpResponse(language, memory);
        } else if (containsAny(lower, "continue", "more", "again", "suite", "encore", "continuez")) {
            response = continueResponse(language, memory, message);
        } else if (containsAny(lower, "wallet", "solde", "balance", "argent", "finance", "financial", "money", "portefeuille")) {
            response = walletResponse(userId, language);
        } else if (containsAny(lower, "role", "admin", "permission", "permissions", "autorisation")) {
            response = roleResponse(admin, language);
        } else if (containsAny(lower, "password", "mot de passe", "login", "connexion", "2fa", "otp", "identity", "identite", "verification")) {
            response = securityResponse(language, message);
        } else if (containsAny(lower, "world", "lore", "character", "kingdom", "story", "book", "fantasy", "monde", "royaume", "personnage", "histoire", "livre")) {
            response = writingResponse(message, language);
        } else if (containsAny(lower, "level", "rank", "score", "niveau", "points", "pc")) {
            response = levelResponse(language);
        } else if (containsAny(lower, "event", "ticket", "collaboration", "collab", "evenement")) {
            response = eventResponse(language);
        } else {
            response = featureResponse(message, language);
        }

        if (userId > 0) {
            chatHistoryDAO.save(userId, message, response);
        }
        return response;
    }

    private String greetingResponse(Language language, ConversationMemory memory) {
        if (memory.hasMemory()) {
            return language == Language.FR
                    ? "Salut, je me souviens que tu travailles souvent sur " + memory.topicLabel(language) + ". On peut continuer la-dessus, ou je peux t'aider avec le wallet, la securite, les roles ou ton profil."
                    : "Hi, I remember you have been focusing on " + memory.topicLabel(language) + ". We can continue there, or I can help with your wallet, security, roles, or profile.";
        }
        return language == Language.FR
                ? "Salut, je suis la pour t'aider dans Mythoria. Tu peux me parler de ton wallet, de la securite, des roles, de ton profil, ou me demander une idee de monde fantasy."
                : "Hi, I am here with you in Mythoria. You can ask about your wallet, security, roles, profile, or ask me to shape a fantasy world or story idea.";
    }

    private String thanksResponse(Language language) {
        return language == Language.FR
                ? "Avec plaisir. Donne-moi juste ton prochain objectif, et je te repondrai avec des etapes simples."
                : "You are welcome. Give me your next goal, and I will answer with simple, practical steps.";
    }

    private String helpResponse(Language language, ConversationMemory memory) {
        if (language == Language.FR) {
            String memoryLine = memory.hasMemory()
                    ? "\nJe remarque aussi que tes discussions recentes tournent surtout autour de " + memory.topicLabel(language) + ". Je peux reprendre ce fil si tu veux.\n"
                    : "";
            return """
                Je peux t'aider a:
                - comprendre les fonctions Mythoria;
                - verifier les infos generales de ton wallet;
                - expliquer login, OTP et verification d'identite;
                - clarifier les roles et permissions;
                - creer des idees de mondes, lores, personnages, livres et evenements.
                """ + memoryLine + """

                Dis-moi ce que tu veux faire maintenant, et je te guide.
                """;
        }

        String memoryLine = memory.hasMemory()
                ? "\nI also notice your recent chats mostly focus on " + memory.topicLabel(language) + ". I can pick that thread back up.\n"
                : "";
        return """
                I can help you:
                - understand Mythoria features;
                - check general wallet details;
                - explain login, OTP, and identity verification;
                - clarify roles and permissions;
                - create worlds, lore, characters, books, and event ideas.
                """ + memoryLine + """

                Tell me what you want to do next, and I will guide you.
                """;
    }

    private String continueResponse(Language language, ConversationMemory memory, String message) {
        if (!memory.hasMemory()) {
            return featureResponse(message, language);
        }

        return language == Language.FR
                ? """
                D'accord, je reprends ton fil recent sur %s.

                Prochaine etape possible: donne-moi une contrainte precise, par exemple le ton, le budget, le role utilisateur, ou le type de scene, et je construis la suite autour de ca.
                """.formatted(memory.topicLabel(language))
                : """
                Sure, I will pick up your recent thread about %s.

                Good next step: give me one constraint, like tone, budget, user role, or scene type, and I will build the continuation around it.
                """.formatted(memory.topicLabel(language));
    }

    private String walletResponse(int userId, Language language) {
        if (userId <= 0) {
            return language == Language.FR
                    ? "Je peux aider avec le wallet, mais je n'ai pas encore de compte MySQL valide pour lire tes donnees. Connecte-toi avec un compte enregistre, puis je pourrai donner un resume plus precis."
                    : "I can help with wallet questions, but I do not have a valid MySQL user id to read your data. Sign in with a saved account, then I can give a more precise summary.";
        }

        List<Wallet> wallets = walletRepository.findByUserId(userId);
        if (wallets.isEmpty()) {
            return language == Language.FR
                    ? "Je n'ai trouve aucun wallet pour ton compte. Conseil general: garde un plafond clair, verifie le statut du wallet, et conserve une marge avant les evenements ou achats importants."
                    : "I did not find a wallet for your account. General advice: keep a clear spending ceiling, check your wallet status, and leave a margin before important events or purchases.";
        }

        Wallet latest = wallets.get(0);
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        String balance = format.format(latest.balance());
        String ceiling = format.format(latest.ceiling());
        double remaining = Math.max(0, latest.ceiling() - latest.balance());

        return language == Language.FR
                ? "Ton wallet le plus recent indique: solde " + balance + " " + latest.currency()
                + ", plafond " + ceiling + ", statut " + latest.status() + ". Il reste environ "
                + format.format(remaining) + " avant le plafond. Conseil: garde une marge pour les tickets, livres ou collaborations. Tu veux que je t'aide a preparer un budget simple?"
                : "Your latest wallet shows: balance " + balance + " " + latest.currency()
                + ", ceiling " + ceiling + ", status " + latest.status() + ". You have about "
                + format.format(remaining) + " left before the ceiling. Suggestion: keep a margin for tickets, books, or collaborations. Want help planning a simple budget?";
    }

    private String roleResponse(boolean admin, Language language) {
        String knowledge = knowledgeBaseService.findRelevantKnowledge("roles permissions admin user author client");
        if (admin) {
            return language == Language.FR
                    ? "Tu es admin, donc tu peux consulter l'espace Admin et gerer les utilisateurs selon les ecrans disponibles. Voila le contexte utile:\n" + knowledge
                    : "You are an admin, so you can open the Admin area and manage users through the available screens. Useful context:\n" + knowledge;
        }
        return language == Language.FR
                ? "Je peux expliquer les roles, mais je ne peux pas executer d'actions admin pour un utilisateur non-admin. Voila ce qui compte:\n" + knowledge
                : "I can explain roles, but I cannot perform admin actions for a non-admin user. Here is what matters:\n" + knowledge;
    }

    private String securityResponse(Language language, String message) {
        String knowledge = knowledgeBaseService.findRelevantKnowledge(message);
        return language == Language.FR
                ? "Pour la securite Mythoria: utilise ton identifiant et mot de passe, puis valide le code OTP SMS. Ne partage jamais ton mot de passe, ton OTP ou tes tokens. Si quelque chose echoue, redemande un code recent puis reessaie. Details:\n" + knowledge
                : "For Mythoria security: use your username/password, then validate the SMS OTP code. Never share passwords, OTPs, or tokens. If something fails, request a fresh code and try again. Details:\n" + knowledge;
    }

    private String writingResponse(String message, Language language) {
        if (language == Language.FR) {
            return """
                    Bonne piste. Voici une idee fantasy pour Mythoria:
                    - Monde: un archipel suspendu ou chaque ile conserve une memoire ancienne.
                    - Conflit: une guilde de cartographes vole les souvenirs pour redessiner les royaumes.
                    - Personnage: une archiviste qui entend les mensonges des cartes.
                    - Royaume: Asterhavn, cite-port gouvernee par des serments graves dans le verre.
                    - Depart d'histoire: le heros trouve une carte qui montre un lieu disparu demain, pas hier.

                    Si tu veux, je peux transformer cette base en chapitre, lore, fiche personnage ou quete.
                    """;
        }
        return """
                Nice direction. Here is a fantasy idea for Mythoria:
                - World: a suspended archipelago where each island stores an ancient memory.
                - Conflict: a guild of cartographers steals memories to redraw kingdoms.
                - Character: an archivist who can hear lies inside maps.
                - Kingdom: Asterhavn, a harbor-city ruled by oaths etched into glass.
                - Story hook: the hero finds a map showing a place that disappears tomorrow, not yesterday.

                I can turn this into a chapter, lore entry, character sheet, or quest next.
                """;
    }

    private String levelResponse(Language language) {
        return language == Language.FR
                ? "Pour augmenter ton niveau: complete ton profil, garde ton wallet actif, participe aux evenements, cree des mondes ou lores, propose des livres, et collabore avec d'autres createurs. Le plus simple est de choisir une action courte maintenant."
                : "To increase your level: complete your profile, keep your wallet active, join events, create worlds or lore, propose books, and collaborate with other creators. The easiest move is to pick one small action now.";
    }

    private String eventResponse(Language language) {
        return language == Language.FR
                ? "Pour les evenements et tickets: verifie ton solde wallet, garde une marge sous le plafond, puis cherche les evenements lies a tes mondes, livres ou collaborations. Je peux aussi t'aider a formuler une idee d'evenement."
                : "For events and tickets: check your wallet balance, keep room under the ceiling, then look for events connected to your worlds, books, or collaborations. I can also help shape an event idea.";
    }

    private String featureResponse(String message, Language language) {
        String knowledge = knowledgeBaseService.findRelevantKnowledge(message);
        return language == Language.FR
                ? "Je vois. Voici le contexte Mythoria le plus proche de ta demande:\n" + knowledge + "\n\nSi tu me donnes ton objectif exact, je peux te proposer la prochaine etape."
                : "Got it. Here is the closest Mythoria context for your request:\n" + knowledge + "\n\nIf you give me your exact goal, I can suggest the next step.";
    }

    private static int parseUserId(User user) {
        if (user == null) {
            return -1;
        }
        try {
            return Integer.parseInt(user.id());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static Language detectLanguage(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "bonjour", "salut", "comment", "aide", "mon ", "ma ", "mes ", "role", "mot de passe", "portefeuille", "solde", "niveau")) {
            return Language.FR;
        }
        return Language.EN;
    }

    private ConversationMemory loadMemory(int userId, Language fallbackLanguage) {
        if (userId <= 0) {
            return ConversationMemory.empty(fallbackLanguage);
        }

        List<String> recentMessages = chatHistoryDAO.findRecentUserMessages(userId, MEMORY_MESSAGE_LIMIT);
        if (recentMessages.isEmpty()) {
            return ConversationMemory.empty(fallbackLanguage);
        }

        int french = 0;
        int english = 0;
        int writing = 0;
        int wallet = 0;
        int security = 0;
        int roles = 0;
        int events = 0;
        int levels = 0;

        for (String recentMessage : recentMessages) {
            Language detected = detectLanguage(recentMessage);
            if (detected == Language.FR) {
                french++;
            } else {
                english++;
            }

            String lower = recentMessage.toLowerCase(Locale.ROOT);
            if (containsAny(lower, "world", "lore", "character", "kingdom", "story", "book", "fantasy", "monde", "royaume", "personnage", "histoire", "livre")) {
                writing++;
            }
            if (containsAny(lower, "wallet", "solde", "balance", "argent", "finance", "financial", "money", "portefeuille")) {
                wallet++;
            }
            if (containsAny(lower, "password", "mot de passe", "login", "connexion", "2fa", "otp", "identity", "identite", "verification")) {
                security++;
            }
            if (containsAny(lower, "role", "admin", "permission", "permissions", "autorisation")) {
                roles++;
            }
            if (containsAny(lower, "event", "ticket", "collaboration", "collab", "evenement")) {
                events++;
            }
            if (containsAny(lower, "level", "rank", "score", "niveau", "points", "pc")) {
                levels++;
            }
        }

        Language preferredLanguage = french > english ? Language.FR : fallbackLanguage;
        MemoryTopic topic = strongestTopic(writing, wallet, security, roles, events, levels);
        return new ConversationMemory(preferredLanguage, topic, recentMessages.size());
    }

    private static MemoryTopic strongestTopic(int writing, int wallet, int security, int roles, int events, int levels) {
        MemoryTopic topic = MemoryTopic.NONE;
        int score = 0;

        if (writing > score) {
            topic = MemoryTopic.WRITING;
            score = writing;
        }
        if (wallet > score) {
            topic = MemoryTopic.WALLET;
            score = wallet;
        }
        if (security > score) {
            topic = MemoryTopic.SECURITY;
            score = security;
        }
        if (roles > score) {
            topic = MemoryTopic.ROLES;
            score = roles;
        }
        if (events > score) {
            topic = MemoryTopic.EVENTS;
            score = events;
        }
        if (levels > score) {
            topic = MemoryTopic.LEVELS;
        }

        return topic;
    }

    private enum Language {
        EN,
        FR
    }

    private enum MemoryTopic {
        NONE,
        WRITING,
        WALLET,
        SECURITY,
        ROLES,
        EVENTS,
        LEVELS
    }

    private record ConversationMemory(Language preferredLanguage, MemoryTopic strongestTopic, int messageCount) {
        private static ConversationMemory empty(Language language) {
            return new ConversationMemory(language, MemoryTopic.NONE, 0);
        }

        private boolean hasMemory() {
            return messageCount > 1 && strongestTopic != MemoryTopic.NONE;
        }

        private String topicLabel(Language language) {
            return switch (strongestTopic) {
                case WRITING -> language == Language.FR ? "l'ecriture fantasy et le lore" : "fantasy writing and lore";
                case WALLET -> language == Language.FR ? "le wallet et le budget" : "wallet and budget";
                case SECURITY -> language == Language.FR ? "la securite du compte" : "account security";
                case ROLES -> language == Language.FR ? "les roles et permissions" : "roles and permissions";
                case EVENTS -> language == Language.FR ? "les evenements et collaborations" : "events and collaborations";
                case LEVELS -> language == Language.FR ? "les niveaux et points" : "levels and points";
                case NONE -> language == Language.FR ? "Mythoria" : "Mythoria";
            };
        }
    }
}
