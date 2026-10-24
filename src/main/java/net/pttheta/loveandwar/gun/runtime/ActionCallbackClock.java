package net.pttheta.loveandwar.gun.runtime;

import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;

/** Side-independent callback phases from Flans ActionGroupInstance; no world or inventory state. */
public final class ActionCallbackClock {
    @FunctionalInterface
    public interface Callback {
        void run(int actionIndex, long triggerIndex, int progress);
    }

    private static final int[] NO_DELAYS = new int[0];
    private static final byte[] NO_PENDING = new byte[0];
    private static final int PENDING = 1;
    private static final int COMPLETED = 2;
    private final float[] delays;
    private final int[] remaining;
    private final byte[] pending;
    private int progress;
    private long triggers;

    public ActionCallbackClock(int actionCount, IntToDoubleFunction delay) {
        delays = new float[actionCount];
        boolean delayed = false;
        for (int i = 0; i < actionCount; i++) {
            delays[i] = (float)delay.applyAsDouble(i);
            delayed |= delays[i] != 0;
        }
        remaining = delayed ? new int[actionCount] : NO_DELAYS;
        pending = delayed ? new byte[actionCount] : NO_PENDING;
    }

    public int progress() { return progress; }
    public long triggers() { return triggers; }

    /** A filtered invocation still counts as a trigger, but leaves excluded pending slots intact. */
    public void trigger(IntPredicate eligible, Callback callback) {
        long index = triggers;
        for (int i = 0; i < delays.length; i++) {
            if (!eligible.test(i)) continue;
            if (delays[i] == 0) callback.run(i, index, progress);
            else {
                // Test zero before truncation: fractional/negative nonzero delays wait for the next tick.
                remaining[i] = (int)delays[i];
                pending[i] |= PENDING;
            }
        }
        // Source OnStart/CheckRetrigger increment only after the whole OnTrigger callback loop.
        triggers++;
    }

    public boolean hasPending() {
        for (byte value : pending) if (value != 0) return true;
        return false;
    }

    public void tick(Callback callback) {
        progress++;
        for (int i = 0; i < pending.length; i++) {
            if ((pending[i] & PENDING) == 0) continue;
            int next = remaining[i] - 1;
            if (next <= 0) {
                pending[i] |= COMPLETED;
                // Quirk kept: due callbacks see current TriggerCount, not the index that queued them.
                callback.run(i, triggers, progress);
            }
            remaining[i] = next;
        }
        // Source removes completed keys after all due callbacks, including any synchronous requeues.
        for (int i = 0; i < pending.length; i++) if ((pending[i] & COMPLETED) != 0) pending[i] = 0;
    }

    public boolean pastDuration(int duration) { return progress > (long)duration * triggers; }
}
