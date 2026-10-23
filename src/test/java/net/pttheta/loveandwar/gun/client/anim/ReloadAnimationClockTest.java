package net.pttheta.loveandwar.gun.client.anim;

import java.util.List;
import net.pttheta.loveandwar.gun.runtime.GunActionState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class ReloadAnimationClockTest {
    @Test void observerAnimationRestartsAtActualRepeatedLoadTriggers() {
        var state = GunActionState.IDLE.withReloadGroup("load", 11, List.of());
        assertEquals(0, state.reloadAnimationElapsed(11));
        for (int tick = 0; tick < 11; tick++) state = state.tickedGroup();
        assertEquals(11, state.reloadAnimationElapsed(11));
        state = state.tickedGroup().withReloadGroup("load", 11, List.of());
        assertEquals(12, state.groupTicksElapsed());
        assertEquals(0, state.reloadAnimationElapsed(11));
        for (int tick = 0; tick < 10; tick++) state = state.tickedGroup();
        assertEquals(10, state.reloadAnimationElapsed(11));
        state = state.tickedGroup().withReloadGroup("load", 11, List.of());
        assertEquals(23, state.groupTicksElapsed());
        assertEquals(0, state.reloadAnimationElapsed(11));
    }
}
