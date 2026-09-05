package com.boldtechnology.boldoogame;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ParticleSystem {
    private static final int MAX_PARTICLES = 220;
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random(9173L);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public void clear() {
        particles.clear();
    }

    public void emit(GameEvent event) {
        int count;
        int color;
        float speed;
        switch (event.type) {
            case WATER:
            case TRAIL:
                count = 13; color = 0xFF73E6FF; speed = 185f; break;
            case TRASH:
                count = 9; color = 0xFF9AD17B; speed = 135f; break;
            case HURT:
            case LIFE_LOST:
                count = 18; color = 0xFFFF806E; speed = 240f; break;
            case CHECKPOINT:
                count = 22; color = 0xFFFFD66E; speed = 210f; break;
            case ABILITY:
            case SHIELD:
                count = 24; color = 0xFF8FF5E1; speed = 245f; break;
            case BARRIER:
            case BOSS_HIT:
                count = 26; color = 0xFF795067; speed = 270f; break;
            case COMPLETE:
                count = 55; color = 0xFFFFCB58; speed = 330f; break;
            default:
                count = 8; color = 0xFFF1CF8B; speed = 145f;
        }
        burst(event.x, event.y, count, color, speed);
    }

    public void dust(float x, float y) {
        burst(x, y, 7, 0xFFD6A965, 105f);
    }

    private void burst(float x, float y, int count, int color, float speed) {
        for (int i = 0; i < count && particles.size() < MAX_PARTICLES; i++) {
            float angle = random.nextFloat() * (float) Math.PI * 2f;
            float magnitude = speed * (0.35f + random.nextFloat() * 0.65f);
            Particle particle = new Particle();
            particle.x = x;
            particle.y = y;
            particle.vx = (float) Math.cos(angle) * magnitude;
            particle.vy = (float) Math.sin(angle) * magnitude - 55f;
            particle.life = particle.maxLife = 0.32f + random.nextFloat() * 0.48f;
            particle.radius = 2.5f + random.nextFloat() * 5.5f;
            particle.color = color;
            particles.add(particle);
        }
    }

    public void update(float dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle particle = particles.get(i);
            particle.life -= dt;
            if (particle.life <= 0f) {
                particles.remove(i);
                continue;
            }
            particle.vy += 380f * dt;
            particle.x += particle.vx * dt;
            particle.y += particle.vy * dt;
            particle.vx *= (float) Math.pow(0.12, dt);
        }
    }

    public void draw(Canvas canvas) {
        for (Particle particle : particles) {
            float alpha = GameMath.clamp(particle.life / particle.maxLife, 0f, 1f);
            paint.setColor(particle.color);
            paint.setAlpha(Math.round(alpha * 255f));
            canvas.drawCircle(particle.x, particle.y, particle.radius * (0.5f + alpha * 0.5f), paint);
        }
        paint.setAlpha(255);
    }

    private static final class Particle {
        float x;
        float y;
        float vx;
        float vy;
        float life;
        float maxLife;
        float radius;
        int color;
    }
}
