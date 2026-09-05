package com.boldtechnology.boldoogame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.util.Locale;

public final class GameView extends View {
    private static final float VIEW_W = GameRenderer.VIEW_W;
    private static final float VIEW_H = GameRenderer.VIEW_H;
    private static final float FIXED_STEP = 1f / 60f;

    private final GameRenderer renderer;
    private final GameEngine engine;
    private final SaveManager saveManager;
    private final InputController input = new InputController();
    private final ParticleSystem particles = new ParticleSystem();
    private final GameAudio audio;
    private GameSettings settings;

    private ScreenState screen = ScreenState.SPLASH;
    private ScreenState settingsReturn = ScreenState.MAIN_MENU;
    private int currentLevel = 1;
    private float uiElapsed;
    private float splashTimer = 1.65f;
    private float accumulator;
    private long lastFrameNanos;
    private boolean appPaused;
    private boolean resetConfirmation;
    private boolean audioReleased;

    public GameView(Context context) {
        super(context);
        setFocusable(true);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        saveManager = new SaveManager(context);
        settings = saveManager.loadSettings();
        renderer = new GameRenderer(getResources());
        engine = new GameEngine(context);
        audio = new GameAudio(context, settings);
        audio.resume();
        lastFrameNanos = System.nanoTime();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = System.nanoTime();
        float frameTime = lastFrameNanos == 0L ? 0f
                : Math.min(0.1f, (now - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = now;
        if (appPaused) frameTime = 0f;

        uiElapsed += frameTime;
        renderer.setElapsed(uiElapsed);
        if (screen == ScreenState.SPLASH) {
            splashTimer -= frameTime;
            if (splashTimer <= 0f) screen = ScreenState.MAIN_MENU;
        }
        if (screen == ScreenState.PLAYING) updateGame(frameTime);
        else particles.update(frameTime);

        float scaleX = getWidth() / VIEW_W;
        float scaleY = getHeight() / VIEW_H;
        canvas.save();
        canvas.scale(scaleX, scaleY);
        drawScreen(canvas);
        canvas.restore();

        if (!appPaused) postInvalidateOnAnimation();
    }

    private void updateGame(float frameTime) {
        accumulator = Math.min(accumulator + frameTime, FIXED_STEP * 6f);
        while (accumulator >= FIXED_STEP && screen == ScreenState.PLAYING) {
            engine.update(FIXED_STEP, input);
            particles.update(FIXED_STEP);
            if (engine.player.landed) particles.dust(engine.player.centerX(), engine.player.y + Player.HEIGHT);
            for (GameEvent event : engine.drainEvents()) processEvent(event);
            if (engine.runState == GameEngine.RunState.COMPLETED) {
                saveManager.recordCompletion(engine.level.id, engine.resultScore, engine.elapsed,
                        engine.resultStars, engine.waterCollected);
                input.clear();
                screen = ScreenState.RESULTS;
            } else if (engine.runState == GameEngine.RunState.DEAD) {
                input.clear();
                screen = ScreenState.GAME_OVER;
            }
            accumulator -= FIXED_STEP;
        }
    }

    private void processEvent(GameEvent event) {
        audio.play(event.type);
        particles.emit(event);
        if (!settings.haptics) return;
        switch (event.type) {
            case HURT:
            case LIFE_LOST:
            case BARRIER:
            case BOSS_HIT:
            case COMPLETE:
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                break;
            case JUMP:
            case WATER:
            case TRAIL:
            case TRASH:
            case STOMP:
            case CHECKPOINT:
            case ABILITY:
            case SHIELD:
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                break;
        }
    }

    private void drawScreen(Canvas canvas) {
        switch (screen) {
            case SPLASH: drawSplash(canvas); break;
            case MAIN_MENU: drawMainMenu(canvas); break;
            case LEVEL_SELECT: drawLevelSelect(canvas); break;
            case PLAYING: renderer.drawGame(canvas, engine, settings, input, particles); break;
            case PAUSED: drawPaused(canvas); break;
            case SETTINGS: drawSettings(canvas); break;
            case RESULTS: drawResults(canvas); break;
            case GAME_OVER: drawGameOver(canvas); break;
            case FINAL_VICTORY: drawFinalVictory(canvas); break;
            case CREDITS: drawCredits(canvas); break;
        }
    }

    private void drawSplash(Canvas canvas) {
        renderer.drawMenuBackdrop(canvas);
        renderer.drawHero(canvas, new RectF(500, 150, 780, 430), false);
        renderer.centered(canvas, "BOLDOO", 640, 505, 62, 0xFFF6BF59);
        renderer.centered(canvas, "GUARDIANS OF THE GOBI", 640, 550, 29, Color.WHITE);
        renderer.centered(canvas, "BOLD TECHNOLOGY SOLUTIONS", 640, 604, 16, 0xFFBBD6D3);
        renderer.panel(canvas, 470, 640, 340, 10, 5, 0x554A7377);
        float progress = GameMath.clamp(1f - splashTimer / 1.65f, 0f, 1f);
        renderer.panel(canvas, 470, 640, 340 * progress, 10, 5, 0xFFF1BE57);
    }

    private void drawMainMenu(Canvas canvas) {
        renderer.drawMenuBackdrop(canvas);
        renderer.drawHero(canvas, new RectF(92, 190, 430, 552), false);
        renderer.drawLogo(canvas, 480, 145);
        renderer.centered(canvas, "Говийн ус, амьтад, цэнхэр тэнгэрийг хамгаал", 785, 282, 21, 0xFFE0E8D7);
        renderer.button(canvas, rect(560, 322, 450, 70), "АЯЛЛЫГ ЭХЛҮҮЛЭХ", true, true);
        renderer.button(canvas, rect(560, 410, 450, 64), "ҮЕ СОНГОХ", true, false);
        renderer.button(canvas, rect(560, 490, 216, 60), "ТОХИРГОО", true, false);
        renderer.button(canvas, rect(794, 490, 216, 60), "БҮТЭЭГЧИД", true, false);
        renderer.drawWaterIcon(canvas, 572, 607, 0.48f);
        renderer.text(canvas, "НИЙТ УС  " + saveManager.getTotalWater(), 600, 616, 21, 0xFFF5D47E);
        renderer.text(canvas, "v0.2.0 • OFFLINE", 850, 616, 17, 0xFFB4CECB);
    }

    private void drawLevelSelect(Canvas canvas) {
        renderer.drawMenuBackdrop(canvas);
        renderer.centered(canvas, "ҮЕ СОНГОХ", 640, 92, 43, 0xFFF4C25F);
        renderer.centered(canvas, "Нээгдсэн үеэ сонгоод Говийн аяллаа үргэлжлүүл", 640, 132, 20, 0xFFD0DFDD);
        int unlocked = saveManager.getUnlockedLevel();
        drawLevelCard(canvas, 1, 92, 178, "БАЯНБҮРДИЙН АВРАЛ", "Усны 10 дуслыг цуглуул", unlocked >= 1);
        drawLevelCard(canvas, 2, 470, 178, "НҮҮДЛИЙН ЗАМ", "Замын тэмдгийг сэргээ", unlocked >= 2);
        drawLevelCard(canvas, 3, 848, 178, "ГОВИЙН ШУУРГА", "Их Утааг ял", unlocked >= 3);
        renderer.button(canvas, rect(45, 626, 180, 55), "БУЦАХ", true, false);
    }

    private void drawLevelCard(Canvas canvas, int levelId, float x, float y,
                               String title, String objective, boolean unlocked) {
        int color = unlocked ? 0xE5143B47 : 0xD52A3439;
        renderer.panel(canvas, x, y, 340, 390, 28, color);
        renderer.panel(canvas, x + 18, y + 18, 304, 118, 20,
                levelId == 1 ? 0xFF1B7F83 : levelId == 2 ? 0xFF69703C : 0xFF5E4055);
        renderer.centered(canvas, "ҮЕ " + levelId, x + 170, y + 62, 20, 0xFFFFD46E);
        renderer.centered(canvas, levelId == 1 ? "OASIS" : levelId == 2 ? "MIGRATION" : "STORM",
                x + 170, y + 106, 29, Color.WHITE);
        if (!unlocked) {
            renderer.drawLock(canvas, x + 170, y + 185, 1.2f);
            renderer.centered(canvas, "ӨМНӨХ ҮЕИЙГ ДУУСГА", x + 170, y + 275, 17, 0xFF9FACAE);
            return;
        }
        renderer.centered(canvas, title, x + 170, y + 181, 20, Color.WHITE);
        renderer.centered(canvas, objective, x + 170, y + 216, 17, 0xFFC9DCDA);
        int stars = saveManager.getBestStars(levelId);
        for (int i = 0; i < 3; i++) renderer.drawStar(canvas, x + 125 + i * 46, y + 255, 20, i < stars);
        int best = saveManager.getBestScore(levelId);
        float bestTime = saveManager.getBestTime(levelId);
        renderer.centered(canvas, best > 0 ? "ШИЛДЭГ ОНОО  " + best : "ШИНЭ АЯЛАЛ",
                x + 170, y + 303, 17, 0xFFFFD47C);
        if (bestTime > 0f) renderer.centered(canvas, "ХУГАЦАА  " + formatTime(bestTime), x + 170, y + 327, 15, 0xFFABC5C4);
        renderer.button(canvas, rect(x + 64, y + 337, 212, 48), "ТОГЛОХ", true, true);
    }

    private void drawPaused(Canvas canvas) {
        renderer.drawGame(canvas, engine, settings, input, particles);
        renderer.panel(canvas, 0, 0, VIEW_W, VIEW_H, 0, 0xB90A202A);
        renderer.panel(canvas, 390, 102, 500, 532, 34, 0xF0133742);
        renderer.centered(canvas, "ТҮР ЗОГССОН", 640, 180, 42, 0xFFF5C25C);
        renderer.centered(canvas, engine.level.titleMn, 640, 220, 20, 0xFFC7DAD8);
        renderer.button(canvas, rect(462, 262, 356, 65), "ҮРГЭЛЖЛҮҮЛЭХ", true, true);
        renderer.button(canvas, rect(462, 345, 356, 62), "ҮЕИЙГ ДАХИН ЭХЛЭХ", true, false);
        renderer.button(canvas, rect(462, 425, 356, 62), "ТОХИРГОО", true, false);
        renderer.button(canvas, rect(462, 505, 356, 62), "ҮЕ СОНГОХ", true, false);
        renderer.centered(canvas, "Урагшилсан checkpoint энэ тоглолтод хадгалагдана", 640, 604, 16, 0xFF9DB9B6);
    }

    private void drawSettings(Canvas canvas) {
        renderer.drawMenuBackdrop(canvas);
        renderer.panel(canvas, 275, 54, 730, 610, 34, 0xEF123642);
        renderer.centered(canvas, "ТОХИРГОО", 640, 111, 40, 0xFFF4C25F);
        settingsRow(canvas, "ДУУНЫ ЭФФЕКТ", "Үсрэлт, цуглуулга, мөргөлдөөн", 155, settings.soundEffects);
        settingsRow(canvas, "ОРЧНЫ ДУУ", "Говийн намуухан салхи", 235, settings.ambientSound);
        settingsRow(canvas, "ЧИЧИРГЭЭ", "Үйлдэл бүрийн мэдрэмж", 315, settings.haptics);
        settingsRow(canvas, "ЗҮҮН ГАРЫН УДИРДЛАГА", "Хөдөлгөөн ба үйлдлийн талыг солино", 395, settings.leftHanded);
        renderer.text(canvas, "ТОВЧНЫ ТОД БАЙДАЛ", 335, 493, 21, Color.WHITE);
        renderer.text(canvas, Math.round(settings.controlOpacity * 100) + "%", 828, 493, 20, 0xFFFFD06B);
        renderer.panel(canvas, 690, 472, 118, 18, 9, 0xFF52666B);
        renderer.panel(canvas, 690, 472, 118 * settings.controlOpacity, 18, 9, 0xFF16A79C);
        renderer.button(canvas, rect(333, 526, 290, 56), "ЯВЦЫГ ЦЭВЭРЛЭХ", true, false);
        renderer.button(canvas, rect(657, 526, 290, 56), "БУЦАХ", true, true);
        if (resetConfirmation) drawResetConfirmation(canvas);
    }

    private void settingsRow(Canvas canvas, String title, String subtitle, float y, boolean on) {
        renderer.text(canvas, title, 335, y, 21, Color.WHITE);
        renderer.text(canvas, subtitle, 335, y + 27, 15, 0xFF9FB9B7);
        renderer.toggle(canvas, 832, y - 25, on);
    }

    private void drawResetConfirmation(Canvas canvas) {
        renderer.panel(canvas, 0, 0, VIEW_W, VIEW_H, 0, 0xB8122026);
        renderer.panel(canvas, 370, 205, 540, 285, 30, 0xFF173A45);
        renderer.centered(canvas, "ЯВЦЫГ ЦЭВЭРЛЭХ ҮҮ?", 640, 270, 30, 0xFFFFC760);
        renderer.centered(canvas, "Оноо, од, нээгдсэн үе бүгд арилна.", 640, 315, 19, Color.WHITE);
        renderer.centered(canvas, "Тохиргоо хэвээр үлдэнэ.", 640, 347, 17, 0xFFAEC5C3);
        renderer.button(canvas, rect(415, 390, 205, 58), "БОЛИХ", true, false);
        renderer.button(canvas, rect(660, 390, 205, 58), "ЦЭВЭРЛЭХ", true, true);
    }

    private void drawResults(Canvas canvas) {
        renderer.drawMenuBackdrop(canvas);
        renderer.panel(canvas, 310, 70, 660, 575, 36, 0xF0123541);
        renderer.centered(canvas, "ҮЕ АМЖИЛТТАЙ!", 640, 137, 39, 0xFFF5C25C);
        renderer.centered(canvas, engine.level.titleMn, 640, 177, 21, Color.WHITE);
        for (int i = 0; i < 3; i++) renderer.drawStar(canvas, 550 + i * 90, 244, 37, i < engine.resultStars);
        renderer.centered(canvas, engine.resultStars + " / 3 ОД", 640, 302, 20, 0xFFFFDE89);
        resultLine(canvas, "ОНОО", String.valueOf(engine.resultScore), 351);
        resultLine(canvas, "ХУГАЦАА", formatTime(engine.elapsed), 394);
        resultLine(canvas, "УС", engine.waterCollected + " / " + engine.level.totalWater(), 437);
        renderer.button(canvas, rect(385, 489, 238, 62), "ДАХИН ТОГЛОХ", true, false);
        renderer.button(canvas, rect(657, 489, 238, 62), engine.level.id < 3 ? "ДАРААГИЙН ҮЕ" : "ТӨГСГӨЛ", true, true);
        renderer.button(canvas, rect(520, 568, 240, 50), "ҮЕ СОНГОХ", true, false);
    }

    private void resultLine(Canvas canvas, String label, String value, float y) {
        renderer.text(canvas, label, 448, y, 19, 0xFFA9C2BF);
        renderer.text(canvas, value, 686, y, 23, Color.WHITE);
    }

    private void drawGameOver(Canvas canvas) {
        renderer.drawMenuBackdrop(canvas);
        renderer.panel(canvas, 360, 110, 560, 490, 34, 0xF01B303A);
        renderer.centered(canvas, "АЯЛАЛ ТҮР ЗОГСЛОО", 640, 192, 36, 0xFFFFB55E);
        renderer.centered(canvas, "Болдоо бууж өгдөггүй.", 640, 239, 22, Color.WHITE);
        renderer.centered(canvas, "Checkpoint: " + engine.checkpointNumber, 640, 283, 18, 0xFFAAC4C1);
        renderer.centered(canvas, "ОНОО  " + engine.score, 640, 335, 25, 0xFFFFD171);
        renderer.button(canvas, rect(450, 382, 380, 65), "ДАХИН ОРОЛДОХ", true, true);
        renderer.button(canvas, rect(450, 465, 380, 62), "ҮЕ СОНГОХ", true, false);
        renderer.button(canvas, rect(520, 543, 240, 42), "ҮНДСЭН ЦЭС", true, false);
    }

    private void drawFinalVictory(Canvas canvas) {
        renderer.drawBackdrop(canvas, 0, VIEW_W, "OASIS");
        renderer.panel(canvas, 0, 0, VIEW_W, VIEW_H, 0, 0x72052935);
        renderer.drawHero(canvas, new RectF(125, 225, 485, 600), false);
        renderer.text(canvas, "ГОВИЙН ТЭНГЭР", 515, 177, 53, 0xFFF6C35E);
        renderer.text(canvas, "ДАХИН ЦЭЛМЭЛЭЭ", 515, 235, 53, Color.WHITE);
        renderer.text(canvas, "Болдоо, Хулан, Тахь, Хавтгай дөрвөн хамгаалагч", 520, 304, 21, 0xFFD8E7D9);
        renderer.text(canvas, "ус, нүүдлийн зам, цэнхэр тэнгэрээ аварлаа.", 520, 337, 21, 0xFFD8E7D9);
        renderer.centered(canvas, "НИЙТ ЦУГЛУУЛСАН УС  " + saveManager.getTotalWater(), 755, 410, 23, 0xFFFFD579);
        renderer.button(canvas, rect(528, 459, 454, 66), "БҮТЭЭГЧДИЙГ ҮЗЭХ", true, true);
        renderer.button(canvas, rect(528, 544, 454, 60), "ҮНДСЭН ЦЭС", true, false);
    }

    private void drawCredits(Canvas canvas) {
        renderer.drawMenuBackdrop(canvas);
        renderer.panel(canvas, 215, 55, 850, 610, 36, 0xEF123642);
        renderer.centered(canvas, "БҮТЭЭГЧИД", 640, 118, 40, 0xFFF5C25C);
        renderer.centered(canvas, "BOLDOO: GUARDIANS OF THE GOBI", 640, 163, 22, Color.WHITE);
        credit(canvas, "ӨГҮҮЛЭЛ БА ДҮРҮҮД", "Oyunbold Ganbold", 225);
        credit(canvas, "ХӨГЖҮҮЛЭЛТ", "Bold Technology Solutions", 305);
        credit(canvas, "ТЕХНОЛОГИ", "Native Android • Java • Canvas", 385);
        credit(canvas, "ДУУ БА ДҮРС", "Тоглоомд зориулсан эх бүтээл", 465);
        renderer.centered(canvas, "Mario/Nintendo-ийн хөрөнгө, нэр, хөгжим ашиглаагүй.", 640, 530, 16, 0xFF9EB8B5);
        renderer.centered(canvas, "Говийг хайрлан хамгаалъя.", 640, 570, 21, 0xFFFFD174);
        renderer.button(canvas, rect(510, 600, 260, 48), "БУЦАХ", true, true);
    }

    private void credit(Canvas canvas, String role, String name, float y) {
        renderer.centered(canvas, role, 640, y, 16, 0xFF9CB9B7);
        renderer.centered(canvas, name, 640, y + 34, 22, Color.WHITE);
    }

    private void startLevel(int levelId) {
        currentLevel = Math.max(1, Math.min(3, levelId));
        engine.startLevel(currentLevel);
        particles.clear();
        input.clear();
        accumulator = 0f;
        screen = ScreenState.PLAYING;
        haptic(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    private void pauseGame() {
        if (screen != ScreenState.PLAYING) return;
        input.clear();
        screen = ScreenState.PAUSED;
        haptic(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    private void resumeGame() {
        if (screen != ScreenState.PAUSED) return;
        input.clear();
        accumulator = 0f;
        lastFrameNanos = System.nanoTime();
        screen = ScreenState.PLAYING;
        haptic(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    private void openSettings(ScreenState returnTo) {
        settingsReturn = returnTo;
        resetConfirmation = false;
        input.clear();
        screen = ScreenState.SETTINGS;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX(event.getActionIndex()) * VIEW_W / Math.max(1f, getWidth());
        float y = event.getY(event.getActionIndex()) * VIEW_H / Math.max(1f, getHeight());
        int action = event.getActionMasked();

        if (screen == ScreenState.SPLASH && action == MotionEvent.ACTION_DOWN) {
            splashTimer = 0f;
            screen = ScreenState.MAIN_MENU;
            return true;
        }
        if (screen == ScreenState.PLAYING) {
            if (action == MotionEvent.ACTION_DOWN && x >= 1190f && y >= 70f && y <= 160f) {
                pauseGame();
                return true;
            }
            updateGameplayPointers(event);
            return true;
        }
        if (action != MotionEvent.ACTION_DOWN) return true;

        switch (screen) {
            case MAIN_MENU: handleMainMenu(x, y); break;
            case LEVEL_SELECT: handleLevelSelect(x, y); break;
            case PAUSED: handlePauseMenu(x, y); break;
            case SETTINGS: handleSettings(x, y); break;
            case RESULTS: handleResults(x, y); break;
            case GAME_OVER: handleGameOver(x, y); break;
            case FINAL_VICTORY:
                if (hit(x, y, 528, 459, 454, 66)) screen = ScreenState.CREDITS;
                else if (hit(x, y, 528, 544, 454, 60)) screen = ScreenState.MAIN_MENU;
                break;
            case CREDITS:
                if (hit(x, y, 510, 590, 260, 65)) screen = ScreenState.MAIN_MENU;
                break;
            default: break;
        }
        return true;
    }

    private void handleMainMenu(float x, float y) {
        if (hit(x, y, 560, 322, 450, 70)) startLevel(Math.min(saveManager.getUnlockedLevel(), 3));
        else if (hit(x, y, 560, 410, 450, 64)) screen = ScreenState.LEVEL_SELECT;
        else if (hit(x, y, 560, 490, 216, 60)) openSettings(ScreenState.MAIN_MENU);
        else if (hit(x, y, 794, 490, 216, 60)) screen = ScreenState.CREDITS;
    }

    private void handleLevelSelect(float x, float y) {
        if (hit(x, y, 45, 610, 190, 80)) {
            screen = ScreenState.MAIN_MENU;
            return;
        }
        int level = x < 432 ? 1 : x < 810 ? 2 : 3;
        float cardX = level == 1 ? 92 : level == 2 ? 470 : 848;
        if (hit(x, y, cardX, 178, 340, 410) && saveManager.getUnlockedLevel() >= level) startLevel(level);
    }

    private void handlePauseMenu(float x, float y) {
        if (hit(x, y, 462, 262, 356, 65)) resumeGame();
        else if (hit(x, y, 462, 345, 356, 62)) startLevel(currentLevel);
        else if (hit(x, y, 462, 425, 356, 62)) openSettings(ScreenState.PAUSED);
        else if (hit(x, y, 462, 505, 356, 62)) screen = ScreenState.LEVEL_SELECT;
    }

    private void handleSettings(float x, float y) {
        if (resetConfirmation) {
            if (hit(x, y, 415, 390, 205, 58)) resetConfirmation = false;
            else if (hit(x, y, 660, 390, 205, 58)) {
                saveManager.resetProgress();
                resetConfirmation = false;
                haptic(HapticFeedbackConstants.LONG_PRESS);
            }
            return;
        }
        if (hit(x, y, 800, 120, 140, 75)) settings.soundEffects = !settings.soundEffects;
        else if (hit(x, y, 800, 200, 140, 75)) settings.ambientSound = !settings.ambientSound;
        else if (hit(x, y, 800, 280, 140, 75)) settings.haptics = !settings.haptics;
        else if (hit(x, y, 800, 360, 140, 75)) settings.leftHanded = !settings.leftHanded;
        else if (hit(x, y, 660, 445, 270, 75)) settings.cycleOpacity();
        else if (hit(x, y, 333, 526, 290, 56)) {
            resetConfirmation = true;
            return;
        } else if (hit(x, y, 657, 526, 290, 56)) {
            saveManager.saveSettings(settings);
            audio.setSettings(settings);
            screen = settingsReturn;
            return;
        }
        saveManager.saveSettings(settings);
        audio.setSettings(settings);
        haptic(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    private void handleResults(float x, float y) {
        if (hit(x, y, 385, 489, 238, 62)) startLevel(currentLevel);
        else if (hit(x, y, 657, 489, 238, 62)) {
            if (currentLevel < 3) startLevel(currentLevel + 1);
            else screen = ScreenState.FINAL_VICTORY;
        } else if (hit(x, y, 500, 555, 280, 80)) screen = ScreenState.LEVEL_SELECT;
    }

    private void handleGameOver(float x, float y) {
        if (hit(x, y, 450, 382, 380, 65)) startLevel(currentLevel);
        else if (hit(x, y, 450, 465, 380, 62)) screen = ScreenState.LEVEL_SELECT;
        else if (hit(x, y, 500, 530, 280, 70)) screen = ScreenState.MAIN_MENU;
    }

    private void updateGameplayPointers(MotionEvent event) {
        int lifted = (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_POINTER_UP)
                ? event.getActionIndex() : -1;
        boolean left = false;
        boolean right = false;
        boolean jump = false;
        boolean ability = false;
        for (int index = 0; index < event.getPointerCount(); index++) {
            if (index == lifted) continue;
            float x = event.getX(index) * VIEW_W / Math.max(1f, getWidth());
            float y = event.getY(index) * VIEW_H / Math.max(1f, getHeight());
            if (y < 505f) continue;
            if (settings.leftHanded) {
                if (x < 180f) jump = true;
                else if (x < 340f) ability = true;
                else if (x > 1120f) right = true;
                else if (x > 970f) left = true;
            } else {
                if (x < 160f) left = true;
                else if (x < 320f) right = true;
                else if (x > 1090f) jump = true;
                else if (x > 930f) ability = true;
            }
        }
        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) left = right = jump = ability = false;
        input.setState(left, right, jump, ability);
    }

    public boolean handleBackPressed() {
        if (screen == ScreenState.PLAYING) {
            pauseGame();
            return true;
        }
        if (screen == ScreenState.PAUSED) {
            resumeGame();
            return true;
        }
        if (screen == ScreenState.SETTINGS) {
            if (resetConfirmation) resetConfirmation = false;
            else {
                saveManager.saveSettings(settings);
                audio.setSettings(settings);
                screen = settingsReturn;
            }
            return true;
        }
        if (screen == ScreenState.LEVEL_SELECT || screen == ScreenState.CREDITS
                || screen == ScreenState.RESULTS || screen == ScreenState.GAME_OVER
                || screen == ScreenState.FINAL_VICTORY) {
            screen = ScreenState.MAIN_MENU;
            return true;
        }
        if (screen == ScreenState.SPLASH) {
            screen = ScreenState.MAIN_MENU;
            return true;
        }
        return false;
    }

    public void setPaused(boolean paused) {
        appPaused = paused;
        input.clear();
        if (paused) {
            if (screen == ScreenState.PLAYING) screen = ScreenState.PAUSED;
            audio.pause();
        } else {
            lastFrameNanos = System.nanoTime();
            audio.resume();
            postInvalidateOnAnimation();
        }
    }

    public void release() {
        if (!audioReleased) {
            audioReleased = true;
            audio.release();
        }
    }

    private void haptic(int constant) {
        if (settings.haptics) performHapticFeedback(constant);
    }

    private static RectF rect(float x, float y, float width, float height) {
        return new RectF(x, y, x + width, y + height);
    }

    private static boolean hit(float x, float y, float left, float top, float width, float height) {
        return x >= left && x <= left + width && y >= top && y <= top + height;
    }

    private static String formatTime(float seconds) {
        int total = Math.max(0, Math.round(seconds));
        return String.format(Locale.US, "%d:%02d", total / 60, total % 60);
    }
}
