package net.pttheta.loveandwar.gun.runtime;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.network.GunAuthoredInput;
import net.pttheta.loveandwar.gun.network.GunPredictionClaim;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Validates owner-authored phases; only the server executes ammunition, ballistics and damage. */
@EventBusSubscriber(modid = "createloveandwar")
public final class GunAuthoredFire {
    private static final Map<ServerPlayer, Map<UUID, ShotRateBudget>> RATES = new WeakHashMap<>();

    private GunAuthoredFire() {}

    static boolean spend(ServerPlayer player, ItemStack gun, float seconds, int count) {
        if (count == 0) return true;
        var id = GunIdentity.id(gun);
        if (id.equals(GunIdentity.INVALID))
            return true; // Internal legacy/test guns are not remotely addressable.
        long now = System.nanoTime();
        var rates = RATES.computeIfAbsent(player, ignored -> new HashMap<>());
        var budget = rates.computeIfAbsent(id, ignored -> new ShotRateBudget(now));
        return budget.spend(
                (seconds <= 0 ? 1 : AuthoredTriggerGate.interval(seconds)) * count, now);
    }

    static final class Remote {
        final UUID activation;
        final AuthoredTriggerGate gate;
        final long started = System.nanoTime();

        Remote(GunPredictionClaim claim, ActionGroup group) {
            activation = claim.activation();
            gate = new AuthoredTriggerGate(group);
        }

        boolean time(int tick) {
            return tick >= 0 && tick <= (System.nanoTime() - started) / 50_000_000L + 30;
        }

        boolean start(ServerPlayer player, ItemStack stack, GunAuthoredInput input) {
            return time(input.tick())
                    && gate.start(input.tick(), input.firstTrigger(), input.count())
                    && spend(player, stack, gate.delay, input.count());
        }

        boolean release(int tick) {
            if (!time(tick) || !gate.validTick(tick)) return false;
            gate.release(tick);
            return true;
        }
    }

    @SubscribeEvent
    public static void prune(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var rates = RATES.get(player);
        if (rates == null) return;
        rates.keySet().removeIf(id -> GunIdentity.resolve(player, id).isEmpty());
        if (rates.isEmpty()) RATES.remove(player);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        RATES.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        RATES.clear();
    }
}
