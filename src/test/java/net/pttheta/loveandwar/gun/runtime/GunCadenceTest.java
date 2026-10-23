package net.pttheta.loveandwar.gun.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GunCadenceTest {
    @Test void g11RetainsFractionalTicksBetweenBurstChecks() {
        var cadence = new GunCadence();
        assertEquals(1, cadence.trigger(0.055F));
        cadence.tick(1);
        assertFalse(cadence.idle(), "The fractional remainder must survive between presses");
        assertEquals(1, cadence.trigger(0.055F));
        cadence.tick(1);
        assertEquals(1, cadence.trigger(0.055F));
    }

    @Test void fullAutoAndSubTickRatesUseOriginalAccumulatorThreshold() {
        var slow = new GunCadence();
        assertEquals(1, slow.trigger(0.1F));
        slow.tick(1);
        assertEquals(0, slow.trigger(0.1F));
        slow.tick(1);
        assertEquals(1, slow.trigger(0.1F));
        var fast = new GunCadence();
        assertEquals(2, fast.trigger(0.025F));
        fast.tick(1);
        assertEquals(2, fast.trigger(0.025F));
    }

    @Test void zeroDelayInitialActionAndMalformedIntervalsRemainBounded() {
        var cadence = new GunCadence();
        assertEquals(1, cadence.trigger(0));
        assertTrue(cadence.idle());
        assertEquals(64, cadence.trigger(Float.MIN_VALUE));
        cadence.tick(1);
        assertEquals(1, cadence.trigger(Float.NaN));
    }
}
