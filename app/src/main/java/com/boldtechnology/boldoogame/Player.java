package com.boldtechnology.boldoogame;

public final class Player {
    public static final float WIDTH = 86f;
    public static final float HEIGHT = 108f;
    private static final float GRAVITY = 1780f;
    private static final float RUN_SPEED = 350f;
    private static final float ACCELERATION = 1900f;
    private static final float DECELERATION = 2450f;
    private static final float JUMP_SPEED = 690f;
    private static final float COYOTE_SECONDS = 0.13f;
    private static final float JUMP_BUFFER_SECONDS = 0.14f;

    public float x;
    public float y;
    public float velocityX;
    public float velocityY;
    public int facing = 1;
    public boolean onGround;
    public boolean groundPounding;
    public boolean hasDash;
    public boolean hasShield;
    public boolean hasGroundPound;
    public int shieldCharges;
    public float invincibleTimer;
    public float dashCooldown;
    public float dashTimer;
    public boolean jumpStarted;
    public boolean dashStarted;
    public boolean groundPoundStarted;
    public boolean groundPoundImpact;
    public boolean landed;
    public LevelData.Platform standingPlatform;

    private float coyoteTimer;
    private float jumpBufferTimer;

    public enum DamageResult { NONE, SHIELDED, HURT }

    public void reset(float startX, float startY) {
        x = startX;
        y = startY;
        velocityX = 0f;
        velocityY = 0f;
        facing = 1;
        onGround = false;
        groundPounding = false;
        invincibleTimer = 0f;
        dashCooldown = 0f;
        dashTimer = 0f;
        coyoteTimer = 0f;
        jumpBufferTimer = 0f;
        standingPlatform = null;
        hasDash = false;
        hasShield = false;
        hasGroundPound = false;
        shieldCharges = 0;
        resetTickFlags();
    }

    public void respawn(float spawnX, float spawnY) {
        x = spawnX;
        y = spawnY;
        velocityX = 0f;
        velocityY = 0f;
        onGround = false;
        standingPlatform = null;
        groundPounding = false;
        dashTimer = 0f;
        invincibleTimer = 2f;
        if (hasShield) shieldCharges = Math.max(1, shieldCharges);
    }

    public void update(float dt, InputController input, LevelData level, float externalPushX) {
        resetTickFlags();
        invincibleTimer = Math.max(0f, invincibleTimer - dt);
        dashCooldown = Math.max(0f, dashCooldown - dt);
        dashTimer = Math.max(0f, dashTimer - dt);
        jumpBufferTimer = Math.max(0f, jumpBufferTimer - dt);

        if (standingPlatform != null && onGround) {
            x += standingPlatform.deltaX;
            y += standingPlatform.deltaY;
        }

        if (onGround) coyoteTimer = COYOTE_SECONDS;
        else coyoteTimer = Math.max(0f, coyoteTimer - dt);

        if (input.consumeJumpPressed()) jumpBufferTimer = JUMP_BUFFER_SECONDS;
        if (input.consumeJumpReleased() && velocityY < -180f) velocityY *= 0.48f;

        int axis = input.leftHeld == input.rightHeld ? 0 : (input.leftHeld ? -1 : 1);
        if (axis != 0) facing = axis;

        boolean abilityPressed = input.consumeAbilityPressed();
        if (abilityPressed && hasGroundPound && !onGround && !groundPounding) {
            groundPounding = true;
            groundPoundStarted = true;
            dashTimer = 0f;
            velocityY = Math.max(velocityY, 720f);
            velocityX *= 0.35f;
        } else if (abilityPressed && hasDash && dashCooldown <= 0f && !groundPounding) {
            dashTimer = 0.24f;
            dashCooldown = 1.05f;
            velocityX = facing * 780f;
            velocityY = Math.min(velocityY, 80f);
            dashStarted = true;
        }

        if (jumpBufferTimer > 0f && coyoteTimer > 0f && !groundPounding) {
            velocityY = -JUMP_SPEED;
            onGround = false;
            standingPlatform = null;
            coyoteTimer = 0f;
            jumpBufferTimer = 0f;
            jumpStarted = true;
        }

        if (dashTimer <= 0f && !groundPounding) {
            float target = GameMath.clamp(axis * RUN_SPEED + externalPushX, -500f, 500f);
            float rate = axis == 0 ? DECELERATION : ACCELERATION;
            velocityX = GameMath.approach(velocityX, target, rate * dt);
        } else {
            velocityX += externalPushX * dt * 0.25f;
        }

        float oldX = x;
        x += velocityX * dt;
        resolveHorizontal(oldX, level);
        x = GameMath.clamp(x, 0f, level.worldWidth - WIDTH);

        boolean wasGroundPounding = groundPounding;
        float gravityScale = input.jumpHeld && velocityY < 0f ? 0.86f : 1.12f;
        if (groundPounding) gravityScale = 1.38f;
        velocityY += GRAVITY * gravityScale * dt;
        velocityY = Math.min(velocityY, groundPounding ? 1040f : 880f);
        float oldY = y;
        y += velocityY * dt;
        resolveVertical(oldY, level);
        if (landed && wasGroundPounding) {
            groundPounding = false;
            groundPoundImpact = true;
        }
    }

    private void resolveHorizontal(float oldX, LevelData level) {
        if (velocityX == 0f) return;
        for (LevelData.Platform platform : level.platforms) {
            resolveHorizontalSolid(oldX, platform.x, platform.y, platform.width, platform.height);
        }
        for (LevelData.Barrier barrier : level.barriers) {
            if (barrier.active) resolveHorizontalSolid(oldX, barrier.x, barrier.y, barrier.width, barrier.height);
        }
    }

    private void resolveHorizontalSolid(float oldX, float sx, float sy, float sw, float sh) {
        if (y + HEIGHT <= sy + 5f || y >= sy + sh - 5f) return;
        if (!GameMath.overlaps(x, y, WIDTH, HEIGHT, sx, sy, sw, sh)) return;
        if (velocityX > 0f && oldX + WIDTH <= sx + 12f) x = sx - WIDTH;
        else if (velocityX < 0f && oldX >= sx + sw - 12f) x = sx + sw;
        else return;
        velocityX = 0f;
        dashTimer = 0f;
    }

    private void resolveVertical(float oldY, LevelData level) {
        boolean wasOnGround = onGround;
        onGround = false;
        standingPlatform = null;
        float oldBottom = oldY + HEIGHT;
        float newBottom = y + HEIGHT;
        float bestTop = Float.MAX_VALUE;
        LevelData.Platform bestPlatform = null;

        if (velocityY >= 0f) {
            for (LevelData.Platform platform : level.platforms) {
                if (x + WIDTH <= platform.x + 8f || x >= platform.x + platform.width - 8f) continue;
                if (oldBottom <= platform.y + Math.max(14f, Math.abs(platform.deltaY) + 8f)
                        && newBottom >= platform.y && platform.y < bestTop) {
                    bestTop = platform.y;
                    bestPlatform = platform;
                }
            }
            for (LevelData.Barrier barrier : level.barriers) {
                if (!barrier.active) continue;
                if (x + WIDTH <= barrier.x + 6f || x >= barrier.x + barrier.width - 6f) continue;
                if (oldBottom <= barrier.y + 12f && newBottom >= barrier.y && barrier.y < bestTop) {
                    bestTop = barrier.y;
                    bestPlatform = null;
                }
            }
            if (bestTop < Float.MAX_VALUE) {
                y = bestTop - HEIGHT;
                velocityY = 0f;
                onGround = true;
                standingPlatform = bestPlatform;
                landed = !wasOnGround;
                coyoteTimer = COYOTE_SECONDS;
            }
        } else {
            float bestBottom = -Float.MAX_VALUE;
            for (LevelData.Platform platform : level.platforms) {
                float bottom = platform.y + platform.height;
                if (x + WIDTH <= platform.x + 7f || x >= platform.x + platform.width - 7f) continue;
                if (oldY >= bottom - 10f && y <= bottom && bottom > bestBottom) bestBottom = bottom;
            }
            for (LevelData.Barrier barrier : level.barriers) {
                if (!barrier.active) continue;
                float bottom = barrier.y + barrier.height;
                if (x + WIDTH <= barrier.x || x >= barrier.x + barrier.width) continue;
                if (oldY >= bottom - 10f && y <= bottom && bottom > bestBottom) bestBottom = bottom;
            }
            if (bestBottom > -Float.MAX_VALUE) {
                y = bestBottom;
                velocityY = 30f;
            }
        }
    }

    public void grantAbility(String type) {
        if (LevelData.AbilityPickup.DASH.equals(type)) hasDash = true;
        if (LevelData.AbilityPickup.SHIELD.equals(type)) {
            hasShield = true;
            shieldCharges = Math.max(1, shieldCharges);
        }
        if (LevelData.AbilityPickup.GROUND_POUND.equals(type)) hasGroundPound = true;
    }

    public DamageResult damage(float sourceX) {
        if (invincibleTimer > 0f || dashTimer > 0f) return DamageResult.NONE;
        if (hasShield && shieldCharges > 0) {
            shieldCharges--;
            invincibleTimer = 1.2f;
            velocityX = sourceX < x ? 260f : -260f;
            velocityY = -280f;
            return DamageResult.SHIELDED;
        }
        invincibleTimer = 1.8f;
        velocityX = sourceX < x ? 330f : -330f;
        velocityY = -390f;
        groundPounding = false;
        return DamageResult.HURT;
    }

    public boolean intersects(float otherX, float otherY, float width, float height) {
        return GameMath.overlaps(x + 12f, y + 7f, WIDTH - 24f, HEIGHT - 10f,
                otherX, otherY, width, height);
    }

    public float centerX() {
        return x + WIDTH * 0.5f;
    }

    public float centerY() {
        return y + HEIGHT * 0.5f;
    }

    private void resetTickFlags() {
        jumpStarted = false;
        dashStarted = false;
        groundPoundStarted = false;
        groundPoundImpact = false;
        landed = false;
    }
}
