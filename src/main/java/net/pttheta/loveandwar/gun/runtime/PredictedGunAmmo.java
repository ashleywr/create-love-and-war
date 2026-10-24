package net.pttheta.loveandwar.gun.runtime;

import net.minecraft.world.item.ItemStack;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sidecar ammunition: authoritative snapshots minus unacknowledged local consumption per
 * activation.
 */
public final class PredictedGunAmmo {
    private static final class Reservation {
        int local, confirmed;
        boolean localFinished, serverFinished;
    }

    private final Map<UUID, Reservation> reservations = new HashMap<>();
    private AmmoPouchContents base = AmmoPouchContents.EMPTY;
    private long revision = -1;
    private long wantedRevision = -1;
    private int wantedCount;
    private AmmoPouchContents cachedBase, cachedRounds;
    private int cachedOutstanding;

    public void begin(UUID activation) {
        reservations.computeIfAbsent(activation, ignored -> new Reservation());
    }

    public void observe(ItemStack actual) {
        long value = GunAmmo.revision(actual);
        var rounds =
                actual.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY);
        if (value < revision || value < wantedRevision) return;
        if (value == wantedRevision && rounds.items().size() != wantedCount) return;
        base = rounds;
        revision = value;
    }

    private void snapshot(long value, int count) {
        if (value < revision || value < wantedRevision) return;
        wantedRevision = value;
        wantedCount = count;
        if (count <= base.items().size()) {
            base =
                    new AmmoPouchContents(
                            base.items().subList(base.items().size() - count, base.items().size()));
            revision = value;
        }
        // A larger/reloaded snapshot needs the actual ItemStack packet; never invent ammunition
        // identities.
    }

    public AmmoPouchContents rounds() {
        int outstanding = 0;
        for (var value : reservations.values())
            outstanding += Math.max(0, value.local - value.confirmed);
        if (outstanding == 0) return base;
        if (cachedBase != base || cachedOutstanding != outstanding) {
            cachedBase = base;
            cachedOutstanding = outstanding;
            cachedRounds =
                    new AmmoPouchContents(
                            base.items()
                                    .subList(
                                            Math.min(outstanding, base.items().size()),
                                            base.items().size()));
        }
        return cachedRounds;
    }

    public ItemStack consume(UUID activation) {
        var rounds = rounds();
        if (rounds.isEmpty()) return ItemStack.EMPTY;
        reservations.computeIfAbsent(activation, ignored -> new Reservation()).local++;
        return rounds.items().getFirst();
    }

    public void acknowledge(UUID activation, int consumed, long version, int count) {
        var value = reservations.get(activation);
        if (value != null) value.confirmed = Math.max(value.confirmed, consumed);
        snapshot(version, count);
    }

    public void end(UUID activation, int consumed, long version, int count) {
        acknowledge(activation, consumed, version, count);
        var value = reservations.get(activation);
        if (value != null) {
            value.serverFinished = true;
            if (value.localFinished) reservations.remove(activation);
        }
    }

    public void localFinished(UUID activation) {
        var value = reservations.get(activation);
        if (value != null) {
            value.localFinished = true;
            if (value.serverFinished) reservations.remove(activation);
        }
    }

    public boolean idle() {
        return reservations.isEmpty();
    }
}
