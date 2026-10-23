package net.pttheta.loveandwar.gun.runtime;

/** Fractional tick accumulator from Flans ActionStack.TryShootMultiple. */
final class GunCadence {
    private float cooldown;

    void tick(int elapsed) {
        this.cooldown = Math.max(0, this.cooldown - elapsed);
    }

    int trigger(float delaySeconds) {
        if (delaySeconds <= 0) return 1;
        // Bound malformed content; no authored firing interval approaches this safety limit.
        float interval = Float.isFinite(delaySeconds) ? Math.max(1.0F / 64, delaySeconds * 20) : 1;
        int count = 0;
        while (this.cooldown < 1 && count < 64) {
            count++;
            this.cooldown += interval;
        }
        return count;
    }

    int ticksUntilReady() {
        return (int)Math.floor(this.cooldown);
    }

    boolean idle() { return this.cooldown <= 0; }
}
