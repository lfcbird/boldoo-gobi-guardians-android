package com.boldtechnology.boldoogame;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

public final class GameRenderer {
    public static final float VIEW_W = 1280f;
    public static final float VIEW_H = 720f;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Bitmap background;
    private final Bitmap boldoo;
    private final Bitmap smogling;
    private float elapsed;

    public GameRenderer(Resources resources) {
        background = BitmapFactory.decodeResource(resources, R.drawable.gobi_background);
        boldoo = BitmapFactory.decodeResource(resources, R.drawable.boldoo_player);
        smogling = BitmapFactory.decodeResource(resources, R.drawable.smogling_enemy);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        textPaint.setShadowLayer(3f, 0f, 2f, 0x8809232C);
    }

    public void setElapsed(float elapsed) {
        this.elapsed = elapsed;
    }

    public void drawGame(Canvas canvas, GameEngine engine, GameSettings settings,
                         InputController input, ParticleSystem particles) {
        drawBackdrop(canvas, engine.camera.x, engine.level.worldWidth, engine.level.environment);
        canvas.save();
        canvas.translate(-engine.camera.x + engine.camera.shakeX, engine.camera.shakeY);
        drawDistantDetails(canvas, engine);
        drawPlatforms(canvas, engine.level);
        drawHazards(canvas, engine.level);
        drawFinish(canvas, engine);
        drawCheckpoints(canvas, engine.level);
        drawCollectibles(canvas, engine.level);
        drawAbilities(canvas, engine.level);
        drawBarriers(canvas, engine.level);
        drawEnemies(canvas, engine);
        drawRocks(canvas, engine);
        drawPlayer(canvas, engine.player, engine.runState == GameEngine.RunState.COMPLETED);
        particles.draw(canvas);
        canvas.restore();
        drawWeather(canvas, engine);
        drawHud(canvas, engine);
        drawControls(canvas, input, settings, engine.activeAbilityLabel());
        if (engine.messageTimer > 0f) drawMessage(canvas, engine.message);
        if (engine.introTimer > 0f) drawLevelIntro(canvas, engine);
    }

    public void drawBackdrop(Canvas canvas, float cameraX, float worldWidth, String environment) {
        float travel = cameraX / Math.max(1f, worldWidth - VIEW_W);
        int srcW = Math.min(background.getWidth(), Math.round(background.getHeight() * VIEW_W / VIEW_H));
        int maxOffset = Math.max(0, background.getWidth() - srcW);
        int srcLeft = Math.round(maxOffset * GameMath.clamp(travel, 0f, 1f));
        Rect src = new Rect(srcLeft, 0, srcLeft + srcW, background.getHeight());
        canvas.drawBitmap(background, src, new RectF(0, 0, VIEW_W, VIEW_H), paint);
        if ("MIGRATION".equals(environment)) {
            paint.setColor(0x22175F66);
            canvas.drawRect(0, 0, VIEW_W, VIEW_H, paint);
        } else if ("STORM".equals(environment)) {
            paint.setColor(0x70463350);
            canvas.drawRect(0, 0, VIEW_W, VIEW_H, paint);
        } else {
            paint.setColor(0x13062938);
            canvas.drawRect(0, 0, VIEW_W, VIEW_H, paint);
        }
    }

    public void drawMenuBackdrop(Canvas canvas) {
        float drift = (float) (Math.sin(elapsed * 0.08f) * 0.5f + 0.5f) * 800f;
        drawBackdrop(canvas, drift, 2080f, "OASIS");
        paint.setColor(0xA90A2632);
        canvas.drawRect(0, 0, VIEW_W, VIEW_H, paint);
        paint.setColor(0x55F5B954);
        canvas.drawCircle(1080, 120, 76 + (float) Math.sin(elapsed) * 4f, paint);
        paint.setColor(0xFF123541);
        canvas.drawCircle(1105, 100, 73, paint);
    }

    private void drawDistantDetails(Canvas canvas, GameEngine engine) {
        float offset = engine.camera.x * 0.78f;
        paint.setColor("STORM".equals(engine.level.environment) ? 0x554F3A43 : 0x446F8C55);
        for (int i = 0; i < 9; i++) {
            float x = i * 610f + offset;
            Path dune = new Path();
            dune.moveTo(x - 220, 610);
            dune.quadTo(x, 465 + (i % 2) * 35, x + 280, 610);
            dune.close();
            canvas.drawPath(dune, paint);
        }
    }

    private void drawPlatforms(Canvas canvas, LevelData level) {
        for (LevelData.Platform platform : level.platforms) {
            int topColor = "STORM".equals(level.environment) ? 0xFFD0A469 : 0xFFF4CB76;
            int bottomColor = "MIGRATION".equals(level.environment) ? 0xFF7B6738 : 0xFF8E542D;
            RectF bounds = new RectF(platform.x, platform.y,
                    platform.x + platform.width, platform.y + platform.height);
            paint.setShader(new LinearGradient(0, platform.y, 0, platform.y + platform.height,
                    topColor, bottomColor, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(bounds, 13f, 13f, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(platform.isMoving() ? 4f : 3f);
            paint.setColor(platform.isMoving() ? 0xFFF5E3A8 : 0xFF5E3925);
            canvas.drawRoundRect(bounds, 13f, 13f, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x99FFF0B5);
            canvas.drawRoundRect(new RectF(bounds.left + 5, bounds.top + 4,
                    bounds.right - 5, Math.min(bounds.bottom, bounds.top + 12)), 7f, 7f, paint);
            if (platform.isMoving()) {
                paint.setColor(0xFF0A7C82);
                canvas.drawCircle(bounds.centerX(), bounds.centerY(), 6f, paint);
            }
        }
    }

    private void drawHazards(Canvas canvas, LevelData level) {
        for (LevelData.Hazard hazard : level.hazards) {
            if (LevelData.Hazard.SPIKES.equals(hazard.type)) {
                int teeth = Math.max(2, Math.round(hazard.width / 22f));
                paint.setColor(0xFF5C5160);
                Path spikes = new Path();
                spikes.moveTo(hazard.x, hazard.y + hazard.height);
                for (int i = 0; i < teeth; i++) {
                    float left = hazard.x + hazard.width * i / teeth;
                    float right = hazard.x + hazard.width * (i + 1) / teeth;
                    spikes.lineTo((left + right) * 0.5f, hazard.y);
                    spikes.lineTo(right, hazard.y + hazard.height);
                }
                spikes.close();
                canvas.drawPath(spikes, paint);
            } else if (LevelData.Hazard.WIND.equals(hazard.type)) {
                paint.setColor(0x55DDF8F4);
                paint.setStrokeWidth(4f);
                paint.setStyle(Paint.Style.STROKE);
                int direction = hazard.strength < 0 ? -1 : 1;
                for (int row = 0; row < 4; row++) {
                    float wave = (elapsed * 105f + row * 117f) % Math.max(120f, hazard.width);
                    float x = direction > 0 ? hazard.x + wave : hazard.x + hazard.width - wave;
                    float y = hazard.y + 85f + row * 86f;
                    canvas.drawLine(x, y, x + direction * 80f, y, paint);
                }
                paint.setStyle(Paint.Style.FILL);
            }
        }
    }

    private void drawFinish(Canvas canvas, GameEngine engine) {
        float x = engine.level.goalX;
        boolean locked = !finishReady(engine);
        paint.setColor(0xFF553624);
        canvas.drawRoundRect(new RectF(x, 340, x + 13, engine.level.groundY), 6, 6, paint);
        Path flag = new Path();
        flag.moveTo(x + 10, 350);
        flag.lineTo(x + 155, 380);
        flag.lineTo(x + 10, 445);
        flag.close();
        paint.setColor(locked ? 0xFF77495A : 0xFF078D92);
        canvas.drawPath(flag, paint);
        centered(canvas, locked ? "ЗОРИЛГО" : "БАРИА", x + 75, 401, 21, locked ? 0xFFFFC9B2 : 0xFFFFE093);
        paint.setColor(locked ? 0xAA755461 : 0xFF18A6A0);
        canvas.drawOval(new RectF(x - 34, engine.level.groundY - 34, x + 60, engine.level.groundY + 18), paint);
        paint.setColor(locked ? 0x55422E38 : 0xAA96FAF1);
        canvas.drawOval(new RectF(x - 23, engine.level.groundY - 27, x + 48, engine.level.groundY + 8), paint);
    }

    private boolean finishReady(GameEngine engine) {
        if (engine.waterCollected < engine.level.requiredWater) return false;
        if (engine.markersCollected < engine.level.requiredMarkers) return false;
        for (LevelData.Barrier barrier : engine.level.barriers) if (barrier.active) return false;
        LevelData.Enemy boss = engine.boss();
        return boss == null || !boss.active;
    }

    private void drawCheckpoints(Canvas canvas, LevelData level) {
        for (LevelData.Checkpoint checkpoint : level.checkpoints) {
            paint.setColor(0xFF5A3827);
            canvas.drawRect(checkpoint.x, checkpoint.y - 118, checkpoint.x + 8, checkpoint.y, paint);
            Path flag = new Path();
            flag.moveTo(checkpoint.x + 7, checkpoint.y - 112);
            flag.lineTo(checkpoint.x + 73, checkpoint.y - 94);
            flag.lineTo(checkpoint.x + 7, checkpoint.y - 70);
            flag.close();
            paint.setColor(checkpoint.activated ? 0xFF1DC6A3 : 0xFFE3AA4C);
            canvas.drawPath(flag, paint);
            if (checkpoint.activated) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(4f);
                paint.setColor(0xAA7CFFE8);
                canvas.drawCircle(checkpoint.x + 25, checkpoint.y - 88, 32, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }
    }

    private void drawCollectibles(Canvas canvas, LevelData level) {
        for (LevelData.Collectible item : level.collectibles) {
            if (item.collected) continue;
            float pulse = 1f + (float) Math.sin(elapsed * 5f + item.x * 0.03f) * 0.08f;
            canvas.save();
            canvas.scale(pulse, pulse, item.x, item.y);
            if (LevelData.Collectible.WATER.equals(item.type)) drawDrop(canvas, item.x, item.y, 1f);
            else if (LevelData.Collectible.TRAIL.equals(item.type)) drawTrailMarker(canvas, item.x, item.y);
            else drawTrash(canvas, item.x, item.y);
            canvas.restore();
        }
    }

    private void drawDrop(Canvas canvas, float x, float y, float scale) {
        Path drop = new Path();
        drop.moveTo(x, y - 27f * scale);
        drop.cubicTo(x + 9f * scale, y - 9f * scale, x + 22f * scale, y + 2f * scale, x + 22f * scale, y + 14f * scale);
        drop.cubicTo(x + 22f * scale, y + 29f * scale, x + 11f * scale, y + 36f * scale, x, y + 36f * scale);
        drop.cubicTo(x - 11f * scale, y + 36f * scale, x - 22f * scale, y + 29f * scale, x - 22f * scale, y + 14f * scale);
        drop.cubicTo(x - 22f * scale, y + 2f * scale, x - 9f * scale, y - 9f * scale, x, y - 27f * scale);
        paint.setShader(new LinearGradient(x - 20, y - 25, x + 20, y + 33,
                0xFFA8FAFF, 0xFF078DB7, Shader.TileMode.CLAMP));
        canvas.drawPath(drop, paint);
        paint.setShader(null);
        paint.setColor(0xFF07536C);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f * scale);
        canvas.drawPath(drop, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xCCFFFFFF);
        canvas.drawCircle(x - 7f * scale, y + 4f * scale, 4.5f * scale, paint);
    }

    private void drawTrailMarker(Canvas canvas, float x, float y) {
        paint.setColor(0xFF65422A);
        canvas.drawRoundRect(new RectF(x - 5, y - 8, x + 5, y + 36), 4, 4, paint);
        Path diamond = new Path();
        diamond.moveTo(x, y - 31);
        diamond.lineTo(x + 27, y - 5);
        diamond.lineTo(x, y + 21);
        diamond.lineTo(x - 27, y - 5);
        diamond.close();
        paint.setColor(0xFFF5C85E);
        canvas.drawPath(diamond, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(0xFF69502B);
        canvas.drawPath(diamond, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawTrash(Canvas canvas, float x, float y) {
        paint.setColor(0xFF557C5A);
        canvas.drawRoundRect(new RectF(x - 18, y - 19, x + 18, y + 23), 6, 6, paint);
        paint.setColor(0xFFCAE19A);
        canvas.drawRect(x - 21, y - 24, x + 21, y - 16, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        canvas.drawCircle(x, y + 2, 9, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawAbilities(Canvas canvas, LevelData level) {
        for (LevelData.AbilityPickup ability : level.abilities) {
            if (ability.collected) continue;
            float pulse = 34f + (float) Math.sin(elapsed * 4.5f) * 5f;
            paint.setColor(0x558BFFF0);
            canvas.drawCircle(ability.x, ability.y, pulse + 12f, paint);
            paint.setColor(0xFF0E868D);
            canvas.drawCircle(ability.x, ability.y, pulse, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
            paint.setColor(0xFFE4FFF6);
            canvas.drawCircle(ability.x, ability.y, pulse, paint);
            paint.setStyle(Paint.Style.FILL);
            if (LevelData.AbilityPickup.DASH.equals(ability.type)) drawBolt(canvas, ability.x, ability.y, 0.8f);
            else if (LevelData.AbilityPickup.SHIELD.equals(ability.type)) drawShield(canvas, ability.x, ability.y, 0.72f);
            else drawDownArrow(canvas, ability.x, ability.y, 0.72f);
        }
    }

    private void drawBarriers(Canvas canvas, LevelData level) {
        for (LevelData.Barrier barrier : level.barriers) {
            if (!barrier.active) continue;
            RectF bounds = new RectF(barrier.x, barrier.y, barrier.x + barrier.width, barrier.y + barrier.height);
            paint.setShader(new LinearGradient(barrier.x, barrier.y, barrier.x + barrier.width, barrier.y + barrier.height,
                    0xFF6B4A62, 0xFF292636, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(bounds, 16, 16, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5f);
            paint.setColor(0xFFA77891);
            canvas.drawRoundRect(bounds, 16, 16, paint);
            paint.setStrokeWidth(3f);
            for (int i = 1; i < 4; i++) {
                float y = barrier.y + barrier.height * i / 4f;
                canvas.drawLine(barrier.x + 12, y, barrier.x + barrier.width - 12, y - 12, paint);
            }
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawEnemies(Canvas canvas, GameEngine engine) {
        for (LevelData.Enemy enemy : engine.level.enemies) {
            if (!enemy.active) continue;
            float bob = (float) Math.sin(elapsed * (enemy.isBoss() ? 2.4f : 4f) + enemy.x * 0.01f) * (enemy.isBoss() ? 7f : 4f);
            RectF dst = new RectF(enemy.x, enemy.y + bob, enemy.x + enemy.width, enemy.y + enemy.height + bob);
            canvas.save();
            if (enemy.direction > 0) canvas.scale(-1, 1, dst.centerX(), dst.centerY());
            if (enemy.isBoss()) {
                float scale = 1f + (float) Math.sin(elapsed * 3f) * 0.025f;
                canvas.scale(scale, scale, dst.centerX(), dst.centerY());
                int tint = enemy.phase() == 1 ? 0xFF5C3E5D : enemy.phase() == 2 ? 0xFF70405A : 0xFF87384C;
                paint.setColorFilter(new PorterDuffColorFilter(tint, PorterDuff.Mode.MULTIPLY));
            } else if (enemy.hitCooldown > 0f && ((int) (enemy.hitCooldown * 12f)) % 2 == 0) {
                paint.setColorFilter(new PorterDuffColorFilter(0xFFFFD9D0, PorterDuff.Mode.SRC_ATOP));
            }
            canvas.drawBitmap(smogling, null, dst, paint);
            paint.setColorFilter(null);
            canvas.restore();
            if (enemy.isBoss()) drawBossHealthWorld(canvas, enemy);
        }
    }

    private void drawBossHealthWorld(Canvas canvas, LevelData.Enemy boss) {
        float left = boss.x + 10;
        float top = boss.y - 25;
        paint.setColor(0xBB1A1924);
        canvas.drawRoundRect(new RectF(left, top, left + boss.width - 20, top + 13), 7, 7, paint);
        paint.setColor(0xFFE15366);
        canvas.drawRoundRect(new RectF(left, top, left + (boss.width - 20) * boss.health / boss.maxHealth, top + 13), 7, 7, paint);
    }

    private void drawRocks(Canvas canvas, GameEngine engine) {
        for (LevelData.FallingRock rock : engine.rocks) {
            float warning = GameMath.clamp((rock.y + 100f) / 650f, 0.18f, 0.78f);
            paint.setColor(withAlpha(0xFF352A30, warning));
            canvas.drawOval(new RectF(rock.x - rock.radius * 1.35f, engine.level.groundY - 9f,
                    rock.x + rock.radius * 1.35f, engine.level.groundY + 5f), paint);
            paint.setColor(0x55301F25);
            canvas.drawOval(new RectF(rock.x - rock.radius * 0.7f, rock.y - rock.radius * 1.7f,
                    rock.x + rock.radius * 0.7f, rock.y + rock.radius * 0.2f), paint);
            paint.setShader(new LinearGradient(rock.x - rock.radius, rock.y - rock.radius,
                    rock.x + rock.radius, rock.y + rock.radius, 0xFFC29568, 0xFF4E3B3B, Shader.TileMode.CLAMP));
            canvas.drawCircle(rock.x, rock.y, rock.radius, paint);
            paint.setShader(null);
            paint.setColor(0xFFDFB888);
            canvas.drawCircle(rock.x - rock.radius * 0.3f, rock.y - rock.radius * 0.3f, rock.radius * 0.18f, paint);
        }
    }

    private void drawPlayer(Canvas canvas, Player player, boolean victory) {
        float speed = Math.abs(player.velocityX);
        float bob = player.onGround && speed > 35f ? (float) Math.sin(elapsed * 14f) * 3.5f : 0f;
        float squashX = 1f;
        float squashY = 1f;
        float rotation = 0f;
        if (!player.onGround && player.velocityY < 0f) {
            squashX = 0.94f; squashY = 1.07f; rotation = player.facing * 3f;
        } else if (!player.onGround && player.velocityY > 120f) {
            squashX = 1.06f; squashY = 0.95f; rotation = -player.facing * 2f;
        } else if (player.onGround && speed < 20f) {
            squashY = 1f + (float) Math.sin(elapsed * 2.2f) * 0.018f;
        }
        if (player.dashTimer > 0f) { squashX = 1.16f; squashY = 0.89f; }
        if (player.groundPounding) { squashX = 1.13f; squashY = 0.88f; rotation = 0f; }
        if (victory) { rotation = (float) Math.sin(elapsed * 5f) * 7f; bob -= 10f + (float) Math.abs(Math.sin(elapsed * 4f)) * 12f; }

        RectF dst = new RectF(player.x, player.y + bob, player.x + Player.WIDTH, player.y + Player.HEIGHT + bob);
        canvas.save();
        if (player.facing < 0) canvas.scale(-1, 1, dst.centerX(), dst.centerY());
        canvas.scale(squashX, squashY, dst.centerX(), dst.bottom);
        canvas.rotate(rotation, dst.centerX(), dst.centerY());
        if (player.invincibleTimer > 0f && ((int) (player.invincibleTimer * 12f)) % 2 == 0) {
            paint.setAlpha(95);
            paint.setColorFilter(new PorterDuffColorFilter(0xFFFFA08D, PorterDuff.Mode.SRC_ATOP));
        }
        canvas.drawBitmap(boldoo, null, dst, paint);
        paint.setAlpha(255);
        paint.setColorFilter(null);
        canvas.restore();

        if (player.hasShield && player.shieldCharges > 0) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5f);
            paint.setColor(0xAA82FFF0);
            canvas.drawOval(new RectF(player.x - 12, player.y - 8, player.x + Player.WIDTH + 12,
                    player.y + Player.HEIGHT + 9), paint);
            paint.setStyle(Paint.Style.FILL);
        }
        if (player.dashTimer > 0f) {
            paint.setColor(0x5579F7E7);
            for (int i = 1; i <= 3; i++) {
                float trailX = player.x - player.facing * i * 27f;
                canvas.drawOval(new RectF(trailX + 10, player.y + 20, trailX + 55, player.y + 88), paint);
            }
        }
    }

    private void drawWeather(Canvas canvas, GameEngine engine) {
        if (!"STORM".equals(engine.level.environment)) return;
        paint.setColor(0x28684845);
        for (int i = 0; i < 28; i++) {
            float x = (i * 173f + elapsed * 210f) % 1500f - 110f;
            float y = (i * 79f + elapsed * 36f) % 670f;
            canvas.save();
            canvas.rotate(-10f, x, y);
            canvas.drawOval(new RectF(x, y, x + 78f, y + 5f), paint);
            canvas.restore();
        }
        paint.setColor(0x162B1F2D);
        canvas.drawRect(0, 0, VIEW_W, VIEW_H, paint);
    }

    private void drawHud(Canvas canvas, GameEngine engine) {
        panel(canvas, 22, 18, 585, 64, 26, 0xD40C2C38);
        drawDrop(canvas, 55, 48, 0.52f);
        text(canvas, engine.waterCollected + "/" + engine.level.totalWater(), 80, 59, 25, Color.WHITE);
        drawHeart(canvas, 176, 48, 0.62f, 0xFFFF7869);
        text(canvas, String.valueOf(engine.lives), 201, 59, 25, Color.WHITE);
        text(canvas, "ОНОО", 251, 57, 18, 0xFF9CCBC8);
        text(canvas, String.valueOf(engine.score), 318, 59, 26, 0xFFFFD168);
        if (engine.level.requiredMarkers > 0) {
            drawTrailMarker(canvas, 455, 47);
            text(canvas, engine.markersCollected + "/" + engine.level.totalMarkers(), 484, 59, 24, Color.WHITE);
        }

        panel(canvas, 836, 24, 380, 50, 24, 0xB80C2C38);
        paint.setColor(0x553B777B);
        canvas.drawRoundRect(new RectF(866, 43, 1184, 56), 7, 7, paint);
        paint.setColor(0xFFF6BB52);
        canvas.drawRoundRect(new RectF(866, 43, 866 + 318 * engine.progress(), 56), 7, 7, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(866 + 318 * engine.progress(), 49.5f, 8, paint);
        text(canvas, "ҮЕ " + engine.level.id, 782, 58, 21, 0xFFFFD168);

        LevelData.Enemy boss = engine.boss();
        if (boss != null && boss.active && engine.player.x > 3880f) {
            panel(canvas, 394, 90, 492, 56, 22, 0xDF251E2B);
            centered(canvas, "ИХ УТАА • ҮЕ " + boss.phase(), 640, 113, 18, 0xFFFFD2CE);
            paint.setColor(0xFF493440);
            canvas.drawRoundRect(new RectF(448, 122, 832, 135), 7, 7, paint);
            paint.setColor(0xFFE45769);
            canvas.drawRoundRect(new RectF(448, 122, 448 + 384f * boss.health / boss.maxHealth, 135), 7, 7, paint);
        }
    }

    private void drawControls(Canvas canvas, InputController input, GameSettings settings, String abilityLabel) {
        float alpha = GameMath.clamp(settings.controlOpacity, 0.3f, 1f);
        float leftX = settings.leftHanded ? 1055f : 88f;
        float rightX = settings.leftHanded ? 1190f : 224f;
        float jumpX = settings.leftHanded ? 104f : 1170f;
        float abilityX = settings.leftHanded ? 254f : 1017f;
        drawControlCircle(canvas, leftX, 628, 57, input.leftHeld, alpha);
        drawControlCircle(canvas, rightX, 628, 57, input.rightHeld, alpha);
        drawControlCircle(canvas, jumpX, 617, 67, input.jumpHeld, alpha);
        boolean abilityAvailable = !abilityLabel.isEmpty();
        drawControlCircle(canvas, abilityX, 634, 51, input.abilityHeld,
                abilityAvailable ? alpha : alpha * 0.38f);

        paint.setColor(withAlpha(Color.WHITE, alpha));
        drawSideArrow(canvas, leftX, 628, -1);
        drawSideArrow(canvas, rightX, 628, 1);
        drawUpArrow(canvas, jumpX, 617);
        if (abilityAvailable) {
            centered(canvas, abilityLabel, abilityX, 640, 13, withAlpha(Color.WHITE, alpha));
        } else {
            centered(canvas, "ЧАДВАР", abilityX, 640, 12, withAlpha(0xFFDAE1DF, alpha * 0.6f));
        }
        panel(canvas, 1216, 92, 48, 48, 18, withAlpha(0xFF0C2C38, 0.74f));
        paint.setColor(0xEFFFFFFF);
        canvas.drawRect(1230, 105, 1236, 127, paint);
        canvas.drawRect(1244, 105, 1250, 127, paint);
    }

    private void drawControlCircle(Canvas canvas, float x, float y, float radius, boolean pressed, float alpha) {
        paint.setColor(withAlpha(pressed ? 0xFFF2B84F : 0xFF0D3440, pressed ? alpha : alpha * 0.72f));
        canvas.drawCircle(x, y, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(withAlpha(Color.WHITE, alpha * 0.72f));
        canvas.drawCircle(x, y, radius, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawSideArrow(Canvas canvas, float x, float y, int direction) {
        Path arrow = new Path();
        arrow.moveTo(x + direction * 25, y - 28);
        arrow.lineTo(x - direction * 20, y);
        arrow.lineTo(x + direction * 25, y + 28);
        arrow.close();
        canvas.drawPath(arrow, paint);
    }

    private void drawUpArrow(Canvas canvas, float x, float y) {
        Path arrow = new Path();
        arrow.moveTo(x, y - 37);
        arrow.lineTo(x - 31, y + 7);
        arrow.lineTo(x - 11, y + 7);
        arrow.lineTo(x - 11, y + 35);
        arrow.lineTo(x + 11, y + 35);
        arrow.lineTo(x + 11, y + 7);
        arrow.lineTo(x + 31, y + 7);
        arrow.close();
        canvas.drawPath(arrow, paint);
    }

    private void drawMessage(Canvas canvas, String message) {
        panel(canvas, 350, 158, 580, 58, 27, 0xDD0A2B38);
        centered(canvas, message, 640, 196, 22, Color.WHITE);
    }

    private void drawLevelIntro(Canvas canvas, GameEngine engine) {
        float alpha = GameMath.clamp(engine.introTimer, 0f, 1f);
        paint.setColor(withAlpha(0xFF071F2A, alpha * 0.72f));
        canvas.drawRect(0, 0, VIEW_W, VIEW_H, paint);
        centered(canvas, "ҮЕ " + engine.level.id, 640, 270, 25, withAlpha(0xFFFFD16E, alpha));
        centered(canvas, engine.level.titleMn, 640, 332, 48, withAlpha(Color.WHITE, alpha));
        centered(canvas, engine.level.title, 640, 374, 24, withAlpha(0xFFB9DBD7, alpha));
    }

    public void drawHero(Canvas canvas, RectF destination, boolean faceLeft) {
        canvas.save();
        if (faceLeft) canvas.scale(-1, 1, destination.centerX(), destination.centerY());
        float breathe = 1f + (float) Math.sin(elapsed * 2f) * 0.012f;
        canvas.scale(breathe, breathe, destination.centerX(), destination.bottom);
        canvas.drawBitmap(boldoo, null, destination, paint);
        canvas.restore();
    }

    public void drawLogo(Canvas canvas, float x, float y) {
        text(canvas, "BOLDOO", x, y, 72, 0xFFF5BD55);
        text(canvas, "GUARDIANS OF THE GOBI", x + 3, y + 54, 34, Color.WHITE);
        text(canvas, "ГОВИЙН ХАМГААЛАГЧИД", x + 5, y + 90, 21, 0xFFB8D8D4);
    }

    public void button(Canvas canvas, RectF bounds, String label, boolean enabled, boolean accent) {
        int color = !enabled ? 0x88515D60 : accent ? 0xEE0D9294 : 0xDB153E49;
        paint.setColor(color);
        canvas.drawRoundRect(bounds, 22, 22, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(enabled ? (accent ? 0xFF8FFFF0 : 0xAAFFFFFF) : 0x557A8586);
        canvas.drawRoundRect(bounds, 22, 22, paint);
        paint.setStyle(Paint.Style.FILL);
        centered(canvas, label, bounds.centerX(), bounds.centerY() + 10, 25,
                enabled ? Color.WHITE : 0xFF9CA9AA);
    }

    public void panel(Canvas canvas, float x, float y, float width, float height, float radius, int color) {
        paint.setColor(color);
        canvas.drawRoundRect(new RectF(x, y, x + width, y + height), radius, radius, paint);
    }

    public void toggle(Canvas canvas, float x, float y, boolean on) {
        paint.setColor(on ? 0xFF13A99C : 0xFF55646A);
        canvas.drawRoundRect(new RectF(x, y, x + 76, y + 38), 20, 20, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(on ? x + 57 : x + 19, y + 19, 15, paint);
    }

    public void drawStar(Canvas canvas, float x, float y, float radius, boolean filled) {
        Path star = new Path();
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            float r = i % 2 == 0 ? radius : radius * 0.44f;
            float px = x + (float) Math.cos(angle) * r;
            float py = y + (float) Math.sin(angle) * r;
            if (i == 0) star.moveTo(px, py); else star.lineTo(px, py);
        }
        star.close();
        paint.setColor(filled ? 0xFFFFCC55 : 0xFF586268);
        canvas.drawPath(star, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(filled ? 0xFFFFE9A2 : 0xFF899397);
        canvas.drawPath(star, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    public void drawLock(Canvas canvas, float x, float y, float scale) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(7f * scale);
        paint.setColor(0xFF9BA6A5);
        canvas.drawArc(new RectF(x - 15 * scale, y - 24 * scale, x + 15 * scale, y + 8 * scale),
                185, 170, false, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(x - 24 * scale, y, x + 24 * scale, y + 38 * scale),
                7 * scale, 7 * scale, paint);
        paint.setColor(0xFF455158);
        canvas.drawCircle(x, y + 17 * scale, 5 * scale, paint);
    }

    public void text(Canvas canvas, String value, float x, float y, float size, int color) {
        textPaint.setTextSize(size);
        textPaint.setColor(color);
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(value, x, y, textPaint);
    }

    public void centered(Canvas canvas, String value, float x, float y, float size, int color) {
        textPaint.setTextSize(size);
        textPaint.setColor(color);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(value, x, y, textPaint);
    }

    public void drawWaterIcon(Canvas canvas, float x, float y, float scale) {
        drawDrop(canvas, x, y, scale);
    }

    private void drawHeart(Canvas canvas, float x, float y, float scale, int color) {
        Path heart = new Path();
        heart.moveTo(x, y + 15 * scale);
        heart.cubicTo(x - 36 * scale, y - 5 * scale, x - 18 * scale, y - 27 * scale, x, y - 10 * scale);
        heart.cubicTo(x + 18 * scale, y - 27 * scale, x + 36 * scale, y - 5 * scale, x, y + 15 * scale);
        heart.close();
        paint.setColor(color);
        canvas.drawPath(heart, paint);
    }

    private void drawBolt(Canvas canvas, float x, float y, float scale) {
        Path bolt = new Path();
        bolt.moveTo(x + 4 * scale, y - 28 * scale);
        bolt.lineTo(x - 20 * scale, y + 4 * scale);
        bolt.lineTo(x - 3 * scale, y + 3 * scale);
        bolt.lineTo(x - 8 * scale, y + 29 * scale);
        bolt.lineTo(x + 22 * scale, y - 7 * scale);
        bolt.lineTo(x + 4 * scale, y - 6 * scale);
        bolt.close();
        paint.setColor(0xFFFFE178);
        canvas.drawPath(bolt, paint);
    }

    private void drawShield(Canvas canvas, float x, float y, float scale) {
        Path shield = new Path();
        shield.moveTo(x, y - 30 * scale);
        shield.lineTo(x + 27 * scale, y - 19 * scale);
        shield.lineTo(x + 23 * scale, y + 12 * scale);
        shield.quadTo(x, y + 34 * scale, x - 23 * scale, y + 12 * scale);
        shield.lineTo(x - 27 * scale, y - 19 * scale);
        shield.close();
        paint.setColor(0xFFD9FFF6);
        canvas.drawPath(shield, paint);
        paint.setColor(0xFF109E95);
        canvas.drawCircle(x, y - 3 * scale, 7 * scale, paint);
    }

    private void drawDownArrow(Canvas canvas, float x, float y, float scale) {
        Path arrow = new Path();
        arrow.moveTo(x - 9 * scale, y - 29 * scale);
        arrow.lineTo(x + 9 * scale, y - 29 * scale);
        arrow.lineTo(x + 9 * scale, y + 5 * scale);
        arrow.lineTo(x + 26 * scale, y + 5 * scale);
        arrow.lineTo(x, y + 31 * scale);
        arrow.lineTo(x - 26 * scale, y + 5 * scale);
        arrow.lineTo(x - 9 * scale, y + 5 * scale);
        arrow.close();
        paint.setColor(0xFFFFE178);
        canvas.drawPath(arrow, paint);
    }

    private static int withAlpha(int color, float alpha) {
        return (Math.round(GameMath.clamp(alpha, 0f, 1f) * 255f) << 24) | (color & 0x00FFFFFF);
    }
}
