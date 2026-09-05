package com.boldtechnology.boldoogame;

public final class GameMath {
    private GameMath() {}

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float approach(float value, float target, float amount) {
        if (value < target) return Math.min(value + amount, target);
        if (value > target) return Math.max(value - amount, target);
        return target;
    }

    public static boolean overlaps(float ax, float ay, float aw, float ah,
                                   float bx, float by, float bw, float bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    public static float smooth(float current, float target, float sharpness, float dt) {
        float amount = 1f - (float) Math.exp(-sharpness * dt);
        return current + (target - current) * amount;
    }
}
