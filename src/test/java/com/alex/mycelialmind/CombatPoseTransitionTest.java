package com.alex.mycelialmind;

import com.alex.mycelialmind.client.model.CombatPoseTransition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatPoseTransitionTest {
    @Test
    void transitionsAreContinuousIndependentAndFrameRateIndependent() {
        var transition = new CombatPoseTransition<Object>();
        Object first = new Object();
        Object second = new Object();
        assertEquals(0.0F, transition.sample(first, false, 100.0F));
        assertEquals(1.0F, transition.sample(second, true, 100.0F));
        assertEquals(0.0F, transition.sample(first, true, 100.0F));
        float halfway = transition.sample(first, true, 102.0F);
        assertEquals(0.5F, halfway, 0.0001F);
        assertEquals(halfway, transition.sample(first, false, 102.0F), 0.0001F);
        assertTrue(transition.sample(first, false, 104.0F) > halfway);
        assertEquals(1.0F, transition.sample(second, true, 104.0F));

        var highFps = new CombatPoseTransition<Object>();
        Object third = new Object();
        highFps.sample(third, false, 100.0F);
        highFps.sample(third, true, 100.0F);
        for (int i = 1; i <= 8; i++) {
            highFps.sample(third, true, 100.0F + i * 0.25F);
        }
        assertEquals(halfway, highFps.sample(third, true, 102.0F), 0.0001F);
        assertTrue(transition.sample(first, false, 120.0F) < 0.25F);
        assertEquals(1.0F, transition.sample(first, true, 1.0F)); // Entity age reset.
    }

    @Test
    void briefAggressionDropsDoNotShakeThePose() {
        var transition = new CombatPoseTransition<Object>();
        Object entity = new Object();
        transition.sample(entity, true, 0.0F);
        for (int tick = 1; tick <= 20; tick++) {
            assertEquals(1.0F, transition.sample(entity, tick % 2 == 0, tick), 0.0001F);
        }
        assertEquals(1.0F, transition.sample(entity, false, 27.0F), 0.0001F);
        assertTrue(transition.sample(entity, false, 29.0F) < 1.0F);
        assertTrue(transition.sample(entity, false, 40.0F) < 0.1F);
    }
}
