package com.alex.mycelialmind;

import com.alex.mycelialmind.client.model.BurrowerModel;
import com.alex.mycelialmind.client.model.MycelialStalkerModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SpringAnimationTest {
    @Test
    void repeatedRenderingAtSameTimeLeavesSpringUnchanged() throws Exception {
        for (Class<?> model : new Class<?>[]{BurrowerModel.class, MycelialStalkerModel.class}) {
            var step = model.getDeclaredMethod("stepSpringDamperExact", float[].class, int.class,
                    float.class, float.class, float.class);
            step.setAccessible(true);
            float[] state = {0.15F, -0.07F};
            float[] initial = state.clone();
            for (int i = 0; i < 100; i++) {
                step.invoke(null, state, 0, 0.4F, 0.18F, 0.0F);
            }
            assertArrayEquals(initial, state, 0.000001F, model.getSimpleName());
            step.invoke(null, state, 0, 0.4F, 0.18F, 0.05F);
            assertNotEquals(initial[0], state[0], model.getSimpleName());
        }
    }
}
