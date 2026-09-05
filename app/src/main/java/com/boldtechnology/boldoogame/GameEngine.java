package com.boldtechnology.boldoogame;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class GameEngine {
    public static final float VIEW_WIDTH = 1280f;
    public static final float VIEW_HEIGHT = 720f;

    public enum RunState { RUNNING, COMPLETED, DEAD }

    private final LevelLoader levelLoader;
    private final Random random = new Random(73421L);
    private final List<GameEvent> events = new ArrayList<>();
    public final List<LevelData.FallingRock> rocks = new ArrayList<>();
    public final Player player = new Player();
    public final Camera2D camera = new Camera2D();

    public LevelData level;
    public RunState runState = RunState.RUNNING;
    public float elapsed;
    public int score;
    public int lives;
    public int waterCollected;
    public int markersCollected;
    public int trashCollected;
    public int enemiesDefeated;
    public int resultScore;
    public int resultStars;
    public String message = "";
    public float messageTimer;
    public float introTimer;
    public float checkpointX;
    public float checkpointY;
    public int checkpointNumber;

    public GameEngine(Context context) {
        levelLoader = new LevelLoader(context);
    }

    public void startLevel(int levelId) {
        level = levelLoader.load(levelId);
        runState = RunState.RUNNING;
        elapsed = 0f;
        score = 0;
        lives = 3;
        waterCollected = 0;
        markersCollected = 0;
        trashCollected = 0;
        enemiesDefeated = 0;
        resultScore = 0;
        resultStars = 0;
        message = level.objectiveMn;
        messageTimer = 4.2f;
        introTimer = 2.1f;
        checkpointX = level.startX;
        checkpointY = level.startY;
        checkpointNumber = 0;
        rocks.clear();
        events.clear();
        random.setSeed(73421L + levelId * 991L);
        player.reset(level.startX, level.startY);
        camera.reset();
    }

    public void update(float dt, InputController input) {
        if (level == null || runState != RunState.RUNNING) return;
        elapsed += dt;
        messageTimer = Math.max(0f, messageTimer - dt);
        introTimer = Math.max(0f, introTimer - dt);

        for (LevelData.Platform platform : level.platforms) platform.update(elapsed);
        float wind = windAtPlayer();
        player.update(dt, input, level, wind);
        emitPlayerActions();

        collectItems();
        collectAbilities();
        activateCheckpoints();
        updateEnemies(dt);
        updateHazards(dt);
        handleBarrierAbilities();

        if (player.y > VIEW_HEIGHT + 180f) loseLife(true);
        checkFinish();
        camera.update(player.x, player.velocityX, level.worldWidth, VIEW_WIDTH, dt);
    }

    private float windAtPlayer() {
        float push = 0f;
        for (LevelData.Hazard hazard : level.hazards) {
            if (LevelData.Hazard.WIND.equals(hazard.type)
                    && player.intersects(hazard.x, hazard.y, hazard.width, hazard.height)) {
                push += hazard.strength;
            }
        }
        return push;
    }

    private void emitPlayerActions() {
        if (player.jumpStarted) addEvent(GameEvent.Type.JUMP, player.centerX(), player.y + Player.HEIGHT);
        if (player.dashStarted) {
            addEvent(GameEvent.Type.ABILITY, player.centerX(), player.centerY());
            showMessage("Хулангийн хурд: түргэн дайралт!", 2.1f);
        }
        if (player.groundPoundStarted) addEvent(GameEvent.Type.ABILITY, player.centerX(), player.centerY());
        if (player.groundPoundImpact) {
            addEvent(GameEvent.Type.BARRIER, player.centerX(), player.y + Player.HEIGHT);
            camera.shake(11f, 0.28f);
        }
    }

    private void collectItems() {
        for (LevelData.Collectible item : level.collectibles) {
            if (item.collected || !player.intersects(item.x - 27f, item.y - 30f, 54f, 62f)) continue;
            item.collected = true;
            if (LevelData.Collectible.WATER.equals(item.type)) {
                waterCollected++;
                score += 100;
                addEvent(GameEvent.Type.WATER, item.x, item.y);
            } else if (LevelData.Collectible.TRAIL.equals(item.type)) {
                markersCollected++;
                score += 140;
                addEvent(GameEvent.Type.TRAIL, item.x, item.y);
            } else {
                trashCollected++;
                score += 90;
                addEvent(GameEvent.Type.TRASH, item.x, item.y);
            }
        }
    }

    private void collectAbilities() {
        for (LevelData.AbilityPickup ability : level.abilities) {
            if (ability.collected || !player.intersects(ability.x - 32f, ability.y - 32f, 64f, 64f)) continue;
            ability.collected = true;
            player.grantAbility(ability.type);
            score += 200;
            addEvent(GameEvent.Type.ABILITY, ability.x, ability.y);
            if (LevelData.AbilityPickup.DASH.equals(ability.type)) {
                showMessage("ХУЛАНГИЙН ХУРД нээгдлээ — тусгай товчийг дар", 3.1f);
            } else if (LevelData.AbilityPickup.SHIELD.equals(ability.type)) {
                showMessage("ТАХИЙН БАМБАЙ нээгдлээ — нэг цохилтыг хамгаална", 3.1f);
            } else {
                showMessage("ХАВТГАЙН НҮДЭЛТ — агаарт тусгай товчийг дар", 3.1f);
            }
        }
    }

    private void activateCheckpoints() {
        for (int i = 0; i < level.checkpoints.size(); i++) {
            LevelData.Checkpoint checkpoint = level.checkpoints.get(i);
            if (checkpoint.activated || player.centerX() < checkpoint.x - 10f) continue;
            checkpoint.activated = true;
            checkpointNumber = i + 1;
            checkpointX = checkpoint.x + 24f;
            checkpointY = checkpoint.y - Player.HEIGHT;
            score += 150;
            if (player.hasShield) player.shieldCharges = Math.max(1, player.shieldCharges);
            showMessage("Хяналтын цэг хадгалагдлаа", 2.2f);
            addEvent(GameEvent.Type.CHECKPOINT, checkpoint.x, checkpoint.y - 70f);
        }
    }

    private void updateEnemies(float dt) {
        for (LevelData.Enemy enemy : level.enemies) {
            if (!enemy.active) continue;
            enemy.hitCooldown = Math.max(0f, enemy.hitCooldown - dt);
            float phaseSpeed = enemy.isBoss() ? 1f + (enemy.phase() - 1) * 0.28f : 1f;
            if (!enemy.isBoss() || player.x > 3860f) {
                enemy.x += enemy.speed * phaseSpeed * enemy.direction * dt;
                if (enemy.x <= enemy.minX) {
                    enemy.x = enemy.minX;
                    enemy.direction = 1;
                } else if (enemy.x >= enemy.maxX) {
                    enemy.x = enemy.maxX;
                    enemy.direction = -1;
                }
            }

            if (!player.intersects(enemy.x, enemy.y, enemy.width, enemy.height)) continue;
            boolean stomp = player.velocityY > 90f
                    && player.y + Player.HEIGHT <= enemy.y + Math.min(52f, enemy.height * 0.43f);
            if (stomp || player.groundPounding) {
                hitEnemy(enemy, player.groundPounding ? 2 : 1);
                player.velocityY = player.groundPounding ? -530f : -430f;
                player.groundPounding = false;
                player.onGround = false;
            } else {
                applyDamage(enemy.x + enemy.width * 0.5f);
            }
        }
    }

    private void hitEnemy(LevelData.Enemy enemy, int damage) {
        if (enemy.hitCooldown > 0f) return;
        enemy.health -= damage;
        enemy.hitCooldown = 0.75f;
        if (enemy.isBoss()) {
            score += 400;
            addEvent(GameEvent.Type.BOSS_HIT, enemy.x + enemy.width * 0.5f, enemy.y + 30f);
            camera.shake(15f, 0.34f);
            if (enemy.health > 0) showMessage("ИХ УТАА — ҮЕ " + enemy.phase() + "/3", 2.1f);
        } else {
            score += 250;
            addEvent(GameEvent.Type.STOMP, enemy.x + enemy.width * 0.5f, enemy.y);
        }
        if (enemy.health <= 0) {
            enemy.active = false;
            enemiesDefeated++;
            if (enemy.isBoss()) {
                score += 1400;
                showMessage("Их Утаа сарнилаа! Бариа руу яв.", 3.2f);
            }
        }
    }

    private void updateHazards(float dt) {
        for (LevelData.Hazard hazard : level.hazards) {
            if (LevelData.Hazard.SPIKES.equals(hazard.type)
                    && player.intersects(hazard.x + 4f, hazard.y, hazard.width - 8f, hazard.height)) {
                applyDamage(hazard.x + hazard.width * 0.5f);
            }
            if (!LevelData.Hazard.ROCK_ZONE.equals(hazard.type)) continue;
            if (player.centerX() < hazard.x - 250f || player.centerX() > hazard.x + hazard.width + 250f) continue;
            float bossMultiplier = 1f;
            LevelData.Enemy boss = boss();
            if (boss != null && !boss.active && hazard.x > 3900f) continue;
            if (boss != null && player.x > 3950f) bossMultiplier = 1f + (boss.phase() - 1) * 0.35f;
            hazard.timer -= dt * bossMultiplier;
            if (hazard.timer <= 0f) {
                spawnRock(hazard);
                hazard.timer = hazard.interval * (0.82f + random.nextFloat() * 0.38f);
            }
        }

        for (LevelData.FallingRock rock : rocks) {
            if (!rock.active) continue;
            rock.velocityY += 620f * dt;
            rock.y += rock.velocityY * dt;
            if (rock.y - rock.radius > 690f) rock.active = false;
            if (rock.active && player.intersects(rock.x - rock.radius, rock.y - rock.radius,
                    rock.radius * 2f, rock.radius * 2f)) {
                rock.active = false;
                applyDamage(rock.x);
                camera.shake(8f, 0.2f);
            }
        }
        for (int i = rocks.size() - 1; i >= 0; i--) if (!rocks.get(i).active) rocks.remove(i);
    }

    private void spawnRock(LevelData.Hazard hazard) {
        LevelData.FallingRock rock = new LevelData.FallingRock();
        float focus = GameMath.clamp(player.centerX() + (random.nextFloat() - 0.5f) * 430f,
                hazard.x + 30f, hazard.x + hazard.width - 30f);
        rock.x = focus;
        rock.y = -45f - random.nextFloat() * 120f;
        rock.velocityY = 170f + random.nextFloat() * 100f;
        rock.radius = 20f + random.nextFloat() * 13f;
        rocks.add(rock);
    }

    private void handleBarrierAbilities() {
        for (LevelData.Barrier barrier : level.barriers) {
            if (!barrier.active) continue;
            boolean dashHit = player.dashTimer > 0f
                    && player.y + Player.HEIGHT > barrier.y
                    && player.y < barrier.y + barrier.height
                    && Math.abs(player.centerX() - (barrier.x + barrier.width * 0.5f)) < 105f;
            boolean poundHit = player.groundPoundImpact
                    && Math.abs(player.centerX() - (barrier.x + barrier.width * 0.5f)) < 145f;
            if (!dashHit && !poundHit) continue;
            barrier.health--;
            player.dashTimer = 0f;
            if (barrier.health <= 0) {
                barrier.active = false;
                score += 300;
                addEvent(GameEvent.Type.BARRIER, barrier.x + barrier.width * 0.5f, barrier.y + 30f);
                camera.shake(12f, 0.3f);
                showMessage("Бохирдлын хаалт нурлаа", 1.8f);
            }
        }
    }

    private void applyDamage(float sourceX) {
        Player.DamageResult result = player.damage(sourceX);
        if (result == Player.DamageResult.NONE) return;
        if (result == Player.DamageResult.SHIELDED) {
            addEvent(GameEvent.Type.SHIELD, player.centerX(), player.centerY());
            showMessage("Тахийн бамбай хамгааллаа", 1.8f);
            return;
        }
        lives--;
        addEvent(GameEvent.Type.HURT, player.centerX(), player.centerY());
        camera.shake(13f, 0.3f);
        if (lives <= 0) runState = RunState.DEAD;
    }

    private void loseLife(boolean fell) {
        if (runState != RunState.RUNNING) return;
        lives--;
        addEvent(GameEvent.Type.LIFE_LOST, player.centerX(), Math.min(player.centerY(), 650f));
        camera.shake(14f, 0.36f);
        if (lives <= 0) {
            runState = RunState.DEAD;
            return;
        }
        player.respawn(checkpointX, checkpointY);
        camera.x = GameMath.clamp(checkpointX - 360f, 0f, Math.max(0f, level.worldWidth - VIEW_WIDTH));
        showMessage(fell ? "Хяналтын цэгээс дахин эхэллээ" : "Дахин оролдоорой", 2.0f);
    }

    private void checkFinish() {
        if (runState != RunState.RUNNING || player.centerX() < level.goalX) return;
        String missing = missingObjective();
        if (!missing.isEmpty()) {
            showMessage(missing, 1.2f);
            player.x = Math.min(player.x, level.goalX - Player.WIDTH * 0.6f);
            return;
        }
        resultStars = ScoreCalculator.stars(collectedForStars(), level.totalScoredCollectibles(),
                lives, elapsed, level.parTime);
        resultScore = score + ScoreCalculator.completionBonus(lives, elapsed, level.parTime);
        score = resultScore;
        runState = RunState.COMPLETED;
        addEvent(GameEvent.Type.COMPLETE, player.centerX(), player.centerY());
    }

    private String missingObjective() {
        if (waterCollected < level.requiredWater) {
            return "Үлдсэн усны дусал: " + (level.requiredWater - waterCollected);
        }
        if (markersCollected < level.requiredMarkers) {
            return "Үлдсэн замын тэмдэг: " + (level.requiredMarkers - markersCollected);
        }
        LevelData.Enemy boss = boss();
        if (boss != null && boss.active) return "Эхлээд Их Утааг ял";
        for (LevelData.Barrier barrier : level.barriers) {
            if (barrier.active) return "Бохирдлын хаалтуудыг нураа";
        }
        return "";
    }

    private int collectedForStars() {
        return waterCollected + markersCollected + trashCollected;
    }

    public LevelData.Enemy boss() {
        if (level == null) return null;
        for (LevelData.Enemy enemy : level.enemies) if (enemy.isBoss()) return enemy;
        return null;
    }

    public String activeAbilityLabel() {
        if (player.hasGroundPound) return "НҮДЭЛТ";
        if (player.hasDash) return "ХУРД";
        return "";
    }

    public float progress() {
        if (level == null) return 0f;
        return GameMath.clamp(player.centerX() / level.goalX, 0f, 1f);
    }

    public List<GameEvent> drainEvents() {
        List<GameEvent> drained = new ArrayList<>(events);
        events.clear();
        return drained;
    }

    private void addEvent(GameEvent.Type type, float x, float y) {
        events.add(new GameEvent(type, x, y));
    }

    private void showMessage(String text, float seconds) {
        if (messageTimer > seconds && message.equals(text)) return;
        message = text;
        messageTimer = Math.max(messageTimer, seconds);
    }
}
