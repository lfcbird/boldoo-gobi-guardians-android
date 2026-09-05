package com.boldtechnology.boldoogame;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class GameMathTest {
    @Test
    public void clampAndApproachAreStable() {
        assertEquals(10f, GameMath.clamp(12f, 0f, 10f), 0.001f);
        assertEquals(-2f, GameMath.approach(-5f, 2f, 3f), 0.001f);
        assertEquals(2f, GameMath.approach(5f, 2f, 5f), 0.001f);
    }

    @Test
    public void overlapDoesNotCountTouchingEdges() {
        assertTrue(GameMath.overlaps(0, 0, 10, 10, 9, 9, 10, 10));
        assertFalse(GameMath.overlaps(0, 0, 10, 10, 10, 0, 10, 10));
    }
}
