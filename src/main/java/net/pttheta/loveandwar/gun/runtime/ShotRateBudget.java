package net.pttheta.loveandwar.gun.runtime;

/** Real-time per-gun debt, with bounded 30-tick catch-up credit for delayed/coalesced delivery. */
final class ShotRateBudget {
    private long last;
    private float credit = 30;

    ShotRateBudget(long now) {
        last = now;
    }

    boolean spend(float cost, long now) {
        credit = Math.min(30, credit + Math.max(0, now - last) / 50_000_000F);
        last = Math.max(last, now);
        if (credit <= 0) return false;
        credit -= cost;
        return true;
    }
}
