package net.pttheta.loveandwar.gun.runtime;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.gun.network.CGunAuthored;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Validate owner-authored callback phases; retain world-clock firing for server-origin groups. */
@EventBusSubscriber(modid = "createloveandwar")
public final class GunFireScheduler {
    private static final Map<ServerPlayer, IdentityHashMap<ItemStack, Cycle>> CYCLES = new WeakHashMap<>();

    private static final class Cycle {
        final GunCadence cadence = new GunCadence();
        long lastTick;
        long seenTick;
        boolean held;
        boolean clientControlled;
        GunDef gun;
        ActionGroup group;
        GunAttachments attachments;
        GunActiveGroups.Origin origin;
        int retriggers;
        int progress;
        int triggers;
        int duration;
        boolean finishing;
        GunAuthoredFire.Remote remote;

        Cycle(long now) { this.lastTick = now; this.seenTick = now; }

        void advance(long now) {
            int elapsed = (int)Math.min(Integer.MAX_VALUE, Math.max(0, now - this.lastTick));
            this.cadence.tick(elapsed);
            this.lastTick = now;
        }
    }

    private GunFireScheduler() {}

    private static Cycle cycle(ServerPlayer player, ItemStack stack) {
        var cycles = CYCLES.computeIfAbsent(player, ignored -> new IdentityHashMap<>());
        return cycles.computeIfAbsent(stack, ignored -> {
            // Runtime fire controls are deliberately not persisted, like the original ActionStack.
            var state = stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
            stack.set(ModDataComponents.GUN_ACTION_STATE, state.withBurstRemaining(0).withWaitingForRelease(false).withCooldown(0));
            return new Cycle(player.level().getGameTime());
        });
    }

    static boolean press(ServerPlayer player, ItemStack stack) {
        var cycle = cycle(player, stack);
        boolean fresh = !cycle.held;
        cycle.held = true;
        cycle.clientControlled |= GunPresentation.controlledInput();
        return fresh;
    }

    static void release(ServerPlayer player, ItemStack stack) {
        var cycles = CYCLES.get(player);
        var cycle = cycles == null ? null : cycles.get(stack);
        if (cycle == null) return;
        cycle.held = false;
        if (cycle.remote != null) {
            if (!cycle.remote.release(
                    GunPresentation.authored().present()
                            ? GunPresentation.authored().tick()
                            : cycle.remote.gate.tick)) cancel(player, stack);
            return;
        }
        if (cycle.group != null
                && GunFireMode.of(cycle.group.repeatMode()) == GunFireMode.FULL_AUTO) {
            GunActionTimeline.finishAfterTick(player, stack, cycle.group.key());
            finish(stack, cycle);
        }
    }

    static void releaseAll(ServerPlayer player) {
        var cycles = CYCLES.get(player);
        if (cycles == null) return;
        for (var entry : cycles.entrySet()) {
            release(player, entry.getKey());
            var state = entry.getKey().getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
            entry.getKey().set(ModDataComponents.GUN_ACTION_STATE, state.released());
        }
    }

    static void start(ServerPlayer player, ItemStack stack, GunDef gun, ActionGroup group, GunAttachments attachments) {
        start(player, stack, gun, group, attachments, GunActiveGroups.Origin.base(stack));
    }

    static void start(ServerPlayer player, ItemStack stack, GunDef gun, ActionGroup group, GunAttachments attachments, GunActiveGroups.Origin origin) {
        var retained = GunActionTimeline.context(player, stack, group.key());
        if (retained.isPresent()) {
            gun = retained.get().gun(); group = retained.get().group(); origin = retained.get().origin();
        }
        var cycle = cycle(player, stack);
        var authored = GunPresentation.authored();
        if (authored.present() && GunPresentation.claim().present()) {
            if (cycle.remote != null
                    && !cycle.remote.activation.equals(GunPresentation.claim().activation()))
                return;
            var remote =
                    cycle.remote == null
                            ? new GunAuthoredFire.Remote(GunPresentation.claim(), group)
                            : cycle.remote;
            if (!remote.start(player, stack, authored)) {
                if (cycle.remote != null) cancel(player, stack);
                return;
            }
            cycle.gun = gun;
            cycle.group = group;
            cycle.attachments = attachments;
            cycle.origin = origin;
            cycle.duration = durationPerTrigger(group);
            cycle.remote = remote;
            cycle.progress = authored.tick();
            cycle.lastTick = player.level().getGameTime();
            cycle.seenTick = cycle.lastTick;
            GunActionTimeline.beginAuthored(player, stack, gun, group, origin);
            GunActionTimeline.advanceAuthored(player, stack, group.key(), authored.tick());
            for (int i = 0; i < authored.count(); i++)
                GunEngine.triggerActions(player, stack, gun, group, attachments, origin);
            cycle.triggers = (int) Math.min(Integer.MAX_VALUE, remote.gate.triggers);
            synchronize(stack, cycle, player.level().getGameTime());
            return;
        }
        if (cycle.remote != null) return;
        cycle.advance(player.level().getGameTime());
        if (cycle.group == null || !cycle.group.key().equals(group.key())) {
            cycle.progress = 0;
            cycle.triggers = 0;
            cycle.finishing = false;
            cycle.remote = null;
        }
        cycle.gun = gun;
        cycle.group = group;
        cycle.attachments = attachments;
        cycle.origin = origin;
        cycle.duration = durationPerTrigger(group);
        emit(player, stack, cycle, cycle.cadence.trigger(group.repeatDelay()));
        cycle.retriggers = group.repeatDelay() > 0 && GunFireMode.of(group.repeatMode()) == GunFireMode.BURST_FIRE
                ? Math.max(0, group.repeatCount() - 1) : 0;
        synchronize(stack, cycle, player.level().getGameTime());
    }

    static void cancel(ServerPlayer player, ItemStack stack) {
        var cycles = CYCLES.get(player);
        var cycle = cycles == null ? null : cycles.get(stack);
        if (cycle != null) {
            if (cycle.group != null) GunActionTimeline.cancel(player, stack, cycle.group.key());
            cycle.group = null;
            cycle.retriggers = 0;
            cycle.finishing = false;
            cycle.remote = null;
        }
    }

    static void finishModeGroup(ServerPlayer player, ItemStack stack, String key) {
        var cycles = CYCLES.get(player);
        var cycle = cycles == null ? null : cycles.get(stack);
        // Retain the final pending feedback tick, without adding a client-only final prediction as server damage.
        if (cycle != null && cycle.group != null && cycle.group.key().equals(key)) cycle.finishing = true;
    }

    /** True when this scheduler owns the active group's progress for this inventory tick. */
    static boolean tick(ServerPlayer player, ItemStack stack) {
        var cycles = CYCLES.get(player);
        var cycle = cycles == null ? null : cycles.get(stack);
        if (cycle == null) {
            var state = stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
            if (!state.activeGroup().isEmpty() && GunLookup.gunDef(stack).stream().flatMap(def -> def.actionGroups().stream())
                    .anyMatch(group -> group.key().equals(state.activeGroup()) && group.actions().stream().anyMatch(action -> "Shoot".equals(action.actionType())))) {
                stack.set(ModDataComponents.GUN_ACTION_STATE, state.idled().withBurstRemaining(0).withWaitingForRelease(false).withCooldown(0));
            } else if (state.activeGroup().isEmpty() && (state.waitingForRelease() || state.burstRemaining() != 0 || state.cooldownUntilGameTime() != 0)) {
                stack.set(
                        ModDataComponents.GUN_ACTION_STATE,
                        state.withFiringFrame("", 0, 0, 0, 0, false));
            }
            return false;
        }
        long now = player.level().getGameTime();
        cycle.seenTick = now;
        if (!player.isAlive() || player.isSpectator()) {
            if (cycle.group != null) finish(stack, cycle);
            cycle.held = false;
            return false;
        }
        if (cycle.remote != null) {
            cycle.lastTick = now;
            if (cycle.group != null && !GunActionTimeline.owns(player, stack, cycle.group.key()))
                finish(stack, cycle);
            return cycle.group != null;
        }
        if (now <= cycle.lastTick) return cycle.group != null;
        // A dropped/unloaded stack must not resume a stale burst when picked up later.
        if (now - cycle.lastTick > 1) {
            if (cycle.group != null) finish(stack, cycle);
            cycle.held = false;
        }
        cycle.advance(now);
        if (cycle.group == null) {
            if (cycle.held && !cycle.clientControlled) GunEngine.tryHeldFire(player, stack);
            return cycle.group != null;
        }

        cycle.progress++;
        GunFireMode mode = GunFireMode.of(cycle.group.repeatMode());
        boolean repeat = !cycle.finishing && mode == GunFireMode.FULL_AUTO && cycle.held;
        if (!cycle.finishing && mode == GunFireMode.BURST_FIRE && cycle.retriggers > 0) {
            // Original CheckRetrigger counts checks, not successful shots, even while out of ammo.
            cycle.retriggers--;
            repeat = true;
        }
        if (repeat && !stack.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY).isEmpty()) {
            emit(player, stack, cycle, cycle.cadence.trigger(cycle.group.repeatDelay() <= 0 ? 0.05F : cycle.group.repeatDelay()));
        }
        if (cycle.finishing || (mode == GunFireMode.BURST_FIRE || mode == GunFireMode.SEMI_AUTO)
                && cycle.progress > (long)cycle.duration * cycle.triggers) {
            finish(stack, cycle);
        } else synchronize(stack, cycle, now);
        return true;
    }

    static int progress(ServerPlayer player, ItemStack stack, String group) {
        var cycles = CYCLES.get(player);
        var cycle = cycles == null ? null : cycles.get(stack);
        if (cycle != null && cycle.group != null && cycle.group.key().equals(group)) return cycle.progress;
        var state = stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
        return state.activeGroup().equals(group) ? state.groupTicksElapsed() : 0;
    }

    static boolean owns(ServerPlayer player, ItemStack stack, String group) {
        var cycles = CYCLES.get(player);
        var cycle = cycles == null ? null : cycles.get(stack);
        return cycle != null && cycle.group != null && cycle.group.key().equals(group);
    }

    private static void emit(ServerPlayer player, ItemStack stack, Cycle cycle, int count) {
        for (int i = 0; i < count; i++) {
            if (!GunAuthoredFire.spend(player, stack, cycle.group.repeatDelay(), 1)) break;
            cycle.triggers++;
            GunEngine.triggerActions(player, stack, cycle.gun, cycle.group, cycle.attachments, cycle.origin);
        }
    }

    private static void synchronize(ItemStack stack, Cycle cycle, long now) {
        var state = stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
        long total = (long)cycle.duration * cycle.triggers + 1;
        stack.set(ModDataComponents.GUN_ACTION_STATE, state.withFiringFrame(cycle.group.key(), cycle.progress,
                (int)Math.min(Integer.MAX_VALUE, total), now + cycle.cadence.ticksUntilReady(), cycle.retriggers,
                cycle.held && GunFireMode.of(cycle.group.repeatMode()) != GunFireMode.FULL_AUTO));
    }

    private static void finish(ItemStack stack, Cycle cycle) {
        cycle.group = null;
        cycle.retriggers = 0;
        cycle.finishing = false;
        cycle.remote = null;
        var state = stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
        stack.set(ModDataComponents.GUN_ACTION_STATE, state.idled().withBurstRemaining(0));
    }

    public static void authored(ServerPlayer player, ItemStack stack, CGunAuthored packet) {
        var cycles = CYCLES.get(player);
        var cycle = cycles == null ? null : cycles.get(stack);
        if (cycle == null
                || cycle.remote == null
                || cycle.group == null
                || !cycle.remote.activation.equals(packet.activation())) return;
        if (!player.isAlive() || player.isSpectator()) {
            cancel(player, stack);
            return;
        }
        var remote = cycle.remote;
        var gate = remote.gate;
        if (packet.tick() <= gate.tick
                && (packet.kind() == CGunAuthored.Kind.TRIGGER && packet.ordinal() < gate.triggers
                        || packet.kind() == CGunAuthored.Kind.STEP
                                && packet.ordinal() <= gate.triggers))
            return; // Idempotent old delivery; never replay gameplay.
        boolean valid = remote.time(packet.tick());
        if (valid)
            switch (packet.kind()) {
                case TRIGGER ->
                        valid =
                                gate.trigger(packet.tick(), packet.ordinal())
                                        && GunAuthoredFire.spend(
                                                player,
                                                stack,
                                                gate.delay <= 0 ? .05F : gate.delay,
                                                1);
                case STEP -> valid = gate.step(packet.tick(), packet.ordinal());
                case FINISH -> valid = gate.finish(packet.tick(), packet.ordinal());
                case CANCEL -> valid = gate.step(packet.tick(), packet.ordinal());
            }
        if (!valid) {
            cancel(player, stack);
            return;
        }
        GunActionTimeline.advanceAuthored(player, stack, cycle.group.key(), packet.tick());
        if (cycle.group == null) return;
        if (packet.kind() == CGunAuthored.Kind.TRIGGER)
            GunEngine.triggerActions(
                    player,
                    stack,
                    cycle.gun,
                    cycle.group,
                    stack.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY),
                    cycle.origin);
        if (cycle.group == null) return;
        cycle.progress = packet.tick();
        cycle.triggers = (int) Math.min(Integer.MAX_VALUE, gate.triggers);
        if (packet.kind() == CGunAuthored.Kind.FINISH
                || packet.kind() == CGunAuthored.Kind.CANCEL) {
            cancel(player, stack);
            finish(stack, cycle);
        } else synchronize(stack, cycle, player.level().getGameTime());
    }

    private static int durationPerTrigger(ActionGroup group) {
        return GunEngine.groupDurationTicks(group);
    }

    @SubscribeEvent
    public static void prune(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var cycles = CYCLES.get(player);
        if (cycles == null) return;
        long now = player.level().getGameTime();
        cycles.entrySet().removeIf(entry -> entry.getKey().isEmpty() || entry.getValue().seenTick < now - 1
                || entry.getValue().group == null && !entry.getValue().held && entry.getValue().cadence.idle());
        if (cycles.isEmpty()) CYCLES.remove(player);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        CYCLES.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) { CYCLES.clear(); }
}
