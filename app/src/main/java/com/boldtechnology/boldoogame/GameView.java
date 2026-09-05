package com.boldtechnology.boldoogame;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public final class GameView extends View {
    private static final float VIEW_W = 1280f;
    private static final float VIEW_H = 720f;
    private static final float WORLD_W = 2440f;
    private static final float PLAYER_W = 104f;
    private static final float PLAYER_H = 118f;
    private static final float GRAVITY = 1500f;
    private static final float RUN_SPEED = 350f;
    private static final float JUMP_SPEED = 680f;

    private static final int TITLE = 0;
    private static final int PLAYING = 1;
    private static final int WON = 2;
    private static final int GAME_OVER = 3;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Bitmap background;
    private final Bitmap boldoo;
    private final Bitmap smogling;
    private final List<RectF> platforms = new ArrayList<>();
    private final List<Drop> drops = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();

    private float playerX;
    private float playerY;
    private float velocityX;
    private float velocityY;
    private float cameraX;
    private float elapsed;
    private float invincible;
    private float messageTimer;
    private long lastFrameNanos;
    private int facing = 1;
    private int state = TITLE;
    private int score;
    private int lives;
    private int collected;
    private boolean onGround;
    private boolean leftHeld;
    private boolean rightHeld;
    private boolean jumpHeld;
    private boolean jumpWasHeld;
    private boolean paused;

    public GameView(Context context) {
        super(context);
        setFocusable(true);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        background = BitmapFactory.decodeResource(getResources(), R.drawable.gobi_background);
        boldoo = BitmapFactory.decodeResource(getResources(), R.drawable.boldoo_player);
        smogling = BitmapFactory.decodeResource(getResources(), R.drawable.smogling_enemy);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        textPaint.setShadowLayer(4f, 0f, 2f, 0xAA082733);
        createLevel();
        resetGame();
    }

    private void createLevel() {
        platforms.clear();
        platforms.add(new RectF(0, 610, WORLD_W, 740));
        platforms.add(new RectF(300, 500, 235, 28));
        platforms.add(new RectF(650, 430, 245, 28));
        platforms.add(new RectF(980, 500, 230, 28));
        platforms.add(new RectF(1280, 410, 245, 28));
        platforms.add(new RectF(1630, 500, 225, 28));
        platforms.add(new RectF(1950, 420, 230, 28));
        platforms.add(new RectF(2200, 520, 180, 28));

        drops.clear();
        drops.add(new Drop(175, 548));
        drops.add(new Drop(415, 438));
        drops.add(new Drop(760, 368));
        drops.add(new Drop(905, 548));
        drops.add(new Drop(1085, 438));
        drops.add(new Drop(1395, 348));
        drops.add(new Drop(1560, 548));
        drops.add(new Drop(1735, 438));
        drops.add(new Drop(2050, 358));
        drops.add(new Drop(2290, 458));

        enemies.clear();
        enemies.add(new Enemy(560, 532, 500, 640, 82));
        enemies.add(new Enemy(1070, 422, 1000, 1170, 72));
        enemies.add(new Enemy(1790, 532, 1690, 1880, 88));
    }

    private void resetGame() {
        playerX = 75;
        playerY = 610 - PLAYER_H;
        velocityX = 0;
        velocityY = 0;
        cameraX = 0;
        elapsed = 0;
        invincible = 0;
        score = 0;
        lives = 3;
        collected = 0;
        onGround = true;
        leftHeld = false;
        rightHeld = false;
        jumpHeld = false;
        jumpWasHeld = false;
        for (Drop drop : drops) drop.collected = false;
        for (Enemy enemy : enemies) enemy.active = true;
    }

    private void startGame() {
        resetGame();
        state = PLAYING;
        lastFrameNanos = System.nanoTime();
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    public void setPaused(boolean value) {
        paused = value;
        lastFrameNanos = System.nanoTime();
        if (!paused) postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0 ? 0f : Math.min(0.033f, (now - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = now;

        if (!paused && state == PLAYING) update(dt);

        float scaleX = getWidth() / VIEW_W;
        float scaleY = getHeight() / VIEW_H;
        canvas.save();
        canvas.scale(scaleX, scaleY);
        drawScene(canvas);
        canvas.restore();

        if (!paused) postInvalidateOnAnimation();
    }

    private void update(float dt) {
        elapsed += dt;
        if (invincible > 0) invincible -= dt;
        if (messageTimer > 0) messageTimer -= dt;

        if (leftHeld == rightHeld) {
            velocityX *= (float) Math.pow(0.0007, dt);
            if (Math.abs(velocityX) < 8) velocityX = 0;
        } else if (leftHeld) {
            velocityX = -RUN_SPEED;
            facing = -1;
        } else {
            velocityX = RUN_SPEED;
            facing = 1;
        }

        if (jumpHeld && !jumpWasHeld && onGround) {
            velocityY = -JUMP_SPEED;
            onGround = false;
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        jumpWasHeld = jumpHeld;

        float previousBottom = playerY + PLAYER_H;
        playerX += velocityX * dt;
        playerX = clamp(playerX, 0, WORLD_W - PLAYER_W);
        velocityY += GRAVITY * dt;
        playerY += velocityY * dt;
        resolvePlatformLanding(previousBottom);

        for (Enemy enemy : enemies) {
            if (!enemy.active) continue;
            enemy.x += enemy.speed * enemy.direction * dt;
            if (enemy.x < enemy.minX) {
                enemy.x = enemy.minX;
                enemy.direction = 1;
            } else if (enemy.x > enemy.maxX) {
                enemy.x = enemy.maxX;
                enemy.direction = -1;
            }
        }

        collectDrops();
        checkEnemies(previousBottom);

        if (playerY > VIEW_H + 80) loseLife();
        if (playerX > 2260 && collected == drops.size()) {
            state = WON;
            score += 1000 + lives * 250;
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        } else if (playerX > 2230 && collected < drops.size()) {
            messageTimer = 0.4f;
        }

        float desiredCamera = clamp(playerX - 390, 0, WORLD_W - VIEW_W);
        cameraX += (desiredCamera - cameraX) * Math.min(1f, dt * 5f);
    }

    private void resolvePlatformLanding(float previousBottom) {
        onGround = false;
        if (velocityY < 0) return;
        float currentBottom = playerY + PLAYER_H;
        float bestTop = Float.MAX_VALUE;
        for (RectF platform : platforms) {
            float left = platform.left;
            float top = platform.top;
            float right = platform.left + platform.right;
            if (playerX + PLAYER_W > left + 8
                    && playerX < right - 8
                    && previousBottom <= top + 12
                    && currentBottom >= top
                    && top < bestTop) {
                bestTop = top;
            }
        }
        if (bestTop < Float.MAX_VALUE) {
            playerY = bestTop - PLAYER_H;
            velocityY = 0;
            onGround = true;
        }
    }

    private void collectDrops() {
        RectF player = playerBounds();
        for (Drop drop : drops) {
            if (!drop.collected && RectF.intersects(player, new RectF(drop.x - 24, drop.y - 28, drop.x + 24, drop.y + 28))) {
                drop.collected = true;
                collected++;
                score += 100;
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }
        }
    }

    private void checkEnemies(float previousBottom) {
        RectF player = playerBounds();
        for (Enemy enemy : enemies) {
            if (!enemy.active) continue;
            RectF enemyBounds = new RectF(enemy.x, enemy.y, enemy.x + 78, enemy.y + 78);
            if (!RectF.intersects(player, enemyBounds)) continue;
            if (velocityY > 80 && previousBottom <= enemy.y + 24) {
                enemy.active = false;
                velocityY = -JUMP_SPEED * 0.55f;
                score += 250;
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            } else if (invincible <= 0) {
                loseLife();
                return;
            }
        }
    }

    private void loseLife() {
        if (invincible > 0) return;
        lives--;
        if (lives <= 0) {
            state = GAME_OVER;
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            return;
        }
        playerX = Math.max(45, playerX - 170);
        playerY = 610 - PLAYER_H;
        velocityX = 0;
        velocityY = 0;
        invincible = 2f;
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }

    private RectF playerBounds() {
        return new RectF(playerX + 18, playerY + 10, playerX + PLAYER_W - 18, playerY + PLAYER_H - 4);
    }

    private void drawScene(Canvas canvas) {
        drawBackground(canvas);
        if (state == TITLE) {
            drawTitle(canvas);
            return;
        }

        drawWorld(canvas);
        drawHud(canvas);
        drawControls(canvas);

        if (state == WON) drawOverlay(canvas, "ГОВИЙГ ХАМГААЛЛАА!", "Оноо: " + score, "ДАХИН ТОГЛОХ");
        if (state == GAME_OVER) drawOverlay(canvas, "АЯЛАЛ ДУУСЛАА", "Оноо: " + score, "ДАХИН ОРОЛДОХ");
    }

    private void drawBackground(Canvas canvas) {
        float travel = cameraX / Math.max(1f, WORLD_W - VIEW_W);
        int srcW = Math.min(background.getWidth(), Math.round(background.getHeight() * VIEW_W / VIEW_H));
        int maxOffset = Math.max(0, background.getWidth() - srcW);
        int srcLeft = Math.round(maxOffset * travel);
        Rect src = new Rect(srcLeft, 0, srcLeft + srcW, background.getHeight());
        canvas.drawBitmap(background, src, new RectF(0, 0, VIEW_W, VIEW_H), paint);
        paint.setColor(0x17052A35);
        canvas.drawRect(0, 0, VIEW_W, VIEW_H, paint);
    }

    private void drawWorld(Canvas canvas) {
        canvas.save();
        canvas.translate(-cameraX, 0);

        for (RectF platform : platforms) drawPlatform(canvas, platform);
        drawFinish(canvas);

        for (Drop drop : drops) if (!drop.collected) drawDrop(canvas, drop.x, drop.y);
        for (Enemy enemy : enemies) if (enemy.active) drawEnemy(canvas, enemy);

        if (invincible <= 0 || ((int) (invincible * 10)) % 2 == 0) drawPlayer(canvas);
        canvas.restore();

        if (messageTimer > 0 && collected < drops.size()) {
            drawPill(canvas, 430, 92, 420, 58, 0xDA0D2D3A);
            drawCenteredText(canvas, "Үлдсэн усны дусал: " + (drops.size() - collected), 640, 130, 28, Color.WHITE);
        }
    }

    private void drawPlatform(Canvas canvas, RectF data) {
        float left = data.left;
        float top = data.top;
        float right = data.left + data.right;
        float bottom = data.top + data.bottom;
        paint.setShader(new LinearGradient(0, top, 0, bottom, 0xFFF7C96F, 0xFF9A5B2C, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(left, top, right, bottom), 16, 16, paint);
        paint.setShader(null);
        paint.setColor(0xFF663A22);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        canvas.drawRoundRect(new RectF(left, top, right, bottom), 16, 16, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFEFD58A);
        canvas.drawRoundRect(new RectF(left + 5, top + 4, right - 5, top + 12), 8, 8, paint);
    }

    private void drawPlayer(Canvas canvas) {
        float bob = onGround && Math.abs(velocityX) > 20 ? (float) Math.sin(elapsed * 14) * 3f : 0f;
        RectF dst = new RectF(playerX, playerY + bob, playerX + PLAYER_W, playerY + PLAYER_H + bob);
        canvas.save();
        if (facing < 0) {
            canvas.scale(-1, 1, dst.centerX(), dst.centerY());
        }
        canvas.drawBitmap(boldoo, null, dst, paint);
        canvas.restore();
    }

    private void drawEnemy(Canvas canvas, Enemy enemy) {
        float bob = (float) Math.sin(elapsed * 4 + enemy.x * 0.01f) * 5f;
        RectF dst = new RectF(enemy.x, enemy.y + bob, enemy.x + 78, enemy.y + 78 + bob);
        canvas.save();
        if (enemy.direction > 0) canvas.scale(-1, 1, dst.centerX(), dst.centerY());
        canvas.drawBitmap(smogling, null, dst, paint);
        canvas.restore();
    }

    private void drawDrop(Canvas canvas, float x, float y) {
        float pulse = 1f + (float) Math.sin(elapsed * 5 + x) * 0.08f;
        canvas.save();
        canvas.scale(pulse, pulse, x, y);
        Path path = new Path();
        path.moveTo(x, y - 28);
        path.cubicTo(x + 8, y - 11, x + 22, y + 2, x + 22, y + 13);
        path.cubicTo(x + 22, y + 28, x + 11, y + 36, x, y + 36);
        path.cubicTo(x - 11, y + 36, x - 22, y + 28, x - 22, y + 13);
        path.cubicTo(x - 22, y + 2, x - 8, y - 11, x, y - 28);
        paint.setShader(new LinearGradient(x - 20, y - 25, x + 20, y + 35,
                0xFF8DF4FF, 0xFF0796C8, Shader.TileMode.CLAMP));
        canvas.drawPath(path, paint);
        paint.setShader(null);
        paint.setColor(0xFF075B78);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xBFFFFFFF);
        canvas.drawCircle(x - 7, y + 4, 5, paint);
        canvas.restore();
    }

    private void drawFinish(Canvas canvas) {
        float x = 2330;
        paint.setColor(0xFF5A3423);
        canvas.drawRoundRect(new RectF(x, 350, x + 12, 610), 5, 5, paint);
        paint.setColor(0xFF0D5265);
        Path flag = new Path();
        flag.moveTo(x + 10, 362);
        flag.lineTo(x + 150, 382);
        flag.lineTo(x + 10, 440);
        flag.close();
        canvas.drawPath(flag, paint);
        drawCenteredText(canvas, "БАРИА", x + 74, 405, 22, 0xFFF8D27A);
        paint.setColor(0xFF0A9EA6);
        canvas.drawOval(new RectF(x - 34, 574, x + 55, 625), paint);
        paint.setColor(0xAA8CF4FF);
        canvas.drawOval(new RectF(x - 23, 581, x + 42, 615), paint);
    }

    private void drawHud(Canvas canvas) {
        drawPill(canvas, 24, 22, 450, 58, 0xCC0D2D3A);
        drawText(canvas, "💧 " + collected + "/" + drops.size(), 48, 61, 27, Color.WHITE);
        drawText(canvas, "ОНОО  " + score, 184, 61, 27, 0xFFF7C96F);
        drawText(canvas, "АМЬ  " + lives, 342, 61, 27, Color.WHITE);

        float progress = clamp(playerX / (WORLD_W - PLAYER_W), 0, 1);
        paint.setColor(0x770D2D3A);
        canvas.drawRoundRect(new RectF(890, 34, 1240, 50), 8, 8, paint);
        paint.setColor(0xFFF4B552);
        canvas.drawRoundRect(new RectF(890, 34, 890 + 350 * progress, 50), 8, 8, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(890 + 350 * progress, 42, 9, paint);
    }

    private void drawControls(Canvas canvas) {
        drawControlCircle(canvas, 86, 625, 58, leftHeld);
        drawControlCircle(canvas, 222, 625, 58, rightHeld);
        drawControlCircle(canvas, 1170, 615, 68, jumpHeld);

        paint.setColor(Color.WHITE);
        Path leftArrow = new Path();
        leftArrow.moveTo(105, 596);
        leftArrow.lineTo(65, 625);
        leftArrow.lineTo(105, 654);
        leftArrow.close();
        canvas.drawPath(leftArrow, paint);

        Path rightArrow = new Path();
        rightArrow.moveTo(203, 596);
        rightArrow.lineTo(243, 625);
        rightArrow.lineTo(203, 654);
        rightArrow.close();
        canvas.drawPath(rightArrow, paint);

        Path jumpArrow = new Path();
        jumpArrow.moveTo(1170, 576);
        jumpArrow.lineTo(1138, 620);
        jumpArrow.lineTo(1159, 620);
        jumpArrow.lineTo(1159, 651);
        jumpArrow.lineTo(1181, 651);
        jumpArrow.lineTo(1181, 620);
        jumpArrow.lineTo(1202, 620);
        jumpArrow.close();
        canvas.drawPath(jumpArrow, paint);
    }

    private void drawControlCircle(Canvas canvas, float x, float y, float radius, boolean pressed) {
        paint.setColor(pressed ? 0xBFF4B552 : 0x770D2D3A);
        canvas.drawCircle(x, y, radius, paint);
        paint.setColor(0xAAFFFFFF);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        canvas.drawCircle(x, y, radius, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawTitle(Canvas canvas) {
        paint.setColor(0xA80A2632);
        canvas.drawRect(0, 0, VIEW_W, VIEW_H, paint);

        paint.setColor(0xFFF4B552);
        canvas.drawCircle(154, 115, 58, paint);
        paint.setColor(0xFF0D2D3A);
        canvas.drawCircle(176, 98, 54, paint);

        RectF hero = new RectF(90, 205, 430, 545);
        canvas.drawBitmap(boldoo, null, hero, paint);

        drawText(canvas, "BOLDOO", 470, 180, 74, 0xFFF4B552);
        drawText(canvas, "GUARDIANS OF THE GOBI", 470, 245, 42, Color.WHITE);
        drawText(canvas, "Говийн усыг хамгаалах анхны аялал", 474, 300, 26, 0xFFE8D8B9);

        drawPill(canvas, 500, 360, 470, 92, 0xEE0E8290);
        drawCenteredText(canvas, "ТОГЛОХ", 735, 420, 38, Color.WHITE);
        drawCenteredText(canvas, "← → хөдөлнө     ↑ үсэрнэ", 735, 505, 25, Color.WHITE);
        drawCenteredText(canvas, "10 усны дуслыг цуглуулаад баянбүрдэд хүрээрэй", 735, 552, 22, 0xFFF8D27A);
        drawCenteredText(canvas, "Bold Technology Solutions • MVP 0.1", 735, 668, 18, 0xFFCEE4E2);
    }

    private void drawOverlay(Canvas canvas, String title, String subtitle, String button) {
        paint.setColor(0xB20A2632);
        canvas.drawRect(0, 0, VIEW_W, VIEW_H, paint);
        drawCenteredText(canvas, title, 640, 264, 56, 0xFFF4B552);
        drawCenteredText(canvas, subtitle, 640, 330, 34, Color.WHITE);
        drawPill(canvas, 440, 392, 400, 92, 0xEE0E8290);
        drawCenteredText(canvas, button, 640, 452, 32, Color.WHITE);
        drawCenteredText(canvas, "Дэлгэц дээр дарж үргэлжлүүлнэ", 640, 530, 23, 0xFFE8D8B9);
    }

    private void drawPill(Canvas canvas, float x, float y, float width, float height, int color) {
        paint.setColor(color);
        canvas.drawRoundRect(new RectF(x, y, x + width, y + height), height / 2, height / 2, paint);
    }

    private void drawText(Canvas canvas, String text, float x, float y, float size, int color) {
        textPaint.setTextSize(size);
        textPaint.setColor(color);
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(text, x, y, textPaint);
    }

    private void drawCenteredText(Canvas canvas, String text, float x, float y, float size, int color) {
        textPaint.setTextSize(size);
        textPaint.setColor(color);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, x, y, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float scaleX = getWidth() / VIEW_W;
        float scaleY = getHeight() / VIEW_H;

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN && state != PLAYING) {
            startGame();
            return true;
        }

        leftHeld = false;
        rightHeld = false;
        jumpHeld = false;
        int lifted = event.getActionMasked() == MotionEvent.ACTION_POINTER_UP
                || event.getActionMasked() == MotionEvent.ACTION_UP ? event.getActionIndex() : -1;
        for (int i = 0; i < event.getPointerCount(); i++) {
            if (i == lifted) continue;
            float x = event.getX(i) / scaleX;
            float y = event.getY(i) / scaleY;
            if (y > 510 && x < 155) leftHeld = true;
            else if (y > 510 && x < 330) rightHeld = true;
            else if (y > 450 && x > 980) jumpHeld = true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            leftHeld = rightHeld = jumpHeld = false;
        }
        return true;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Drop {
        final float x;
        final float y;
        boolean collected;

        Drop(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class Enemy {
        float x;
        final float y;
        final float minX;
        final float maxX;
        final float speed;
        int direction = -1;
        boolean active = true;

        Enemy(float x, float y, float minX, float maxX, float speed) {
            this.x = x;
            this.y = y;
            this.minX = minX;
            this.maxX = maxX;
            this.speed = speed;
        }
    }
}
