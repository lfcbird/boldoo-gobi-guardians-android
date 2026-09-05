package com.boldtechnology.boldoogame;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ScoreCalculatorTest {
    @Test
    public void awardsThreeStarsForCompleteFastRun() {
        assertEquals(3, ScoreCalculator.stars(10, 10, 3, 80f, 100f));
    }

    @Test
    public void completionBonusRewardsLivesAndTime() {
        int strong = ScoreCalculator.completionBonus(3, 70f, 100f);
        int weak = ScoreCalculator.completionBonus(1, 120f, 100f);
        assertTrue(strong > weak);
    }
}
