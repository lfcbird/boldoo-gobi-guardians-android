package com.boldtechnology.boldoogame;

public final class ProgressionRules {
    private ProgressionRules() {}

    public static int unlockedAfterCompletion(int completedLevel, int levelCount) {
        return Math.min(levelCount, Math.max(1, completedLevel + 1));
    }
}
