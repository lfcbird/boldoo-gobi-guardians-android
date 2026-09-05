package com.boldtechnology.boldoogame;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PlayerMovementTest {
    @Test
    public void playerAcceleratesAndJumpsFromPlatform() {
        LevelData level = flatLevel();
        Player player = new Player();
        InputController input = new InputController();
        player.reset(100, 610 - Player.HEIGHT);
        player.update(1f / 60f, input, level, 0f);
        input.setState(false, true, false, false);
        for (int i = 0; i < 20; i++) player.update(1f / 60f, input, level, 0f);
        assertTrue(player.x > 120f);
        input.setState(false, true, true, false);
        player.update(1f / 60f, input, level, 0f);
        assertTrue(player.velocityY < 0f);
    }

    @Test
    public void windMovesAnIdlePlayer() {
        LevelData level = flatLevel();
        Player player = new Player();
        InputController input = new InputController();
        player.reset(300, 610 - Player.HEIGHT);
        player.update(1f / 60f, input, level, 0f);
        for (int i = 0; i < 12; i++) player.update(1f / 60f, input, level, -250f);
        assertTrue(player.velocityX < -150f);
        assertTrue(player.x < 300f);
    }

    private static LevelData flatLevel() {
        LevelData level = new LevelData();
        level.id = 1;
        level.worldWidth = 1800;
        level.goalX = 1700;
        LevelData.Platform ground = new LevelData.Platform();
        ground.id = "ground";
        ground.x = ground.baseX = 0f;
        ground.y = ground.baseY = 610f;
        ground.width = 1800f;
        ground.height = 130f;
        level.platforms.add(ground);
        return level;
    }
}
