package net.pttheta.loveandwar.gun.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ShotRateBudgetTest {
    @Test
    void coalescedDeliveryHasBoundedCreditThenRequiresRealElapsedTime() {
        var budget = new ShotRateBudget(0);
        for (int i = 0; i < 30; i++) assertTrue(budget.spend(1, 0));
        assertFalse(budget.spend(1, 0));
        assertTrue(budget.spend(1, 50_000_000));
        assertFalse(budget.spend(1, 50_000_000));
    }

    @Test
    void sustainedAuthoredRateKeepsFractionalDebtWithoutDrift() {
        var budget = new ShotRateBudget(0);
        for (int i = 0; i < 1000; i++) assertTrue(budget.spend(1.1F, i * 55_000_000L));
    }

    @Test
    void longIntervalsCanFireOnceButCannotEraseDebtWithIdleCreditCap() {
        var budget = new ShotRateBudget(0);
        assertTrue(budget.spend(100, 0));
        assertFalse(budget.spend(100, 0));
        assertFalse(budget.spend(100, 3_500_000_000L));
        assertTrue(budget.spend(100, 5_000_000_000L));
    }

    @Test
    void idleCreditIsCappedAndBackwardsTimeCannotRefillIt() {
        var budget = new ShotRateBudget(0);
        assertTrue(budget.spend(30, 10_000_000_000L));
        assertFalse(budget.spend(1, 0));
        assertFalse(budget.spend(1, 10_000_000_000L));
        assertTrue(budget.spend(1, 10_050_000_000L));
    }
}
