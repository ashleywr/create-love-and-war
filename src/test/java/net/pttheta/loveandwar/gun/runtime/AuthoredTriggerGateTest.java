package net.pttheta.loveandwar.gun.runtime;

import static org.junit.jupiter.api.Assertions.*;

import net.pttheta.loveandwar.gun.content.ActionGroup;

import org.junit.jupiter.api.Test;

import java.util.List;

class AuthoredTriggerGateTest {
    private static AuthoredTriggerGate gate(String mode, float delay, int repeats) {
        return new AuthoredTriggerGate(
                new ActionGroup(
                        "fire", true, false, true, false, mode, delay, repeats, 0, 0, "", List.of(),
                        List.of()));
    }

    @Test
    void mat49FinalStartOccursAfterReleaseBeforeRetirement() {
        var gate = gate("FullAuto", .1F, 0);
        assertTrue(gate.start(0, 0, 1));
        gate.release(1);
        assertFalse(gate.trigger(1, 1));
        assertTrue(gate.trigger(2, 1));
        assertTrue(gate.finish(2, 2));
        assertFalse(gate.trigger(3, 2));
    }

    @Test
    void releasedBurstKeepsOnlyItsAuthoredRetriggerChecks() {
        var gate = gate("BurstFire", .055F, 3);
        assertTrue(gate.start(0, 0, 1));
        gate.release(0);
        assertTrue(gate.trigger(1, 1));
        assertTrue(gate.trigger(2, 2));
        assertFalse(gate.trigger(3, 3));
        assertTrue(gate.finish(4, 3));
    }

    @Test
    void subTickCallbacksSharePhaseButCannotExceedCadence() {
        var gate = gate("FullAuto", .025F, 0);
        assertFalse(gate.start(0, 0, 3));
        assertTrue(gate.start(0, 0, 2));
        assertTrue(gate.trigger(1, 2));
        assertTrue(gate.trigger(1, 3));
        assertFalse(gate.trigger(1, 4));
    }

    @Test
    void semiCannotForgeAutonomousTriggersOrSkipOrdinals() {
        var gate = gate("SemiAuto", .1F, 0);
        assertFalse(gate.start(4, 0, 1));
        assertFalse(gate.start(0, 1, 1));
        assertFalse(gate.start(0, 0, 65));
        assertTrue(gate.start(0, 0, 1));
        assertFalse(gate.trigger(1, 1));
        assertFalse(gate.step(1, 2));
        assertTrue(gate.step(1, 1));
        assertFalse(gate.start(1, 1, 1)); // Remaining cooldown is one full tick.
        assertTrue(gate.start(2, 1, 1));
    }

    @Test
    void endBoundaryCanFinishButCannotResurrectSemiInstance() {
        var gate = gate("SemiAuto", .1F, 0);
        assertTrue(gate.start(0, 0, 1));
        assertFalse(gate.finish(2, 1));
        assertFalse(gate.start(3, 1, 1));
        assertTrue(gate.finish(3, 1));
    }

    @Test
    void zeroDelayHasSingleInitialTriggerThenOnePerStart() {
        var gate = gate("FullAuto", 0, 0);
        assertFalse(gate.start(0, 0, 2));
        assertTrue(gate.start(0, 0, 1));
        assertFalse(gate.trigger(0, 1));
        assertTrue(gate.trigger(1, 1));
        assertFalse(gate.trigger(1, 2));
    }

    @Test
    void missingOrEarlyFinishAndBackwardsPhasesAreRejected() {
        var gate = gate("FullAuto", .05F, 0);
        assertTrue(gate.start(0, 0, 1));
        assertFalse(gate.finish(1, 1));
        gate.release(1);
        assertFalse(gate.finish(1, 1));
        assertFalse(gate.step(0, 1));
        assertFalse(gate.step(-1, 1));
        assertTrue(gate.finish(2, 1));
    }
}
