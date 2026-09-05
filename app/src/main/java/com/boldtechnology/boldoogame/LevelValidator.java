package com.boldtechnology.boldoogame;

import java.util.HashSet;
import java.util.Set;

public final class LevelValidator {
    private LevelValidator() {}

    public static void validate(LevelData level) {
        if (level.id < 1 || level.id > 3) throw new IllegalArgumentException("Invalid level id");
        if (level.worldWidth < 1280f) throw new IllegalArgumentException("World is too narrow");
        if (level.goalX <= level.startX || level.goalX > level.worldWidth) {
            throw new IllegalArgumentException("Goal must be inside the world after the start");
        }
        if (level.platforms.isEmpty()) throw new IllegalArgumentException("Level has no platforms");
        if (level.requiredWater > level.totalWater()) throw new IllegalArgumentException("Not enough water drops");
        if (level.requiredMarkers > level.totalMarkers()) throw new IllegalArgumentException("Not enough markers");

        Set<String> ids = new HashSet<>();
        for (LevelData.Platform platform : level.platforms) {
            requireUnique(ids, platform.id);
            requireBounds(level, platform.x, platform.y, platform.width, platform.height);
        }
        for (LevelData.Collectible collectible : level.collectibles) {
            requireUnique(ids, collectible.id);
            requirePoint(level, collectible.x, collectible.y);
        }
        for (LevelData.Enemy enemy : level.enemies) {
            requireUnique(ids, enemy.id);
            requireBounds(level, enemy.x, enemy.y, enemy.width, enemy.height);
            if (enemy.minX > enemy.maxX) throw new IllegalArgumentException("Enemy patrol is reversed");
        }
        for (LevelData.Checkpoint checkpoint : level.checkpoints) {
            requireUnique(ids, checkpoint.id);
            requirePoint(level, checkpoint.x, checkpoint.y);
        }
    }

    private static void requireUnique(Set<String> ids, String id) {
        if (id == null || id.isEmpty() || !ids.add(id)) {
            throw new IllegalArgumentException("Missing or duplicate id: " + id);
        }
    }

    private static void requirePoint(LevelData level, float x, float y) {
        if (x < 0f || x > level.worldWidth || y < 0f || y > 900f) {
            throw new IllegalArgumentException("Object is outside world bounds");
        }
    }

    private static void requireBounds(LevelData level, float x, float y, float width, float height) {
        if (width <= 0f || height <= 0f || x < 0f || x + width > level.worldWidth || y < 0f || y > 900f) {
            throw new IllegalArgumentException("Invalid object bounds");
        }
    }
}
