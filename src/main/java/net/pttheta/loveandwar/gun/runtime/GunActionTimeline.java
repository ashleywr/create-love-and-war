package net.pttheta.loveandwar.gun.runtime;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.UUID;
import java.util.ArrayList;
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
        GunActiveGroups.Entry descriptor;
        final boolean delayedScope;
        final int[] remaining;
        final boolean[] pending;
        long lastTick;
        long tickedAt = Long.MIN_VALUE;
        long stateTickedAt = Long.MIN_VALUE;
        int progress;
        long triggers;
        boolean finishing;

        Frame(ServerPlayer player, GunDef gun, ActionGroup group, GunActiveGroups.Origin origin, long now) {
            this.gun = gun; this.group = group; this.lastTick = now;
            descriptor = new GunActiveGroups.Entry(group.key(), origin, UUID.randomUUID(), now, player.getUUID());
            delayedScope=group.actions().stream().anyMatch(action->"Scope".equals(action.actionType())&&action.delay()!=0);
            boolean delayed = group.actions().stream().anyMatch(action -> action.delay() != 0);
            remaining = delayed ? new int[group.actions().size()] : NO_DELAYS;
            pending = delayed ? new boolean[remaining.length] : NO_PENDING;
        }
    }
    private GunActionTimeline() {}

    private static boolean eligible(ActionDef action, boolean soundsOnly) {
        return !soundsOnly || action.actionType().equals("PlaySound") || action.actionType().equals("EjectCasing");
    }

    static void trigger(ServerPlayer player, ItemStack stack, GunDef gun, ActionGroup group, GunAttachments attachments, boolean soundsOnly) {
        trigger(player, stack, gun, group, attachments, soundsOnly, GunActiveGroups.Origin.base(stack));
    }

    static void trigger(ServerPlayer player, ItemStack stack, GunDef gun, ActionGroup group, GunAttachments attachments, boolean soundsOnly, GunActiveGroups.Origin origin) {
        claim(player, stack);
        var groups = FRAMES.computeIfAbsent(player, ignored -> new IdentityHashMap<>()).computeIfAbsent(stack, ignored -> new LinkedHashMap<>());
        var frame = groups.get(group.key());
        if (frame == null) {
            frame = new Frame(player, gun, group, origin, player.level().getGameTime());
            groups.put(group.key(), frame);
            publish(player, stack, groups);
        }
        // Quirk kept: ActionStack aliases Def.key and reuses the first instance's actual definition/context.
        gun = frame.gun;
        group = frame.group;
        frame.triggers++;
        if(frame.delayedScope){
            var previous=frame.descriptor;
            frame.descriptor=new GunActiveGroups.Entry(previous.key(),previous.origin(),previous.activation(),previous.startedAt(),previous.owner(),frame.triggers);
            publish(player,stack,groups);
        }
        var sources = RealisticDamage.apply(GunLookup.contentKey(stack), group, StatSources.forFiring(gun, group, attachments));
        int progress = frame.progress;
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
        if (!player.isAlive() || player.isSpectator() || stack.isEmpty()) { stacks.remove(stack); publish(player, stack, null); return; }
        long now = player.level().getGameTime();
        boolean removed = false;
        var iterator = groups.values().iterator();
        while (iterator.hasNext()) {
            var frame = iterator.next();
            if (now <= frame.lastTick) continue;
            // Runtime pending work must not resume after a dropped/unloaded interval.
            if (now - frame.lastTick > 1) { iterator.remove(); removed = true; continue; }
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
        if (removed) publish(player, stack, groups);
    }

    static void finishTick(ServerPlayer player, ItemStack stack) {
        var stacks = FRAMES.get(player);
        var groups = stacks == null ? null : stacks.get(stack);
        if (groups == null) return;
        long now = player.level().getGameTime();
        // Defer removal until reload transitions can retrigger the same instance and extend its lifetime.
        boolean removed = groups.values().removeIf(frame -> {
            boolean remove = frame.tickedAt == now && (frame.finishing
                || (GunFireMode.of(frame.group.repeatMode()) == GunFireMode.SEMI_AUTO || GunFireMode.of(frame.group.repeatMode()) == GunFireMode.BURST_FIRE)
                && frame.progress > (long)GunEngine.groupDurationTicks(frame.group) * frame.triggers);
            if (remove && frame.finishing) {
                var state = stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
                if (state.activeToggles().contains(frame.group.key())) stack.set(ModDataComponents.GUN_ACTION_STATE, state.withToggle(frame.group.key(), false));
            }
            return remove;
        });
        if (removed) publish(player, stack, groups);
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
        if (groups != null && groups.remove(key) != null) publish(player, stack, groups);
    }

    static void cancelAll(ServerPlayer player, ItemStack stack) {
        var stacks = FRAMES.get(player);
        if (stacks != null) stacks.remove(stack);
        publish(player, stack, null);
    }

    private static Frame frame(ServerPlayer player, ItemStack stack, String key) {
        var stacks = FRAMES.get(player);
        var groups = stacks == null ? null : stacks.get(stack);
        return groups == null ? null : groups.get(key);
    }

    record Context(GunDef gun, ActionGroup group, GunActiveGroups.Origin origin, int progress, long triggers) {}

    static java.util.Optional<Context> context(ServerPlayer player, ItemStack stack, String key) {
        var frame = frame(player, stack, key);
        return frame == null ? java.util.Optional.empty() : java.util.Optional.of(new Context(frame.gun, frame.group, frame.descriptor.origin(), frame.progress, frame.triggers));
    }

    static void synchronize(ServerPlayer player, ItemStack stack) {
        claim(player, stack);
        var stacks = FRAMES.get(player);
        publish(player, stack, stacks == null ? null : stacks.get(stack));
    }

    private static void claim(ServerPlayer owner, ItemStack stack) {
        boolean transferred = false;
        for (var entry : GunActiveGroups.of(stack).entries()) if (!entry.owner().equals(owner.getUUID())) { transferred = true; break; }
        if (!transferred) return;
        // Transfer invalidates old holders before creating a replacement. Their later logout/prune cannot erase its view.
        for (var entry : new ArrayList<>(FRAMES.entrySet())) if (entry.getKey() != owner && entry.getValue().remove(stack) != null) {
            GunFireScheduler.cancel(entry.getKey(), stack);
            if (entry.getValue().isEmpty()) FRAMES.remove(entry.getKey());
        }
        stack.remove(ModDataComponents.GUN_ACTIVE_GROUPS);
    }

    private static void publish(ServerPlayer owner, ItemStack stack, Map<String, Frame> groups) {
        if (stack.isEmpty()) return;
        var current = GunActiveGroups.of(stack);
        for (var entry : current.entries()) if (!entry.owner().equals(owner.getUUID())) return;
        if (groups == null || groups.isEmpty()) { stack.remove(ModDataComponents.GUN_ACTIVE_GROUPS); return; }
        var previous = GunActiveGroups.of(stack).entries();
        boolean same = previous.size() == groups.size();
        int index = 0;
        if (same) for (var frame : groups.values()) if (!previous.get(index++).equals(frame.descriptor)) { same = false; break; }
        if (same) return;
        var descriptors = new GunActiveGroups(groups.values().stream().map(frame -> frame.descriptor).toList());
        if (!descriptors.equals(GunActiveGroups.of(stack))) stack.set(ModDataComponents.GUN_ACTIVE_GROUPS, descriptors);
    }

    static boolean owns(ServerPlayer player, ItemStack stack, String key) {
        return frame(player, stack, key) != null;
    }

    static boolean isFinishing(ServerPlayer player, ItemStack stack, String key) {
        var frame = frame(player, stack, key);
        return frame != null && frame.finishing;
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
            if (entry.getValue().values().removeIf(frame -> frame.lastTick < now - 1)) publish(player, entry.getKey(), entry.getValue());
            return entry.getKey().isEmpty() || entry.getValue().isEmpty();
        });
        if (stacks.isEmpty()) FRAMES.remove(player);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        var stacks = FRAMES.remove(event.getEntity());
        if (stacks != null) stacks.keySet().forEach(stack -> publish((ServerPlayer)event.getEntity(), stack, null));
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) {
        FRAMES.forEach((owner, stacks) -> stacks.keySet().forEach(stack -> publish(owner, stack, null)));
        FRAMES.clear();
    }
}
