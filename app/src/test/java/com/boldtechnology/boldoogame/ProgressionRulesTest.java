package com.boldtechnology.boldoogame;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class ProgressionRulesTest {
    @Test
    public void unlocksNextLevelWithoutExceedingCampaign() {
        assertEquals(2, ProgressionRules.unlockedAfterCompletion(1, 3));
        assertEquals(3, ProgressionRules.unlockedAfterCompletion(2, 3));
        assertEquals(3, ProgressionRules.unlockedAfterCompletion(3, 3));
    }
}
