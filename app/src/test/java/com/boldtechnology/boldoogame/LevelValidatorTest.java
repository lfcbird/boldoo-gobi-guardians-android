package com.boldtechnology.boldoogame;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public final class LevelValidatorTest {
    @Test
    public void acceptsMinimalValidLevel() {
        LevelData level = levelWithGround();
        LevelValidator.validate(level);
    }

    @Test
    public void rejectsImpossibleRequiredWater() {
        LevelData level = levelWithGround();
        level.requiredWater = 1;
        assertThrows(IllegalArgumentException.class, () -> LevelValidator.validate(level));
    }

    @Test
    public void bossReportsThreeHealthPhases() {
        LevelData.Enemy boss = new LevelData.Enemy();
        boss.type = LevelData.Enemy.GREAT_SMOG;
        boss.maxHealth = boss.health = 3;
        assertEquals(1, boss.phase());
        boss.health = 2;
        assertEquals(2, boss.phase());
        boss.health = 1;
        assertEquals(3, boss.phase());
    }

    private static LevelData levelWithGround() {
        LevelData level = new LevelData();
        level.id = 1;
        level.worldWidth = 1600;
        level.startX = 50;
        level.goalX = 1500;
        LevelData.Platform ground = new LevelData.Platform();
        ground.id = "ground";
        ground.x = ground.baseX = 0;
        ground.y = ground.baseY = 610;
        ground.width = 1600;
        ground.height = 130;
        level.platforms.add(ground);
        return level;
    }
}
