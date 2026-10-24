package net.pttheta.loveandwar.gun.runtime;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.pttheta.loveandwar.gun.content.GunContent;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;

import java.util.List;
import java.util.UUID;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class ConcurrentFiringGameTests {
    private record Fixture(FakePlayer player, ItemStack gun, GunDef definition)
            implements AutoCloseable {
        void press() {
            var original = GunContent.guns().put("mars", definition);
            try {
                GunEngine.handleInput(player, gun, InteractionHand.MAIN_HAND, "Fire1", true);
            } finally {
                GunContent.guns().put("mars", original);
            }
        }

        List<String> keys() {
            return GunActiveGroups.of(gun).entries().stream()
                    .map(GunActiveGroups.Entry::key)
                    .toList();
        }

        long triggers(String key) {
            return GunActionTimeline.context(player, gun, key).orElseThrow().triggers();
        }

        @Override
        public void close() {
            GunFireScheduler.cancel(player, gun);
            GunActionTimeline.cancelAll(player, gun);
        }
    }

    private static ItemStack item(String key) {
        return new ItemStack(
                BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:" + key)));
    }

    private static Fixture fixture(GameTestHelper h, float delay) {
        return fixture(h, delay, "FullAuto", 0);
    }

    private static Fixture fixture(GameTestHelper h, float delay, String mode, int actionDelay) {
        return fixture(h, delay, mode, actionDelay, null);
    }

    private static Fixture fixture(
            GameTestHelper h, float delay, String mode, int actionDelay, FakePlayer owner) {
        var json =
                GunDef.CODEC
                        .encodeStart(JsonOps.INSTANCE, GunContent.gun("mars").orElseThrow())
                        .getOrThrow()
                        .getAsJsonObject();
        json.add(
                "inputHandlers",
                JsonParser.parseString(
                        """
                        [{"inputType":"Fire1","nodes":[
                          {"actionGroupToTrigger":"first","andContinueEvaluating":true},
                          {"actionGroupToTrigger":"second"}]}]
                        """));
        var groups = new com.google.gson.JsonArray();
        for (var key : List.of("first", "second"))
            groups.add(
                    JsonParser.parseString(
                            "{\"key\":\""
                                    + key
                                    + "\",\"repeatMode\":\""
                                    + mode
                                    + "\",\"repeatCount\":3,\"repeatDelay\":"
                                    + delay
                                    + ",\"actions\":[{\"actionType\":\"Shoot\",\"duration\":0.4,\"delay\":"
                                    + actionDelay
                                    + "}]}"));
        json.add("actionGroups", groups);
        var gun = item("mars");
        gun.set(
                ModDataComponents.GUN_LOADED_ROUNDS,
                new AmmoPouchContents(
                        java.util.stream.IntStream.range(0, 12)
                                .mapToObj(i -> item("pistol_bullet"))
                                .toList()));
        var p =
                owner == null
                        ? new FakePlayer(
                                h.getLevel(), new GameProfile(UUID.randomUUID(), "MultiFire"))
                        : owner;
        p.setPos(h.absolutePos(new BlockPos(2, 6, 2)).getCenter());
        p.setItemInHand(InteractionHand.MAIN_HAND, gun);
        return new Fixture(p, gun, GunDef.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void positiveDelayStartsTwoGroupsWithSharedZeroTriggerSecond(GameTestHelper h) {
        try (var f = fixture(h, .1F)) {
            f.press();
            h.assertTrue(
                    f.keys().equals(List.of("first", "second")),
                    "Both accepted groups own independent instances in activation order");
            h.assertTrue(
                    GunFireScheduler.owns(f.player, f.gun, "first")
                            && GunFireScheduler.owns(f.player, f.gun, "second"),
                    "Both groups retain a scheduler");
            h.assertTrue(
                    f.triggers("first") == 1
                            && f.triggers("second") == 0
                            && GunAmmo.count(f.gun) == 11,
                    "Second group starts with zero callbacks after first spends shared"
                            + " positive-delay cadence");
        }
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void nonpositiveInitialDelaysBypassSharedCooldownIndependently(GameTestHelper h) {
        try (var f = fixture(h, 0)) {
            f.press();
            h.assertTrue(
                    f.keys().equals(List.of("first", "second")) && GunAmmo.count(f.gun) == 10,
                    "Source DoInitialTrigger invokes each zero-delay group exactly once");
        }
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void reverseActivationTickOrderSpendsOneSharedCooldown(GameTestHelper h) {
        var f = fixture(h, .1F);
        try {
            f.press();
            h.assertTrue(f.keys().equals(List.of("first", "second")), "Fixture starts two groups");
            for (int tick = 1; tick <= 2; tick++) {
                int phase = tick;
                h.runAtTickTime(
                        tick,
                        () -> {
                            try {
                                GunEngine.tick(f.player, f.gun);
                                h.assertTrue(
                                        f.triggers("first") == 1
                                                && f.triggers("second") == (phase == 2 ? 1 : 0),
                                        "Later activation gets first access to shared cadence at"
                                                + " phase"
                                                + phase);
                                h.assertTrue(
                                        GunAmmo.count(f.gun) == (phase == 2 ? 10 : 11),
                                        "Only shared-budget callbacks consume rounds");
                                if (phase == 2) h.succeed();
                            } finally {
                                if (phase == 2) f.close();
                            }
                        });
            }
        } catch (RuntimeException | Error failure) {
            f.close();
            throw failure;
        }
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void releaseRetainsEachFinalPendingCallbackWithoutRestartingGroups(
            GameTestHelper h) {
        var f = fixture(h, 0, "FullAuto", 1);
        f.press();
        h.assertTrue(GunAmmo.count(f.gun) == 12, "Both initial callbacks wait for delay1");
        GunEngine.handleInput(f.player, f.gun, InteractionHand.MAIN_HAND, "Fire1", false);
        h.assertTrue(
                !GunFireScheduler.owns(f.player, f.gun, "first")
                        && !GunFireScheduler.owns(f.player, f.gun, "second"),
                "Release ends both unclaimed controllers");
        h.runAtTickTime(
                1,
                () -> {
                    try {
                        GunEngine.tick(f.player, f.gun);
                        h.assertTrue(
                                GunAmmo.count(f.gun) == 10 && f.keys().isEmpty(),
                                "Each finishing instance still receives its pending tick, then"
                                        + " retires");
                        h.succeed();
                    } finally {
                        f.close();
                    }
                });
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void finishingOneGroupPreservesItsSiblingAndSharedBudget(GameTestHelper h) {
        var f = fixture(h, .1F);
        f.press();
        GunFireScheduler.finishModeGroup(f.player, f.gun, "first");
        GunActionTimeline.finishAfterTick(f.player, f.gun, "first");
        for (int tick = 1; tick <= 2; tick++) {
            int phase = tick;
            h.runAtTickTime(
                    tick,
                    () -> {
                        try {
                            GunEngine.tick(f.player, f.gun);
                            h.assertTrue(
                                    f.keys().equals(List.of("second"))
                                            && GunFireScheduler.owns(f.player, f.gun, "second")
                                            && !GunFireScheduler.owns(f.player, f.gun, "first"),
                                    "Finishing first cannot clear the live sibling controller");
                            h.assertTrue(
                                    GunAmmo.count(f.gun) == (phase == 2 ? 10 : 11),
                                    "Sibling retains shared source cooldown");
                            if (phase == 2) h.succeed();
                        } finally {
                            if (phase == 2) f.close();
                        }
                    });
        }
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void sharedCadenceDoesNotExtendEitherBurstAttemptBudget(GameTestHelper h) {
        var f = fixture(h, .1F, "BurstFire", 0);
        f.press();
        for (int tick = 1; tick <= 4; tick++) {
            int phase = tick;
            h.runAtTickTime(
                    tick,
                    () -> {
                        try {
                            GunEngine.tick(f.player, f.gun);
                            h.assertTrue(
                                    f.triggers("first") == (phase >= 2 ? 2 : 1)
                                            && f.keys().equals(List.of("first")),
                                    "Zero-trigger burst finishes at phase1; surviving burst has"
                                        + " only two retrigger attempts");
                            h.assertTrue(
                                    GunAmmo.count(f.gun) == (phase >= 2 ? 10 : 11),
                                    "Cadence starvation does not add retry attempts");
                            if (phase == 4) h.succeed();
                        } finally {
                            if (phase == 4) f.close();
                        }
                    });
        }
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void repeatedPlainGroupRetainsItsInstanceWithoutReplacingSibling(
            GameTestHelper h) {
        try (var f = fixture(h, .1F)) {
            f.press();
            var before = GunActiveGroups.of(f.gun);
            GunFireScheduler.start(
                    f.player,
                    f.gun,
                    f.definition,
                    f.definition.actionGroups().getFirst(),
                    GunAttachments.EMPTY);
            h.assertTrue(
                    GunActiveGroups.of(f.gun).equals(before)
                            && GunFireScheduler.owns(f.player, f.gun, "first")
                            && GunFireScheduler.owns(f.player, f.gun, "second")
                            && GunAmmo.count(f.gun) == 11,
                    "Same-key request retains both identities and cannot reset shared cooldown");
        }
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void laterStartChecksObserveTheFirstGroupsAmmoConsumption(GameTestHelper h) {
        try (var f = fixture(h, 0)) {
            f.gun.set(
                    ModDataComponents.GUN_LOADED_ROUNDS,
                    new AmmoPouchContents(List.of(item("pistol_bullet"))));
            f.press();
            h.assertTrue(
                    f.keys().equals(List.of("first"))
                            && GunAmmo.count(f.gun) == 0
                            && !GunFireScheduler.owns(f.player, f.gun, "second"),
                    "Candidate selection snapshots modes; subsequent CanStart sees current ammo");
        }
        h.succeed();
    }

    private static List<net.pttheta.loveandwar.gun.network.SGunRecoil> shots(
            GunAudioGameTests.Listener listener) {
        return listener.packets.stream()
                .filter(
                        net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket.class
                                ::isInstance)
                .map(
                        net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket.class
                                ::cast)
                .map(net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket::payload)
                .filter(net.pttheta.loveandwar.gun.network.SGunRecoil.class::isInstance)
                .map(net.pttheta.loveandwar.gun.network.SGunRecoil.class::cast)
                .toList();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void delayedCallbacksCompeteForLastRoundInReverseActivationOrder(GameTestHelper h)
            throws ReflectiveOperationException {
        var listener = new GunAudioGameTests.Listener(h);
        var f = fixture(h, 0, "FullAuto", 1, listener.player);
        f.gun.set(ModDataComponents.GUN_ID, UUID.randomUUID());
        f.gun.set(
                ModDataComponents.GUN_LOADED_ROUNDS,
                new AmmoPouchContents(List.of(item("pistol_bullet"))));
        try {
            f.press();
            var ids =
                    GunActiveGroups.of(f.gun).entries().stream()
                            .map(GunActiveGroups.Entry::activation)
                            .toList();
            h.assertTrue(
                    ids.size() == 2, "Delayed starts can both pass before either consumes ammo");
            h.runAtTickTime(
                    1,
                    () -> {
                        try {
                            GunEngine.tick(f.player, f.gun);
                            var rows = shots(listener);
                            h.assertTrue(
                                    rows.stream()
                                                    .map(row -> row.receipt().activation())
                                                    .toList()
                                                    .equals(List.of(ids.get(1), ids.get(0)))
                                            && rows.stream()
                                                    .map(row -> row.receipt().consumed())
                                                    .toList()
                                                    .equals(List.of(1, 0)),
                                    "Later group's pending callback consumes last round before"
                                        + " earlier group's empty callback");
                            h.succeed();
                        } finally {
                            f.close();
                            listener.close();
                        }
                    });
        } catch (RuntimeException | Error failure) {
            f.close();
            listener.close();
            throw failure;
        }
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void laterGroupsRepeatPrecedesEarlierGroupsDelayedCallback(GameTestHelper h)
            throws ReflectiveOperationException {
        var listener = new GunAudioGameTests.Listener(h);
        var base = fixture(h, .05F, "FullAuto", 1, listener.player);
        var json =
                GunDef.CODEC
                        .encodeStart(JsonOps.INSTANCE, base.definition)
                        .getOrThrow()
                        .getAsJsonObject();
        var second = json.getAsJsonArray("actionGroups").get(1).getAsJsonObject();
        second.addProperty("repeatDelay", 0);
        second.getAsJsonArray("actions").get(0).getAsJsonObject().addProperty("delay", 0);
        var f =
                new Fixture(
                        base.player,
                        base.gun,
                        GunDef.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        f.gun.set(ModDataComponents.GUN_ID, UUID.randomUUID());
        f.gun.set(
                ModDataComponents.GUN_LOADED_ROUNDS,
                new AmmoPouchContents(List.of(item("pistol_bullet"), item("pistol_bullet"))));
        try {
            f.press();
            var ids =
                    GunActiveGroups.of(f.gun).entries().stream()
                            .map(GunActiveGroups.Entry::activation)
                            .toList();
            h.assertTrue(
                    ids.size() == 2 && GunAmmo.count(f.gun) == 1,
                    "Only later group's immediate callback consumes a round initially");
            listener.packets.clear();
            h.runAtTickTime(
                    1,
                    () -> {
                        try {
                            GunEngine.tick(f.player, f.gun);
                            var rows = shots(listener);
                            h.assertTrue(
                                    rows.stream()
                                                    .map(row -> row.receipt().activation())
                                                    .toList()
                                                    .equals(List.of(ids.get(1), ids.get(0)))
                                            && rows.stream()
                                                    .map(row -> row.receipt().consumed())
                                                    .toList()
                                                    .equals(List.of(2, 0)),
                                    "Complete later-group tick (including repeat) precedes earlier"
                                        + " group's pending callback");
                            h.succeed();
                        } finally {
                            f.close();
                            listener.close();
                        }
                    });
        } catch (RuntimeException | Error failure) {
            f.close();
            listener.close();
            throw failure;
        }
    }
}
