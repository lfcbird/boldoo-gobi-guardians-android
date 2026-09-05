package com.boldtechnology.boldoogame;

import android.content.Context;
import android.content.SharedPreferences;

public final class SaveManager {
    private static final String PREFS = "boldoo_guardians_progress_v2";
    private static final int LEVEL_COUNT = 3;
    private final SharedPreferences prefs;

    public SaveManager(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public GameSettings loadSettings() {
        GameSettings settings = new GameSettings();
        settings.soundEffects = prefs.getBoolean("settings_sfx", true);
        settings.ambientSound = prefs.getBoolean("settings_ambient", true);
        settings.haptics = prefs.getBoolean("settings_haptics", true);
        settings.leftHanded = prefs.getBoolean("settings_left_handed", false);
        settings.controlOpacity = prefs.getFloat("settings_opacity", 0.62f);
        return settings;
    }

    public void saveSettings(GameSettings settings) {
        prefs.edit()
                .putBoolean("settings_sfx", settings.soundEffects)
                .putBoolean("settings_ambient", settings.ambientSound)
                .putBoolean("settings_haptics", settings.haptics)
                .putBoolean("settings_left_handed", settings.leftHanded)
                .putFloat("settings_opacity", settings.controlOpacity)
                .apply();
    }

    public int getUnlockedLevel() {
        return Math.max(1, Math.min(LEVEL_COUNT, prefs.getInt("unlocked_level", 1)));
    }

    public boolean isCompleted(int levelId) {
        return prefs.getBoolean("level_" + levelId + "_completed", false);
    }

    public int getBestScore(int levelId) {
        return prefs.getInt("level_" + levelId + "_best_score", 0);
    }

    public float getBestTime(int levelId) {
        return prefs.getFloat("level_" + levelId + "_best_time", 0f);
    }

    public int getBestStars(int levelId) {
        return prefs.getInt("level_" + levelId + "_stars", 0);
    }

    public int getTotalWater() {
        return prefs.getInt("total_water", 0);
    }

    public void recordCompletion(int levelId, int score, float time, int stars, int water) {
        int unlocked = Math.max(getUnlockedLevel(),
                ProgressionRules.unlockedAfterCompletion(levelId, LEVEL_COUNT));
        int bestScore = Math.max(score, getBestScore(levelId));
        float oldTime = getBestTime(levelId);
        float bestTime = oldTime <= 0f ? time : Math.min(oldTime, time);
        int bestStars = Math.max(stars, getBestStars(levelId));
        prefs.edit()
                .putBoolean("level_" + levelId + "_completed", true)
                .putInt("level_" + levelId + "_best_score", bestScore)
                .putFloat("level_" + levelId + "_best_time", bestTime)
                .putInt("level_" + levelId + "_stars", bestStars)
                .putInt("unlocked_level", unlocked)
                .putInt("total_water", getTotalWater() + Math.max(0, water))
                .apply();
    }

    public void resetProgress() {
        GameSettings settings = loadSettings();
        prefs.edit().clear().commit();
        saveSettings(settings);
    }
}
