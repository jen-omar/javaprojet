package com.example.mythoriadesktop.api;

import com.example.mythoriadesktop.services.ProfileScoringService;

public final class ProfileScoringApi {
    private final ProfileScoringService profileScoringService;

    public ProfileScoringApi() {
        this(new ProfileScoringService());
    }

    public ProfileScoringApi(ProfileScoringService profileScoringService) {
        this.profileScoringService = profileScoringService;
    }

    public String scoreProfile(String userId) {
        return profileScoringService.calculateAndUpdateScoreJson(userId);
    }
}
