package com.example.mythoriadesktop.model;

public record ProfileScoringResponse(
        String userId,
        int score,
        String level,
        String message
) {
}
