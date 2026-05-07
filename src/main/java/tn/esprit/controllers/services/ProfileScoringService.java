package tn.esprit.controllers.services;

import tn.esprit.data.UserRepository;
import tn.esprit.Models.ProfileScoringResponse;
import tn.esprit.Models.User;
import tn.esprit.Models.UserActivity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Optional;

public final class ProfileScoringService {
    private static final int POINTS_PER_LOGIN = 2;
    private static final int POINTS_FULL_PROFILE = 50;
    private static final int POINTS_PER_ARTWORK = 25;
    private static final int POINTS_PER_BOOK = 40;
    private static final int POINTS_PER_EVENT = 30;
    private static final int POINTS_PER_PURCHASE = 10;
    private static final int POINTS_PER_COMMENT = 5;
    private static final int POINTS_PER_COLLABORATION = 35;
    private static final int POINTS_PER_REPORT = -25;

    private final UserRepository userRepository;
    private final UserActivityService userActivityService;
    private final Gson gson = new GsonBuilder().create();

    public ProfileScoringService() {
        this(new UserRepository(), new UserActivityService());
    }

    public ProfileScoringService(UserRepository userRepository, UserActivityService userActivityService) {
        this.userRepository = userRepository;
        this.userActivityService = userActivityService;
    }

    public ProfileScoringResponse calculateAndUpdateScore(String userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return new ProfileScoringResponse(
                    Optional.ofNullable(userId).orElse(""),
                    0,
                    "",
                    "Utilisateur introuvable."
            );
        }

        UserActivity activity = userActivityService.analyze(user.get());
        int score = calculateScore(activity);
        String level = levelFromScore(score);
        boolean updated = userRepository.updateScoreAndLevel(user.get().id(), score, level);
        String message = updated
                ? "Score et niveau du profil mis a jour."
                : "Score calcule, mais la mise a jour du profil a echoue.";

        return new ProfileScoringResponse(user.get().id(), score, level, message);
    }

    public String calculateAndUpdateScoreJson(String userId) {
        return gson.toJson(calculateAndUpdateScore(userId));
    }

    public int calculateScore(UserActivity activity) {
        int profilePoints = (activity.profileCompletionPercent() * POINTS_FULL_PROFILE) / 100;
        int score = 0;
        score += activity.loginCount() * POINTS_PER_LOGIN;
        score += profilePoints;
        score += activity.createdArtworks() * POINTS_PER_ARTWORK;
        score += activity.createdBooks() * POINTS_PER_BOOK;
        score += activity.eventParticipations() * POINTS_PER_EVENT;
        score += activity.purchases() * POINTS_PER_PURCHASE;
        score += activity.comments() * POINTS_PER_COMMENT;
        score += activity.collaborations() * POINTS_PER_COLLABORATION;
        score += activity.reports() * POINTS_PER_REPORT;
        return Math.max(0, score);
    }

    public static String levelFromScore(int score) {
        if (score >= 1000) {
            return "L\u00e9gende";
        }
        if (score >= 600) {
            return "Expert";
        }
        if (score >= 300) {
            return "Avanc\u00e9";
        }
        if (score >= 100) {
            return "Actif";
        }
        return "D\u00e9butant";
    }
}
