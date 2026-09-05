package com.boldtechnology.boldoogame;

public final class ScoreCalculator {
    private ScoreCalculator() {}

    public static int completionBonus(int lives, float elapsedSeconds, float parSeconds) {
        int lifeBonus = Math.max(0, lives) * 300;
        int timeBonus = Math.max(0, Math.round((parSeconds - elapsedSeconds) * 12f));
        return 900 + lifeBonus + timeBonus;
    }

    public static int stars(int collected, int total, int lives, float elapsedSeconds, float parSeconds) {
        if (total <= 0) total = 1;
        float ratio = collected / (float) total;
        int stars = ratio >= 0.60f ? 1 : 0;
        if (ratio >= 0.90f && lives >= 1) stars = 2;
        if (ratio >= 0.999f && lives >= 2 && elapsedSeconds <= parSeconds) stars = 3;
        return Math.max(1, stars);
    }
}
