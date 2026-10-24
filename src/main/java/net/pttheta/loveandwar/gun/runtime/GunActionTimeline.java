package net.pttheta.loveandwar.gun.runtime;

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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Original per-action pending slot semantics, scoped to server-owned runtime group instances. */
@EventBusSubscriber(modid = "createloveandwar")
public final class GunActionTimeline {
    private static final Map<ServerPlayer, IdentityHashMap<ItemStack, Map<String, Frame>>> FRAMES = new WeakHashMap<>();
    private static final ActionCallbackClock.Callback NO_CALLBACK = (index, trigger, progress) -> {};
    private static final class Frame {
        final GunDef gun;
        final ActionGroup group;
        GunActiveGroups.Entry descriptor;
        final boolean delayedScope;
        final ActionCallbackClock clock;
        final GunPresentation.Activation presentation;
        long lastTick;
        long tickedAt = Long.MIN_VALUE;
        long stateTickedAt = Long.MIN_VALUE;
        boolean finishing;

        Frame(ServerPlayer player, ItemStack stack, GunDef gun, ActionGroup group, GunActiveGroups.Origin origin, long now) {
            this.gun = gun; this.group = group; this.lastTick = now;
            descriptor = new GunActiveGroups.Entry(group.key(), origin, UUID.randomUUID(), now, player.getUUID());
            presentation = new GunPresentation.Activation(stack,descriptor.activation(),group.key(),origin);
            delayedScope=group.actions().stream().anyMatch(action->"Scope".equals(action.actionType())&&action.delay()!=0);
            clock = new ActionCallbackClock(group.actions().size(), index -> group.actions().get(index).delay());
        }
    }

    /** Ephemeral dispatch context: never stored in FRAMES, so it cannot retain a weak-map player key. */
    private static final class Dispatch implements ActionCallbackClock.Callback {
        final ServerPlayer player;
        final ItemStack stack;
        final Frame frame;
        List<ModifierSource> sources;

        Dispatch(ServerPlayer player, ItemStack stack, Frame frame, List<ModifierSource> sources) {
            this.player = player; this.stack = stack; this.frame = frame; this.sources = sources;
        }

        @Override public void run(int index, long trigger, int progress) {
            if (sources == null) {
                var attachments = stack.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY);
                sources = RealisticDamage.apply(GunLookup.contentKey(stack), frame.group, StatSources.forFiring(frame.gun, frame.group, attachments));
            }
            frame.presentation.run(index,()->GunEngine.executeAction(player, stack, frame.gun, frame.group, frame.group.actions().get(index), sources, progress, trigger));
        }
    }
    private GunActionTimeline() {}

    private static boolean eligible(ActionDef action, boolean soundsOnly) {
        return !soundsOnly
                || action.actionType().equals("PlaySound")
                || action.actionType().equals("EjectCasing");
    }
    static void beginAuthored(ServerPlayer player, ItemStack stack, GunDef gun, ActionGroup group, GunActiveGroups.Origin origin) {
        claim(player, stack);
        var groups = FRAMES.computeIfAbsent(player, ignored -> new IdentityHashMap<>())
                .computeIfAbsent(stack, ignored -> new LinkedHashMap<>());
        if (!groups.containsKey(group.key())) {
            groups.put(group.key(), new Frame(player, stack, gun, group, origin, player.level().getGameTime()));
            publish(player, stack, groups);
        }
    }

    static void advanceAuthored(ServerPlayer player, ItemStack stack, String key, int tick) {
        var frame = frame(player, stack, key);
        if (frame == null || !frame.presentation.authored || tick < frame.clock.progress()) return;
        frame.clock.advance(
                tick - frame.clock.progress(), new Dispatch(player, stack, frame, null));
    }

    static void trigger(
            ServerPlayer player,
            ItemStack stack,
            GunDef gun,
            ActionGroup group,
            GunAttachments attachments,
            boolean soundsOnly) {
        trigger(
                player,
                stack,
                gun,
                group,
                attachments,
                soundsOnly,
                GunActiveGroups.Origin.base(stack));
    }

    static void trigger(
            ServerPlayer player,
            ItemStack stack,
            GunDef gun,
            ActionGroup group,
            GunAttachments attachments,
            boolean soundsOnly,
            GunActiveGroups.Origin origin) {
        claim(player, stack);
        var groups = FRAMES.computeIfAbsent(player, ignored -> new IdentityHashMap<>()).computeIfAbsent(stack, ignored -> new LinkedHashMap<>());
        var frame = groups.get(group.key());
        if (frame == null) {
            frame = new Frame(player, stack, gun, group, origin, player.level().getGameTime());
            groups.put(group.key(), frame);
            publish(player, stack, groups);
        }
        // Quirk kept: ActionStack aliases Def.key and reuses the first instance's actual definition/context.
        gun = frame.gun;
        group = frame.group;
        if(frame.delayedScope){
            var previous=frame.descriptor;
            frame.descriptor=new GunActiveGroups.Entry(previous.key(),previous.origin(),previous.activation(),previous.startedAt(),previous.owner(),frame.clock.triggers()+1);
            publish(player,stack,groups);
        }
        var sources = RealisticDamage.apply(GunLookup.contentKey(stack), group, StatSources.forFiring(gun, group, attachments));
        var actual = frame.group;
        frame.clock.trigger(index -> eligible(actual.actions().get(index), soundsOnly), new Dispatch(player, stack, frame, sources));
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
            if (frame.presentation.authored && !frame.finishing) {
                frame.lastTick = now;
                continue;
            }
            if (now <= frame.lastTick) continue;
            // Runtime pending work must not resume after a dropped/unloaded interval.
            if (now - frame.lastTick > 1) { frame.presentation.retire(player,stack); iterator.remove(); removed = true; continue; }
            frame.lastTick = now; frame.tickedAt = now;
            // Stable authored-index order is retained; original HashMap same-time ordering was unspecified.
            // No dispatch/stat allocation on the ordinary no-pending tick path.
            frame.clock.tick(frame.clock.hasPending() ? new Dispatch(player, stack, frame, null) : NO_CALLBACK);
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
            boolean remove = frame.tickedAt == now && (!frame.presentation.authored||frame.finishing) && (frame.finishing
                || (GunFireMode.of(frame.group.repeatMode()) == GunFireMode.SEMI_AUTO || GunFireMode.of(frame.group.repeatMode()) == GunFireMode.BURST_FIRE)
                && frame.clock.pastDuration(GunEngine.groupDurationTicks(frame.group)));
            if (remove && frame.finishing) {
                var state = stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
                if (state.activeToggles().contains(frame.group.key())) stack.set(ModDataComponents.GUN_ACTION_STATE, state.withToggle(frame.group.key(), false));
            }
            if (remove) frame.presentation.retire(player,stack);
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
        var removed=groups==null?null:groups.remove(key);
        if (removed!=null) { removed.presentation.retire(player,stack); publish(player, stack, groups); }
    }

    static void cancelAll(ServerPlayer player, ItemStack stack) {
        var stacks = FRAMES.get(player);
        var removed=stacks==null?null:stacks.remove(stack);
        if(removed!=null)removed.values().forEach(frame->frame.presentation.retire(player,stack));
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
        if(frame!=null)GunPresentation.confirm(frame.presentation);
        return frame == null ? java.util.Optional.empty() : java.util.Optional.of(new Context(frame.gun, frame.group, frame.descriptor.origin(), frame.clock.progress(), frame.clock.triggers()));
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
    static boolean permitsPresentation(ServerPlayer player,ItemStack stack,String key){
        var frame=frame(player,stack,key);return frame==null||GunPresentation.permits(frame.presentation);
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
            if (entry.getValue().values().removeIf(frame -> {
                if(frame.lastTick>=now-1)return false;frame.presentation.retire(player,entry.getKey());return true;
            })) publish(player, entry.getKey(), entry.getValue());
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
