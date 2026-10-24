package net.pttheta.loveandwar.gun.runtime;

import net.pttheta.loveandwar.gun.content.ActionGroup;

/** Server checks source phase/cadence independently of client-reported hits or ammunition. */
final class AuthoredTriggerGate {
    final GunFireMode mode;
    final float delay;
    final int repeats, duration;
    int tick;
    long triggers;
    int release = -1, burstUntil = -1, lastStart = -1;
    float cooldown;

    AuthoredTriggerGate(ActionGroup group) {
        mode = GunFireMode.of(group.repeatMode());
        delay = group.repeatDelay();
        repeats = group.repeatCount();
        duration = group.durationTicks();
    }

    boolean validTick(int value) {
        if (value < tick || value < 0) return false;
        if (mode == GunFireMode.FULL_AUTO && release >= 0 && value > release + 1) return false;
        return (mode != GunFireMode.SEMI_AUTO && mode != GunFireMode.BURST_FIRE)
                || value <= (long) duration * triggers + 1
                || lastStart < 0;
    }

    private boolean consume(int value, int count, float interval) {
        if (!validTick(value) || count < 0 || count > 64) return false;
        float next = Math.max(0, cooldown - (value - tick));
        if (interval <= 0) {
            if (count > 1) return false;
        } else
            for (int i = 0; i < count; i++) {
                if (next >= 1) return false;
                next += interval(interval);
            }
        cooldown = next;
        tick = value;
        triggers += count;
        return true;
    }

    boolean start(int value, long first, int count) {
        if (lastStart < 0 && value != 0) return false;
        if (lastStart >= 0
                && (mode == GunFireMode.SEMI_AUTO || mode == GunFireMode.BURST_FIRE)
                && value > (long) duration * triggers) return false;
        if (first != triggers || lastStart == value || !consume(value, count, delay)) return false;
        lastStart = value;
        release = -1;
        burstUntil =
                delay > 0 && mode == GunFireMode.BURST_FIRE
                        ? value + Math.max(0, repeats - 1)
                        : value;
        return true;
    }

    boolean trigger(int value, long ordinal) {
        if (ordinal != triggers
                || value <= lastStart
                || mode == GunFireMode.SEMI_AUTO
                || mode == GunFireMode.TOGGLE
                || mode == GunFireMode.UNKNOWN) return false;
        if (mode == GunFireMode.BURST_FIRE && value > burstUntil) return false;
        return consume(value, 1, delay <= 0 ? .05F : delay);
    }

    boolean step(int value, long count) {
        if (count != triggers || !validTick(value)) return false;
        cooldown = Math.max(0, cooldown - (value - tick));
        tick = value;
        return true;
    }

    void release(int value) {
        release = Math.max(tick, value);
    }

    boolean finish(int value, long count) {
        if (!step(value, count)) return false;
        return mode == GunFireMode.FULL_AUTO
                ? release >= 0 && value == release + 1
                : value > (long) duration * triggers;
    }

    static float interval(float seconds) {
        return Float.isFinite(seconds) ? Math.max(1F / 64, seconds * 20) : 1;
    }
}
