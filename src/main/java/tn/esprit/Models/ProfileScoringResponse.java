package tn.esprit.Models;

public record ProfileScoringResponse(
        String userId,
        int score,
        String level,
        String message
) {
}
