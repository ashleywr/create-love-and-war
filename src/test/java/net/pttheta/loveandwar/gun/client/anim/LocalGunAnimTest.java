package net.pttheta.loveandwar.gun.client.anim;

import net.pttheta.loveandwar.gun.runtime.GunActionState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalGunAnimTest {
    @Test void confirmedRetriggerRestartsAnimationDespiteMonotonicGroupProgress() {
        LocalGunAnim.clear();
        try {
            var first = GunActionState.IDLE.withFiringFrame("primary_fire", 10, 16, 0, 2, true);
            assertEquals(10, LocalGunAnim.resolve("g11k2", first).ticks(), 0.001);
            var second = first.withFiringFrame("primary_fire", 11, 31, 0, 1, true);
            assertEquals(0, LocalGunAnim.resolve("g11k2", second).ticks(), 0.001,
                    "A server-confirmed repeated trigger must restart its animation without requiring a second client press");
        } finally { LocalGunAnim.clear(); }
    }
}
