package net.pttheta.loveandwar.gun.runtime;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.pttheta.loveandwar.gun.network.CGunAuthored;
import net.pttheta.loveandwar.gun.network.CGunInput;
import net.pttheta.loveandwar.gun.network.GunNetworking;
import net.pttheta.loveandwar.gun.network.GunPredictionClaim;
import net.pttheta.loveandwar.gun.network.SGunPresentationEnd;
import net.pttheta.loveandwar.gun.network.SGunRecoil;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class GunAuthoredTriggerGameTests {
    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void impossibleReleasePhaseCannotExtendAuthoredFullAutoLifetime(GameTestHelper h)
            throws ReflectiveOperationException {
        try (var owner = new GunAudioGameTests.Listener(h)) {
            var gun = prepare(owner, "mat49", 4);
            var start = press(owner, gun);
            input(owner, start);
            input(owner, start.released().authored(100_000, 1, 0));
            authored(
                    owner,
                    new CGunAuthored(
                            start.gunId(),
                            start.prediction().activation(),
                            2,
                            1,
                            CGunAuthored.Kind.TRIGGER));
            h.assertTrue(
                    GunAmmo.count(gun) == 3 && ends(owner) == 1,
                    "Release phase obeys the same time/lifetime checks as trigger phases");
        }
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void allBundledInitialBatchesMatchOwnerAndAuthoritativeConsumption(
            GameTestHelper h) throws ReflectiveOperationException {
        int checked = 0;
        for (var entry : net.pttheta.loveandwar.gun.content.GunContent.guns().entrySet()) {
            try (var owner = new GunAudioGameTests.Listener(h)) {
                var gun = prepare(owner, entry.getKey(), 3);
                var magazine =
                        GunLookup.resolveMagazineForSlot(entry.getValue().magazines().getFirst())
                                .orElseThrow();
                var round =
                        net.pttheta.loveandwar.gun.content.GunContent.bullets().keySet().stream()
                                .map(
                                        key ->
                                                new ItemStack(
                                                        BuiltInRegistries.ITEM.get(
                                                                ResourceLocation.parse(
                                                                        "createloveandwar:"
                                                                                + key))))
                                .filter(
                                        stack ->
                                                GunLookup.matchesBullet(
                                                        magazine.matchingBullets(), stack))
                                .findFirst()
                                .orElseThrow();
                gun.set(
                        ModDataComponents.GUN_LOADED_ROUNDS,
                        new AmmoPouchContents(List.of(round, round.copy(), round.copy())));
                var shadow = gun.copy();
                var player =
                        new Player(
                                h.getLevel(),
                                h.absolutePos(new BlockPos(2, 3, 2)),
                                0,
                                owner.player.getGameProfile()) {
                            @Override
                            public boolean isSpectator() {
                                return false;
                            }

                            @Override
                            public boolean isCreative() {
                                return false;
                            }
                        };
                player.setPos(owner.player.position());
                player.setItemInHand(InteractionHand.MAIN_HAND, shadow);
                var indices = new ArrayList<Long>();
                var local =
                        new LocalGunActions(
                                player, shadow, (g, key, x, y, index) -> indices.add(index));
                var packet =
                        local.input(
                                CGunInput.capture(player, InteractionHand.MAIN_HAND, "Fire1")
                                        .orElseThrow(),
                                shadow);
                input(owner, packet);
                h.assertTrue(
                        packet.authored().present() && !indices.isEmpty(),
                        "Catalog gun authors an initial batch: " + entry.getKey());
                h.assertTrue(
                        local.rounds() == GunAmmo.count(gun)
                                && indices.equals(
                                        shots(owner).stream()
                                                .map(SGunRecoil::triggerIndex)
                                                .toList()),
                        "Owner/server ammunition and callback indices match: " + entry.getKey());
                h.assertTrue(
                        shots(owner).stream().allMatch(p -> p.receipt().clientOwned()),
                        "Catalog callback never replays owner recoil: " + entry.getKey());
                GunFireScheduler.cancel(owner.player, gun);
                checked++;
            }
        }
        h.assertTrue(checked == 49, "All49 bundled initial batches exercised: " + checked);
        h.succeed();
    }

    private static void authored(GunAudioGameTests.Listener owner, CGunAuthored packet)
            throws ReflectiveOperationException {
        var handler =
                GunNetworking.class.getDeclaredMethod(
                        "handleAuthored", CGunAuthored.class, IPayloadContext.class);
        handler.setAccessible(true);
        handler.invoke(null, packet, context(owner));
    }

    private static ItemStack prepare(GunAudioGameTests.Listener owner, String key, int count) {
        var gun =
                new ItemStack(
                        BuiltInRegistries.ITEM.get(
                                ResourceLocation.parse("createloveandwar:" + key)));
        String ammo = key.equals("g11k2") ? "caseless_bullet" : "pistol_bullet";
        gun.set(
                ModDataComponents.GUN_LOADED_ROUNDS,
                new AmmoPouchContents(
                        java.util.stream.IntStream.range(0, count)
                                .mapToObj(
                                        i ->
                                                new ItemStack(
                                                        BuiltInRegistries.ITEM.get(
                                                                ResourceLocation.parse(
                                                                        "createloveandwar:"
                                                                                + ammo))))
                                .toList()));
        owner.player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        owner.player.setXRot(-90);
        GunIdentity.inventoryTick(owner.player);
        return gun;
    }

    private static CGunInput press(GunAudioGameTests.Listener owner, ItemStack gun) {
        return CGunInput.capture(owner.player, InteractionHand.MAIN_HAND, "Fire1")
                .orElseThrow()
                .predicted(
                        new GunPredictionClaim(
                                java.util.UUID.randomUUID(),
                                "primary_fire",
                                GunActiveGroups.Origin.base(gun)))
                .authored(0, 0, 1);
    }

    private static List<SGunRecoil> shots(GunAudioGameTests.Listener owner) {
        return owner.packets.stream()
                .filter(
                        p ->
                                p
                                                instanceof
                                                net.minecraft.network.protocol.common
                                                                        .ClientboundCustomPayloadPacket
                                                                c
                                        && c.payload() instanceof SGunRecoil)
                .map(
                        p ->
                                (SGunRecoil)
                                        ((net.minecraft.network.protocol.common
                                                                .ClientboundCustomPayloadPacket)
                                                        p)
                                                .payload())
                .toList();
    }

    private static long ends(GunAudioGameTests.Listener owner) {
        return owner.packets.stream()
                .filter(
                        p ->
                                p
                                                instanceof
                                                net.minecraft.network.protocol.common
                                                                        .ClientboundCustomPayloadPacket
                                                                c
                                        && c.payload() instanceof SGunPresentationEnd)
                .count();
    }

    @FunctionalInterface
    private interface Checked {
        void run() throws ReflectiveOperationException;
    }

    private static void definition(String group, Checked test) throws ReflectiveOperationException {
        var json =
                net.pttheta.loveandwar.gun.content.GunDef.CODEC
                        .encodeStart(
                                com.mojang.serialization.JsonOps.INSTANCE,
                                net.pttheta.loveandwar.gun.content.GunContent.gun("mars")
                                        .orElseThrow())
                        .getOrThrow()
                        .getAsJsonObject();
        json.add(
                "inputHandlers",
                com.google.gson.JsonParser.parseString(
                        "[{\"inputType\":\"Fire1\",\"nodes\":[{\"actionGroupToTrigger\":\"primary_fire\"}]}]"));
        json.add("actionGroups", com.google.gson.JsonParser.parseString("[" + group + "]"));
        var old =
                net.pttheta.loveandwar.gun.content.GunContent.guns()
                        .put(
                                "mars",
                                net.pttheta.loveandwar.gun.content.GunDef.CODEC
                                        .parse(com.mojang.serialization.JsonOps.INSTANCE, json)
                                        .getOrThrow());
        try {
            test.run();
        } finally {
            net.pttheta.loveandwar.gun.content.GunContent.guns().put("mars", old);
        }
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void delayedAuthoredCallbacksWaitForOwnerPhaseAndRetainOverwriteCounter(
            GameTestHelper h) throws ReflectiveOperationException {
        definition(
                "{\"key\":\"primary_fire\",\"repeatMode\":\"SemiAuto\",\"actions\":[{\"actionType\":\"Shoot\",\"delay\":2.9,\"duration\":1}]}",
                () -> {
                    try (var owner = new GunAudioGameTests.Listener(h)) {
                        var gun = prepare(owner, "mars", 4);
                        var start = press(owner, gun);
                        input(owner, start);
                        input(owner, start.released());
                        input(owner, start.authored(1, 1, 1));
                        input(owner, start.released().authored(1, 2, 0));
                        for (int i = 0; i < 8; i++) GunEngine.tick(owner.player, gun);
                        h.assertTrue(
                                shots(owner).isEmpty() && GunAmmo.count(gun) == 4,
                                "World clock cannot prematurely execute owner-delayed callbacks");
                        var due =
                                new CGunAuthored(
                                        start.gunId(),
                                        start.prediction().activation(),
                                        3,
                                        2,
                                        CGunAuthored.Kind.STEP);
                        authored(owner, due);
                        authored(owner, due);
                        authored(
                                owner,
                                new CGunAuthored(
                                        start.gunId(),
                                        start.prediction().activation(),
                                        2,
                                        2,
                                        CGunAuthored.Kind.STEP));
                        h.assertTrue(
                                GunAmmo.count(gun) == 3
                                        && shots(owner).stream()
                                                .map(SGunRecoil::triggerIndex)
                                                .toList()
                                                .equals(List.of(2L)),
                                "Overwritten callback executes once at current count2;"
                                        + " duplicate/stale steps do not replay");
                        h.assertTrue(
                                GunActionTimeline.owns(owner.player, gun, "primary_fire"),
                                "Harmless duplicate steps keep the retained instance");
                        GunFireScheduler.cancel(owner.player, gun);
                    }
                });
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void atomicSubTickInitialBatchPreservesMultipleShootAndEmptyCallbacks(
            GameTestHelper h) throws ReflectiveOperationException {
        definition(
                "{\"key\":\"primary_fire\",\"repeatMode\":\"SemiAuto\",\"repeatDelay\":0.025,\"actions\":[{\"actionType\":\"Shoot\"},{\"actionType\":\"Shoot\"}]}",
                () -> {
                    try (var owner = new GunAudioGameTests.Listener(h)) {
                        var gun = prepare(owner, "mars", 1);
                        var start = press(owner, gun).authored(0, 0, 2);
                        input(owner, start);
                        h.assertTrue(
                                GunAmmo.count(gun) == 0
                                        && shots(owner).stream()
                                                .map(SGunRecoil::triggerIndex)
                                                .toList()
                                                .equals(List.of(0L, 0L, 1L, 1L)),
                                "Atomic initial batch keeps both actions and empty callbacks with"
                                        + " original indices");
                        authored(
                                owner,
                                new CGunAuthored(
                                        start.gunId(),
                                        start.prediction().activation(),
                                        1,
                                        2,
                                        CGunAuthored.Kind.FINISH));
                        h.assertTrue(
                                ends(owner) == 1,
                                "Sub-tick semi instance retires after its final phase");
                    }
                });
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void releasedG11BatchedTriggersRemainAuthoritativeWithoutWorldTickRepeats(
            GameTestHelper h) throws ReflectiveOperationException {
        try (var owner = new GunAudioGameTests.Listener(h)) {
            var gun = prepare(owner, "g11k2", 12);
            var start = press(owner, gun);
            input(owner, start);
            input(owner, start.released());
            for (int i = 0; i < 8; i++) GunEngine.tick(owner.player, gun);
            h.assertTrue(
                    GunAmmo.count(gun) == 11 && shots(owner).size() == 1,
                    "World ticks cannot synthesize owner-authored repeats");
            authored(
                    owner,
                    new CGunAuthored(
                            start.gunId(),
                            start.prediction().activation(),
                            1,
                            1,
                            CGunAuthored.Kind.TRIGGER));
            authored(
                    owner,
                    new CGunAuthored(
                            start.gunId(),
                            start.prediction().activation(),
                            2,
                            2,
                            CGunAuthored.Kind.TRIGGER));
            authored(
                    owner,
                    new CGunAuthored(
                            start.gunId(),
                            start.prediction().activation(),
                            4,
                            3,
                            CGunAuthored.Kind.FINISH));
            h.assertTrue(
                    GunAmmo.count(gun) == 9
                            && shots(owner).stream()
                                    .map(SGunRecoil::triggerIndex)
                                    .toList()
                                    .equals(List.of(0L, 1L, 2L)),
                    "Coalesced released burst applies every authoritative callback once");
            h.assertTrue(
                    ends(owner) == 1 && !GunActionTimeline.owns(owner.player, gun, "primary_fire"),
                    "Premature finish is rejected by retiring the activation without more damage");
        }
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void actualG11OwnerClockRetiresNaturallyAfterItsFullAnimationLifetime(
            GameTestHelper h) throws ReflectiveOperationException {
        var owner = new GunAudioGameTests.Listener(h);
        var gun = prepare(owner, "g11k2", 12);
        var actual = gun.copy();
        var player =
                new Player(
                        h.getLevel(),
                        h.absolutePos(new BlockPos(2, 3, 2)),
                        0,
                        owner.player.getGameProfile()) {
                    @Override
                    public boolean isSpectator() {
                        return false;
                    }

                    @Override
                    public boolean isCreative() {
                        return false;
                    }
                };
        player.setPos(owner.player.position());
        player.setItemInHand(InteractionHand.MAIN_HAND, actual);
        var local = new LocalGunActions(player, actual, (g, key, x, y, index) -> {});
        var start =
                local.input(
                        CGunInput.capture(player, InteractionHand.MAIN_HAND, "Fire1").orElseThrow(),
                        actual);
        input(owner, start);
        input(owner, local.input(start.released(), actual));
        // Original bundled animation duration0.75s gives15ticks per trigger, not just
        // repeatDelay1tick.
        for (int tick = 1; tick <= 46; tick++) {
            int phase = tick;
            h.runAtTickTime(
                    tick,
                    () -> {
                        try {
                            local.tick(actual);
                            GunEngine.tick(owner.player, gun);
                            flush(owner, local);
                            if (phase < 46)
                                h.assertTrue(
                                        ends(owner) == 0
                                                && GunActionTimeline.owns(
                                                        owner.player, gun, "primary_fire"),
                                        "No early retirement at phase" + phase);
                            else {
                                h.assertTrue(
                                        ends(owner) == 1
                                                && !GunActionTimeline.owns(
                                                        owner.player, gun, "primary_fire"),
                                        "Natural owner FINISH retires at phase46");
                                h.assertTrue(
                                        GunAmmo.count(gun) == 9
                                                && local.rounds() == 9
                                                && shots(owner).size() == 3,
                                        "Animation lifetime cannot generate additional shots");
                                owner.close();
                                h.succeed();
                            }
                        } catch (ReflectiveOperationException ex) {
                            owner.close();
                            throw new IllegalStateException(ex);
                        } catch (RuntimeException ex) {
                            owner.close();
                            throw ex;
                        }
                    });
        }
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void duplicateTriggerAndFinalPacketsCannotReplayAmmunition(GameTestHelper h)
            throws ReflectiveOperationException {
        try (var owner = new GunAudioGameTests.Listener(h)) {
            var gun = prepare(owner, "mat49", 4);
            var start = press(owner, gun);
            input(owner, start);
            input(owner, start.released().authored(1, 1, 0));
            var trigger =
                    new CGunAuthored(
                            start.gunId(),
                            start.prediction().activation(),
                            2,
                            1,
                            CGunAuthored.Kind.TRIGGER);
            authored(owner, trigger);
            authored(owner, trigger);
            var finish =
                    new CGunAuthored(
                            start.gunId(),
                            start.prediction().activation(),
                            2,
                            2,
                            CGunAuthored.Kind.FINISH);
            authored(owner, finish);
            authored(owner, finish);
            authored(owner, trigger);
            h.assertTrue(
                    GunAmmo.count(gun) == 2 && shots(owner).size() == 2 && ends(owner) == 1,
                    "Repeated delivery cannot consume or retire twice");
        }
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void authoredPacketsResolveOnlyAuthenticatedOwnersInventoryAndActivation(
            GameTestHelper h) throws ReflectiveOperationException {
        try (var owner = new GunAudioGameTests.Listener(h);
                var other = new GunAudioGameTests.Listener(h)) {
            var gun = prepare(owner, "mat49", 4);
            var otherGun = prepare(other, "mat49", 4);
            var start = press(owner, gun);
            input(owner, start);
            var next =
                    new CGunAuthored(
                            start.gunId(),
                            start.prediction().activation(),
                            2,
                            1,
                            CGunAuthored.Kind.TRIGGER);
            authored(other, next);
            authored(
                    owner,
                    new CGunAuthored(
                            start.gunId(),
                            java.util.UUID.randomUUID(),
                            2,
                            1,
                            CGunAuthored.Kind.TRIGGER));
            h.assertTrue(
                    GunAmmo.count(gun) == 3 && GunAmmo.count(otherGun) == 4,
                    "Neither wrong owner nor wrong activation fires a gun");
            authored(owner, next);
            h.assertTrue(
                    GunAmmo.count(gun) == 2, "Authenticated matching activation remains usable");
            GunFireScheduler.cancel(owner.player, gun);
        }
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void ordinalGapsAndImpossibleFuturePhasesRetireWithoutDamage(GameTestHelper h)
            throws ReflectiveOperationException {
        for (boolean future : List.of(false, true))
            try (var owner = new GunAudioGameTests.Listener(h)) {
                var gun = prepare(owner, "mat49", 4);
                var start = press(owner, gun);
                input(owner, start);
                authored(
                        owner,
                        new CGunAuthored(
                                start.gunId(),
                                start.prediction().activation(),
                                future ? 100_000 : 2,
                                future ? 1 : 8,
                                CGunAuthored.Kind.TRIGGER));
                authored(
                        owner,
                        new CGunAuthored(
                                start.gunId(),
                                start.prediction().activation(),
                                2,
                                1,
                                CGunAuthored.Kind.TRIGGER));
                h.assertTrue(
                        GunAmmo.count(gun) == 3 && shots(owner).size() == 1 && ends(owner) == 1,
                        "Invalid phase/count retires before further server gameplay");
            }
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void burstCannotAddFourthCallbackAndCancelAllowsFreshActivation(GameTestHelper h)
            throws ReflectiveOperationException {
        try (var owner = new GunAudioGameTests.Listener(h)) {
            var gun = prepare(owner, "g11k2", 12);
            var start = press(owner, gun);
            input(owner, start);
            authored(
                    owner,
                    new CGunAuthored(
                            start.gunId(),
                            start.prediction().activation(),
                            1,
                            1,
                            CGunAuthored.Kind.TRIGGER));
            authored(
                    owner,
                    new CGunAuthored(
                            start.gunId(),
                            start.prediction().activation(),
                            2,
                            2,
                            CGunAuthored.Kind.TRIGGER));
            authored(
                    owner,
                    new CGunAuthored(
                            start.gunId(),
                            start.prediction().activation(),
                            3,
                            3,
                            CGunAuthored.Kind.TRIGGER));
            h.assertTrue(
                    GunAmmo.count(gun) == 9 && ends(owner) == 1,
                    "Burst boundary rejects extra gameplay");
            input(owner, start.released());
            var second = press(owner, gun);
            input(owner, second);
            h.assertTrue(
                    GunAmmo.count(gun) == 8 && shots(owner).getLast().triggerIndex() == 0,
                    "Canceled remote gate cannot poison a new activation");
            GunFireScheduler.cancel(owner.player, gun);
        }
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void authoredPayloadCodecsPreservePhasesAndRejectHiddenNegativeMetadata(
            GameTestHelper h) {
        var buf =
                new net.minecraft.network.RegistryFriendlyByteBuf(
                        io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            var owner = java.util.UUID.randomUUID();
            var activation = java.util.UUID.randomUUID();
            for (var kind : CGunAuthored.Kind.values()) {
                var packet = new CGunAuthored(owner, activation, 17, 65, kind);
                CGunAuthored.STREAM_CODEC.encode(buf, packet);
                h.assertTrue(
                        CGunAuthored.STREAM_CODEC.decode(buf).equals(packet),
                        "Authored callback codec preserves " + kind);
                buf.clear();
            }
            for (int[] bad : new int[][] {{-1, 0, 0}, {0, -1, 0}, {0, 0, -1}, {0, 0, 65}}) {
                buf.writeBoolean(true);
                buf.writeVarInt(bad[0]);
                buf.writeVarLong(bad[1]);
                buf.writeVarInt(bad[2]);
                boolean rejected = false;
                try {
                    net.pttheta.loveandwar.gun.network.GunAuthoredInput.read(buf);
                } catch (io.netty.handler.codec.DecoderException expected) {
                    rejected = true;
                }
                h.assertTrue(rejected, "Invalid metadata cannot disguise itself as a legacy input");
                buf.clear();
            }
        } finally {
            buf.release();
        }
        h.succeed();
    }

    private static IPayloadContext context(GunAudioGameTests.Listener owner) {
        return (IPayloadContext)
                Proxy.newProxyInstance(
                        IPayloadContext.class.getClassLoader(),
                        new Class[] {IPayloadContext.class},
                        (p, m, args) -> {
                            if (m.getName().equals("player")) return owner.player;
                            if (m.getName().equals("enqueueWork")) {
                                ((Runnable) args[0]).run();
                                return CompletableFuture.completedFuture(null);
                            }
                            throw new UnsupportedOperationException(m.getName());
                        });
    }

    private static void input(GunAudioGameTests.Listener owner, CGunInput packet)
            throws ReflectiveOperationException {
        var handler =
                GunNetworking.class.getDeclaredMethod(
                        "handle", CGunInput.class, IPayloadContext.class);
        handler.setAccessible(true);
        handler.invoke(null, packet, context(owner));
    }

    private static void flush(GunAudioGameTests.Listener owner, LocalGunActions local)
            throws ReflectiveOperationException {
        // Same baseline fixture runs before an authored-trigger transport exists.
        java.lang.reflect.Method drain;
        try {
            drain = LocalGunActions.class.getMethod("drainAuthored");
        } catch (NoSuchMethodException absent) {
            return;
        }
        for (var packet : (List<?>) drain.invoke(local)) {
            var handler =
                    GunNetworking.class.getDeclaredMethod(
                            "handleAuthored", packet.getClass(), IPayloadContext.class);
            handler.setAccessible(true);
            handler.invoke(null, packet, context(owner));
        }
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void onePollMat49ReleaseKeepsLocalAndAuthoritativeShotsAligned(GameTestHelper h)
            throws ReflectiveOperationException {
        var owner = new GunAudioGameTests.Listener(h);
        var gun =
                new ItemStack(
                        BuiltInRegistries.ITEM.get(
                                ResourceLocation.parse("createloveandwar:mat49")));
        var round =
                new ItemStack(
                        BuiltInRegistries.ITEM.get(
                                ResourceLocation.parse("createloveandwar:pistol_bullet")));
        gun.set(
                ModDataComponents.GUN_LOADED_ROUNDS,
                new AmmoPouchContents(List.of(round, round.copy(), round.copy(), round.copy())));
        owner.player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        owner.player.setXRot(-90);
        GunIdentity.inventoryTick(owner.player);
        var actual = gun.copy();
        var player =
                new Player(
                        h.getLevel(),
                        h.absolutePos(new BlockPos(2, 3, 2)),
                        0,
                        owner.player.getGameProfile()) {
                    @Override
                    public boolean isSpectator() {
                        return false;
                    }

                    @Override
                    public boolean isCreative() {
                        return false;
                    }
                };
        player.setPos(owner.player.position());
        player.setItemInHand(InteractionHand.MAIN_HAND, actual);
        var indices = new ArrayList<Long>();
        var local =
                new LocalGunActions(player, actual, (g, key, x, y, index) -> indices.add(index));
        var press =
                local.input(
                        CGunInput.capture(player, InteractionHand.MAIN_HAND, "Fire1").orElseThrow(),
                        actual);
        input(owner, press);
        flush(owner, local); // END0 pressed/held.
        h.runAtTickTime(
                1,
                () -> {
                    try {
                        local.tick(actual);
                        GunEngine.tick(owner.player, gun);
                        flush(owner, local); // START1.
                        var release = local.input(press.released(), actual);
                        input(owner, release);
                        flush(owner, local); // END1 released.
                    } catch (ReflectiveOperationException ex) {
                        owner.close();
                        throw new IllegalStateException(ex);
                    }
                });
        h.runAtTickTime(
                2,
                () -> {
                    try {
                        local.tick(actual);
                        GunEngine.tick(owner.player, gun);
                        flush(owner, local); // START2 final callback, then retirement.
                        h.assertTrue(
                                indices.equals(List.of(0L, 1L)),
                                "Source phase trace must contain initial0 and final1: " + indices);
                        h.assertTrue(
                                GunAmmo.count(gun) == 2 && local.rounds() == 2,
                                "One-poll release must keep two local callbacks and two"
                                        + " authoritative rounds aligned; server="
                                        + GunAmmo.count(gun)
                                        + " local="
                                        + local.rounds());
                        h.succeed();
                    } catch (ReflectiveOperationException ex) {
                        throw new IllegalStateException(ex);
                    } finally {
                        owner.close();
                    }
                });
    }
}
