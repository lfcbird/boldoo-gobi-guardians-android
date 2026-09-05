package com.boldtechnology.boldoogame;

public final class Camera2D {
    public float x;
    public float shakeX;
    public float shakeY;
    private float shakeTime;
    private float shakeStrength;
    private float clock;

    public void reset() {
        x = 0f;
        shakeX = 0f;
        shakeY = 0f;
        shakeTime = 0f;
        clock = 0f;
    }

    public void update(float playerX, float velocityX, float worldWidth, float viewportWidth, float dt) {
        float lookAhead = GameMath.clamp(velocityX * 0.36f, -150f, 150f);
        float target = GameMath.clamp(playerX - viewportWidth * 0.38f + lookAhead,
                0f, Math.max(0f, worldWidth - viewportWidth));
        x = GameMath.smooth(x, target, 5.5f, dt);
        clock += dt;
        if (shakeTime > 0f) {
            shakeTime -= dt;
            float fade = GameMath.clamp(shakeTime * 6f, 0f, 1f);
            shakeX = (float) Math.sin(clock * 67f) * shakeStrength * fade;
            shakeY = (float) Math.cos(clock * 53f) * shakeStrength * 0.6f * fade;
        } else {
            shakeX = shakeY = 0f;
        }
    }

    public void shake(float strength, float duration) {
        shakeStrength = Math.max(shakeStrength, strength);
        shakeTime = Math.max(shakeTime, duration);
    }
}
