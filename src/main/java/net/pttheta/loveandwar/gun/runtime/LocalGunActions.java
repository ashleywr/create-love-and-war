package net.pttheta.loveandwar.gun.runtime;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.gun.content.stat.ModifierContext;
import net.pttheta.loveandwar.gun.content.stat.StatResolver;
import net.pttheta.loveandwar.gun.network.CGunInput;
import net.pttheta.loveandwar.gun.network.GunPredictionClaim;
import net.pttheta.loveandwar.gun.network.GunShotReceipt;
import net.pttheta.loveandwar.gun.network.SGunPresentationEnd;
import net.pttheta.loveandwar.item.ModDataComponents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Owning-player firing presentation. Never mutates the authoritative inventory or creates damage.
 */
public final class LocalGunActions {
    @FunctionalInterface
    public interface Shot {
        void run(ItemStack gun, String group, float horizontal, float vertical, long index);
    }

    private final Player player;
    private final Shot shot;
    private final ItemStack shadow;
    private final PredictedGunAmmo ammo = new PredictedGunAmmo();
    private final GunCadence cadence = new GunCadence();
    private final List<Frame> frames = new ArrayList<>();
    private final Set<String> held = new HashSet<>();
    private boolean reloading;
    private GunActiveGroups remote = GunActiveGroups.EMPTY;
    private GunActiveGroups remoteSource;

    private final class Frame {
        final UUID id = UUID.randomUUID();
        final GunDef gun;
        final ActionGroup group;
        final GunActiveGroups.Origin origin;
        final String input;
        final ActionCallbackClock clock;
        final GunActiveGroups.Entry view;
        int bursts;
        boolean finishing;

        Frame(GunEngine.PredictedContext context, String input) {
            gun = context.gun();
            group = context.group();
            origin = context.origin();
            this.input = input;
            clock =
                    new ActionCallbackClock(
                            group.actions().size(), i -> group.actions().get(i).delay());
            ammo.begin(id);
            view =
                    new GunActiveGroups.Entry(
                            group.key(),
                            origin,
                            id,
                            player.level().getGameTime(),
                            player.getUUID());
        }

        GunPredictionClaim claim() {
            return new GunPredictionClaim(id, group.key(), origin);
        }

        void trigger(int count) {
            for (int n = 0; n < count; n++) clock.trigger(i -> true, this::callback);
        }

        void callback(int action, long index, int progress) {
            if (!group.actions().get(action).actionType().equals("Shoot")) return;
            var round = ammo.consume(id);
            var sources =
                    StatSources.forFiring(
                            gun,
                            group,
                            shadow.getOrDefault(
                                    ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY));
            var context = ModifierContext.ofGroups(List.of(group.key()));
            boolean produced =
                    GunLookup.bulletDef(round)
                            .map(
                                    bullet ->
                                            bullet.hitscans().stream()
                                                            .anyMatch(
                                                                    v ->
                                                                            StatResolver.resolve(
                                                                                            "shot_count",
                                                                                            v.core()
                                                                                                    .shotCount(),
                                                                                            sources,
                                                                                            context)
                                                                                    > 0)
                                                    || bullet.projectiles().stream()
                                                            .anyMatch(
                                                                    v ->
                                                                            StatResolver.resolve(
                                                                                            "shot_count",
                                                                                            v.core()
                                                                                                    .shotCount(),
                                                                                            sources,
                                                                                            context)
                                                                                    > 0))
                            .orElse(false);
            float h =
                    produced
                            ? Math.max(
                                    0,
                                    StatResolver.resolve("horizontal_recoil", 0, sources, context))
                            : 0;
            float v =
                    produced
                            ? Math.max(
                                    0, StatResolver.resolve("vertical_recoil", 0, sources, context))
                            : 0;
            shot.run(shadow, group.key(), h, v, index);
            shadow.set(ModDataComponents.GUN_LOADED_ROUNDS, ammo.rounds());
        }
    }

    public LocalGunActions(Player player, ItemStack gun, Shot shot) {
        this.player = player;
        this.shot = shot;
        shadow = gun.copy();
        sync(gun);
    }

    public void sync(ItemStack actual) {
        ammo.observe(actual);
        shadow.set(
                ModDataComponents.GUN_ATTACHMENTS,
                actual.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY));
        shadow.set(ModDataComponents.GUN_LOADED_ROUNDS, ammo.rounds());
        var current = GunActiveGroups.of(actual);
        if (remoteSource == null || !current.equals(remoteSource)) {
            remoteSource = current;
            remote =
                    new GunActiveGroups(
                            current.entries().stream()
                                    .filter(
                                            entry ->
                                                    entry.resolve()
                                                            .map(
                                                                    group ->
                                                                            group.actions().stream()
                                                                                    .noneMatch(
                                                                                            a ->
                                                                                                    a.actionType()
                                                                                                            .equals(
                                                                                                                    "Shoot")))
                                                            .orElse(false))
                                    .toList());
            publish();
        }
        var state = actual.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
        String active = state.activeGroup();
        reloading =
                GunLookup.gunDef(actual)
                        .map(
                                def ->
                                        def.reloads().stream()
                                                .anyMatch(
                                                        r ->
                                                                active.equals(r.startActionKey())
                                                                        || active.equals(
                                                                                r.ejectActionKey())
                                                                        || active.equals(
                                                                                r
                                                                                        .loadOneActionKey())
                                                                        || active.equals(
                                                                                r.endActionKey())))
                        .orElse(false);
        if (!reloading) {
            state = state.idled().withCooldown(0).withWaitingForRelease(held.contains("Fire1"));
            if (!frames.isEmpty()) {
                var frame = frames.getFirst();
                state =
                        state.withFiringFrame(
                                frame.group.key(),
                                frame.clock.progress(),
                                (int)
                                        Math.min(
                                                Integer.MAX_VALUE,
                                                (long) frame.group.durationTicks()
                                                                * frame.clock.triggers()
                                                        + 1),
                                0,
                                0,
                                held.contains("Fire1"));
            }
        }
        shadow.set(ModDataComponents.GUN_ACTION_STATE, state);
    }

    public CGunInput input(CGunInput input, ItemStack actual) {
        sync(actual);
        if (!input.pressed()) {
            held.remove(input.inputType());
            for (var frame : frames)
                if ((input.prediction().present()
                                ? frame.id.equals(input.prediction().activation())
                                : frame.input.equals(input.inputType()))
                        && GunFireMode.of(frame.group.repeatMode()) == GunFireMode.FULL_AUTO)
                    frame.finishing = true;
            return input;
        }
        if (!player.isAlive() || player.isSpectator()) return input;
        boolean fresh = held.add(input.inputType());
        if (!fresh) return input;
        var predicted =
                GunEngine.predictInputContext(
                        player, shadow, input.inputType(), player.level().getGameTime());
        if (predicted.isEmpty()) return input;
        var context = predicted.get();
        var group = context.group();
        if (group.actions().stream().noneMatch(a -> a.actionType().equals("Shoot"))) {
            if (context.gun().reloads().stream()
                    .anyMatch(r -> r.startActionKey().equals(group.key()))) cancel();
            return input;
        }
        var mode = GunFireMode.of(group.repeatMode());
        if (mode != GunFireMode.SEMI_AUTO
                && mode != GunFireMode.FULL_AUTO
                && mode != GunFireMode.BURST_FIRE) return input;
        Frame frame = null;
        for (var value : frames)
            if (value.group.key().equals(group.key())) {
                frame = value;
                break;
            }
        if (frame == null) {
            frame = new Frame(context, input.inputType());
            frames.add(frame);
            publish();
        }
        frame.finishing = false;
        frame.trigger(cadence.trigger(frame.group.repeatDelay()));
        frame.bursts =
                frame.group.repeatDelay() > 0
                                && GunFireMode.of(frame.group.repeatMode())
                                        == GunFireMode.BURST_FIRE
                        ? Math.max(0, frame.group.repeatCount() - 1)
                        : 0;
        return input.predicted(frame.claim());
    }

    /**
     * Source autonomous owner callbacks run at client START; global recoil integrates later at END.
     */
    public void tick(ItemStack actual) {
        sync(actual);
        cadence.tick(1);
        if (!player.isAlive() || player.isSpectator()) {
            cancel();
            return;
        }
        for (int i = frames.size() - 1; i >= 0; i--) {
            var frame = frames.get(i);
            frame.clock.tick(frame::callback);
            var mode = GunFireMode.of(frame.group.repeatMode());
            boolean repeat = mode == GunFireMode.FULL_AUTO;
            if (mode == GunFireMode.BURST_FIRE && frame.bursts > 0) {
                frame.bursts--;
                repeat = true;
            }
            if (repeat && !ammo.rounds().isEmpty() && !reloading)
                frame.trigger(
                        cadence.trigger(
                                frame.group.repeatDelay() <= 0 ? .05F : frame.group.repeatDelay()));
            if (frame.finishing
                    || ((mode == GunFireMode.SEMI_AUTO || mode == GunFireMode.BURST_FIRE)
                            && frame.clock.pastDuration(frame.group.durationTicks()))) {
                frames.remove(i);
                ammo.localFinished(frame.id);
                publish();
            }
        }
    }

    private void publish() {
        var entries = new ArrayList<GunActiveGroups.Entry>();
        for (var entry : remote.entries())
            if (frames.stream().noneMatch(f -> f.group.key().equals(entry.key())))
                entries.add(entry);
        for (var frame : frames) entries.add(frame.view);
        if (entries.isEmpty()) shadow.remove(ModDataComponents.GUN_ACTIVE_GROUPS);
        else shadow.set(ModDataComponents.GUN_ACTIVE_GROUPS, new GunActiveGroups(entries));
    }

    public void acknowledge(GunShotReceipt receipt) {
        ammo.acknowledge(
                receipt.prediction(),
                receipt.consumed(),
                receipt.ammoRevision(),
                receipt.remainingRounds());
    }

    public void end(SGunPresentationEnd end) {
        ammo.end(end.prediction(), end.consumed(), end.ammoRevision(), end.remainingRounds());
    }

    public int rounds() {
        return ammo.rounds().items().size();
    }

    public boolean matches(ItemStack gun) {
        return shadow.getItem() == gun.getItem();
    }

    public boolean idle() {
        return frames.isEmpty() && held.isEmpty() && ammo.idle() && cadence.idle();
    }

    public void cancel() {
        for (var frame : frames) ammo.localFinished(frame.id);
        frames.clear();
        held.clear();
        publish();
    }
}
