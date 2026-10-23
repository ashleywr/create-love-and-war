package net.pttheta.loveandwar.gun.runtime;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.pttheta.loveandwar.gun.content.ActionDef;
import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.gun.content.stat.ModifierSource;
import net.pttheta.loveandwar.item.ModDataComponents;

/** Original per-action pending slot semantics, scoped to server-owned runtime group instances. */
@EventBusSubscriber(modid = "createloveandwar")
public final class GunActionTimeline {
    private static final Map<ServerPlayer, IdentityHashMap<ItemStack, Map<String, Frame>>> FRAMES = new WeakHashMap<>();
    private static final int[] NO_DELAYS = new int[0];
    private static final boolean[] NO_PENDING = new boolean[0];
    private static final class Frame {
        final GunDef gun;
        final ActionGroup group;
        final boolean soundsOnly;
        final int[] remaining;
        final boolean[] pending;
        long lastTick;
        long tickedAt = Long.MIN_VALUE;
        long stateTickedAt = Long.MIN_VALUE;
        int progress;
        long triggers;
        boolean finishing;

        Frame(GunDef gun, ActionGroup group, boolean soundsOnly, long now) {
            this.gun = gun; this.group = group; this.soundsOnly = soundsOnly; this.lastTick = now;
            boolean delayed = group.actions().stream().anyMatch(action -> eligible(action, soundsOnly) && action.delay() != 0);
            remaining = delayed ? new int[group.actions().size()] : NO_DELAYS;
            pending = delayed ? new boolean[remaining.length] : NO_PENDING;
        }
    }
    private GunActionTimeline() {}

    private static boolean eligible(ActionDef action, boolean soundsOnly) {
        return !soundsOnly || action.actionType().equals("PlaySound") || action.actionType().equals("EjectCasing");
    }

    static void trigger(ServerPlayer player, ItemStack stack, GunDef gun, ActionGroup group, GunAttachments attachments, boolean soundsOnly) {
        var groups = FRAMES.computeIfAbsent(player, ignored -> new IdentityHashMap<>()).computeIfAbsent(stack, ignored -> new LinkedHashMap<>());
        var frame = groups.get(group.key());
        if (frame == null || !frame.group.equals(group) || frame.soundsOnly != soundsOnly) {
            frame = new Frame(gun, group, soundsOnly, player.level().getGameTime());
            groups.put(group.key(), frame);
        }
        frame.triggers++;
        var sources = RealisticDamage.apply(GunLookup.contentKey(stack), group, StatSources.forFiring(gun, group, attachments));
        int progress = GunFireScheduler.progress(player, stack, group.key());
        for (int index = 0; index < group.actions().size(); index++) {
            var action = group.actions().get(index);
            if (!eligible(action, soundsOnly)) continue;
            if (action.delay() == 0) GunEngine.executeAction(player, stack, gun, group, action, sources, progress);
            else {
                // Keep nonzero fractional/negative delays queued; truncation happens only after the zero check.
                frame.remaining[index] = (int)action.delay();
                frame.pending[index] = true;
            }
        }
    }

    static void tick(ServerPlayer player, ItemStack stack) {
        var stacks = FRAMES.get(player);
        var groups = stacks == null ? null : stacks.get(stack);
        if (groups == null) return;
        if (!player.isAlive() || player.isSpectator() || stack.isEmpty()) { stacks.remove(stack); return; }
        long now = player.level().getGameTime();
        var iterator = groups.values().iterator();
        while (iterator.hasNext()) {
            var frame = iterator.next();
            if (now <= frame.lastTick) continue;
            // Runtime pending work must not resume after a dropped/unloaded interval.
            if (now - frame.lastTick > 1) { iterator.remove(); continue; }
            frame.lastTick = now; frame.tickedAt = now; frame.progress++;
            List<ModifierSource> sources = null;
            for (int index = 0; index < frame.pending.length; index++) {
                if (!frame.pending[index]) continue;
                if (frame.remaining[index] > 1) { frame.remaining[index]--; continue; }
                frame.pending[index] = false;
                if (sources == null) {
                    var attachments = stack.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY);
                    sources = RealisticDamage.apply(GunLookup.contentKey(stack), frame.group, StatSources.forFiring(frame.gun, frame.group, attachments));
                }
                // Different same-time callbacks use authored index order; original HashMap order was unspecified.
                GunEngine.executeAction(player, stack, frame.gun, frame.group, frame.group.actions().get(index), sources, frame.progress);
            }
        }
    }

    static void finishTick(ServerPlayer player, ItemStack stack) {
        var stacks = FRAMES.get(player);
        var groups = stacks == null ? null : stacks.get(stack);
        if (groups == null) return;
        long now = player.level().getGameTime();
        // Defer removal until reload transitions can retrigger the same instance and extend its lifetime.
        groups.values().removeIf(frame -> frame.tickedAt == now && (frame.finishing
                || (GunFireMode.of(frame.group.repeatMode()) == GunFireMode.SEMI_AUTO || GunFireMode.of(frame.group.repeatMode()) == GunFireMode.BURST_FIRE)
                && frame.progress > (long)GunEngine.groupDurationTicks(frame.group) * frame.triggers));
        if (groups.isEmpty()) stacks.remove(stack);
        if (stacks.isEmpty()) FRAMES.remove(player);
    }

    static void finishAfterTick(ServerPlayer player, ItemStack stack, String key) {
        var stacks = FRAMES.get(player);
        var groups = stacks == null ? null : stacks.get(stack);
        var frame = groups == null ? null : groups.get(key);
        if (frame != null) frame.finishing = true;
    }

    static void cancel(ServerPlayer player, ItemStack stack, String key) {
        var stacks = FRAMES.get(player);
        var groups = stacks == null ? null : stacks.get(stack);
        if (groups != null) groups.remove(key);
    }

    static void cancelAll(ServerPlayer player, ItemStack stack) {
        var stacks = FRAMES.get(player);
        if (stacks != null) stacks.remove(stack);
    }

    private static Frame frame(ServerPlayer player, ItemStack stack, String key) {
        var stacks = FRAMES.get(player);
        var groups = stacks == null ? null : stacks.get(stack);
        return groups == null ? null : groups.get(key);
    }

    static boolean owns(ServerPlayer player, ItemStack stack, String key) {
        return frame(player, stack, key) != null;
    }

    static boolean advanceState(ServerPlayer player, ItemStack stack, String key) {
        var frame = frame(player, stack, key);
        long now = player.level().getGameTime();
        if (frame == null || frame.tickedAt != now || frame.stateTickedAt == now) return false;
        frame.stateTickedAt = now;
        return true;
    }

    @SubscribeEvent public static void prune(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var stacks = FRAMES.get(player);
        if (stacks == null) return;
        long now = player.level().getGameTime();
        stacks.entrySet().removeIf(entry -> {
            entry.getValue().values().removeIf(frame -> frame.lastTick < now - 1);
            return entry.getKey().isEmpty() || entry.getValue().isEmpty();
        });
        if (stacks.isEmpty()) FRAMES.remove(player);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { FRAMES.remove(event.getEntity()); }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { FRAMES.clear(); }
}
