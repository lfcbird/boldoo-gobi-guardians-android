package com.boldtechnology.boldoogame;

import java.util.ArrayList;
import java.util.List;

public final class LevelData {
    public int id;
    public String title = "";
    public String titleMn = "";
    public String objectiveMn = "";
    public String environment = "OASIS";
    public float worldWidth = 3200f;
    public float groundY = 610f;
    public float startX = 90f;
    public float startY = 490f;
    public float goalX = 3000f;
    public float parTime = 120f;
    public int requiredWater;
    public int requiredMarkers;
    public final List<Platform> platforms = new ArrayList<>();
    public final List<Collectible> collectibles = new ArrayList<>();
    public final List<Enemy> enemies = new ArrayList<>();
    public final List<Hazard> hazards = new ArrayList<>();
    public final List<Checkpoint> checkpoints = new ArrayList<>();
    public final List<AbilityPickup> abilities = new ArrayList<>();
    public final List<Barrier> barriers = new ArrayList<>();

    public int totalWater() {
        int count = 0;
        for (Collectible collectible : collectibles) {
            if (Collectible.WATER.equals(collectible.type)) count++;
        }
        return count;
    }

    public int totalMarkers() {
        int count = 0;
        for (Collectible collectible : collectibles) {
            if (Collectible.TRAIL.equals(collectible.type)) count++;
        }
        return count;
    }

    public int totalScoredCollectibles() {
        return collectibles.size();
    }

    public static final class Platform {
        public String id = "";
        public float baseX;
        public float baseY;
        public float x;
        public float y;
        public float width;
        public float height;
        public float moveX;
        public float moveY;
        public float speed;
        public float phase;
        public float deltaX;
        public float deltaY;

        public void update(float elapsed) {
            float oldX = x;
            float oldY = y;
            if (speed > 0f && (moveX != 0f || moveY != 0f)) {
                float wave = (float) Math.sin(elapsed * speed + phase);
                x = baseX + moveX * wave;
                y = baseY + moveY * wave;
            } else {
                x = baseX;
                y = baseY;
            }
            deltaX = x - oldX;
            deltaY = y - oldY;
        }

        public boolean isMoving() {
            return moveX != 0f || moveY != 0f;
        }
    }

    public static final class Collectible {
        public static final String WATER = "WATER";
        public static final String TRAIL = "TRAIL";
        public static final String TRASH = "TRASH";
        public String id = "";
        public String type = WATER;
        public float x;
        public float y;
        public boolean collected;
    }

    public static final class Enemy {
        public static final String SMOGLING = "SMOGLING";
        public static final String GREAT_SMOG = "GREAT_SMOG";
        public String id = "";
        public String type = SMOGLING;
        public float x;
        public float y;
        public float width = 78f;
        public float height = 78f;
        public float minX;
        public float maxX;
        public float speed = 90f;
        public int direction = -1;
        public int health = 1;
        public int maxHealth = 1;
        public float hitCooldown;
        public boolean active = true;

        public boolean isBoss() {
            return GREAT_SMOG.equals(type);
        }

        public int phase() {
            if (!isBoss() || maxHealth <= 1) return 1;
            return Math.max(1, Math.min(3, maxHealth - health + 1));
        }
    }

    public static final class Hazard {
        public static final String WIND = "WIND";
        public static final String SPIKES = "SPIKES";
        public static final String ROCK_ZONE = "ROCK_ZONE";
        public String id = "";
        public String type = SPIKES;
        public float x;
        public float y;
        public float width;
        public float height;
        public float strength;
        public float interval = 1.5f;
        public float timer;
    }

    public static final class Checkpoint {
        public String id = "";
        public float x;
        public float y;
        public boolean activated;
    }

    public static final class AbilityPickup {
        public static final String DASH = "DASH";
        public static final String SHIELD = "SHIELD";
        public static final String GROUND_POUND = "GROUND_POUND";
        public String id = "";
        public String type = DASH;
        public float x;
        public float y;
        public boolean collected;
    }

    public static final class Barrier {
        public String id = "";
        public float x;
        public float y;
        public float width;
        public float height;
        public int health = 1;
        public boolean active = true;
    }

    public static final class FallingRock {
        public float x;
        public float y;
        public float velocityY;
        public float radius;
        public boolean active = true;
    }
}
