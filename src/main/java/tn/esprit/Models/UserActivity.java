package tn.esprit.Models;

public record UserActivity(
        int loginCount,
        int profileCompletionPercent,
        int createdArtworks,
        int createdBooks,
        int eventParticipations,
        int purchases,
        int comments,
        int collaborations,
        int reports
) {
}
