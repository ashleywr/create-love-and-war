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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.pttheta.loveandwar.gun.content.GunContent;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.gun.network.CGunInput;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class LocalGunPredictionGameTests {
    private record Shot(float horizontal, float vertical, long index) {}

    private record Fixture(
            Player player, ItemStack gun, LocalGunActions runtime, List<Shot> shots) {
        CGunInput press() {
            return runtime.input(
                    CGunInput.capture(player, InteractionHand.MAIN_HAND, "Fire1").orElseThrow(),
                    gun);
        }

        void release(CGunInput input) {
            runtime.input(input.released(), gun);
        }

        void ticks(int count) {
            for (int n = 0; n < count; n++) runtime.tick(gun);
        }
    }

    private static ItemStack item(String key) {
        return new ItemStack(
                BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:" + key)));
    }

    private static Fixture fixture(GameTestHelper h, String key, int count) {
        var player =
                new Player(
                        h.getLevel(),
                        h.absolutePos(new BlockPos(2, 3, 2)),
                        0,
                        new GameProfile(UUID.randomUUID(), "LocalPredict")) {
                    @Override
                    public boolean isSpectator() {
                        return false;
                    }

                    @Override
                    public boolean isCreative() {
                        return false;
                    }
                };
        player.setPos(h.absolutePos(new BlockPos(2, 3, 2)).getCenter());
        var gun = item(key);
        gun.set(ModDataComponents.GUN_ID, UUID.randomUUID());
        String round = key.equals("g11k2") ? "caseless_bullet" : "pistol_bullet";
        gun.set(
                ModDataComponents.GUN_LOADED_ROUNDS,
                new AmmoPouchContents(
                        java.util.stream.IntStream.range(0, count)
                                .mapToObj(n -> item(round))
                                .toList()));
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        var shots = new ArrayList<Shot>();
        return new Fixture(
                player,
                gun,
                new LocalGunActions(
                        player, gun, (g, group, x, y, index) -> shots.add(new Shot(x, y, index))),
                shots);
    }

    private static void definition(
            String actions, String mode, float delay, int repeat, Runnable test) {
        var json =
                GunDef.CODEC
                        .encodeStart(JsonOps.INSTANCE, GunContent.gun("mars").orElseThrow())
                        .getOrThrow()
                        .getAsJsonObject();
        json.add(
                "inputHandlers",
                JsonParser.parseString(
                        "[{\"inputType\":\"Fire1\",\"nodes\":[{\"actionGroupToTrigger\":\"primary_fire\"}]}]"));
        json.add(
                "actionGroups",
                JsonParser.parseString(
                        "[{\"key\":\"primary_fire\",\"repeatMode\":\""
                                + mode
                                + "\",\"repeatDelay\":"
                                + delay
                                + ",\"repeatCount\":"
                                + repeat
                                + ",\"actions\":"
                                + actions
                                + ",\"modifiers\":[{\"stat\":\"vertical_recoil\",\"accumulators\":[{\"operation\":\"BaseAdd\",\"value\":15}]}]}]"));
        var old =
                GunContent.guns()
                        .put("mars", GunDef.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        try {
            test.run();
        } finally {
            GunContent.guns().put("mars", old);
        }
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void localG11BurstRunsBeforeAnyServerReplyAndReservesRounds(GameTestHelper h) {
        var f = fixture(h, "g11k2", 12);
        var input = f.press();
        h.assertTrue(
                input.prediction().present()
                        && f.shots.size() == 1
                        && f.shots.getFirst().index() == 0,
                "Initial local callback and claim exist before network transport");
        f.release(input);
        f.ticks(4);
        h.assertTrue(
                f.shots.stream().map(Shot::index).toList().equals(List.of(0L, 1L, 2L)),
                "G11 keeps released burst source callback order without replies");
        h.assertTrue(
                f.runtime.rounds() == 9 && GunAmmo.count(f.gun) == 12,
                "Sidecar consumes3 without mutating authoritative12round inventory");
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void staleAuthoritativeOneRoundCannotFuelRepeatedLocalShots(GameTestHelper h) {
        var f = fixture(h, "g11k2", 1);
        f.press();
        f.ticks(12);
        h.assertTrue(
                f.shots.size() == 1 && f.runtime.rounds() == 0 && GunAmmo.count(f.gun) == 1,
                "Unacknowledged last round remains reserved through repeat checks");
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void subTickMultipleShootActionsIncludeDistinctEmptyCallbacks(GameTestHelper h) {
        definition(
                "[{\"actionType\":\"Shoot\"},{\"actionType\":\"Shoot\"}]",
                "SemiAuto",
                .025F,
                0,
                () -> {
                    var f = fixture(h, "mars", 1);
                    f.press();
                    h.assertTrue(
                            f.shots.equals(
                                    List.of(
                                            new Shot(0, 15, 0),
                                            new Shot(0, 0, 0),
                                            new Shot(0, 0, 1),
                                            new Shot(0, 0, 1))),
                            "Two sub-tick triggers execute both Shoot entries; later empty"
                                    + " collections still callback once");
                    h.assertTrue(
                            f.runtime.rounds() == 0 && GunAmmo.count(f.gun) == 1,
                            "Cannot consume below zero or change server inventory");
                });
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void localDelayedRetriggerOverwritesAndUsesCurrentTriggerCount(GameTestHelper h) {
        definition(
                "[{\"actionType\":\"Shoot\",\"delay\":2.9,\"duration\":1}]",
                "SemiAuto",
                0,
                0,
                () -> {
                    var f = fixture(h, "mars", 3);
                    var first = f.press();
                    f.release(first);
                    f.ticks(1);
                    var second = f.press();
                    f.release(second);
                    h.assertTrue(
                            first.prediction()
                                    .activation()
                                    .equals(second.prediction().activation()),
                            "Still-active plain-key alias keeps local activation");
                    f.ticks(1);
                    h.assertTrue(f.shots.isEmpty(), "Retrigger overwrites pending slot");
                    f.ticks(1);
                    h.assertTrue(
                            f.shots.equals(List.of(new Shot(0, 15, 2))) && f.runtime.rounds() == 2,
                            "One due callback receives current count2");
                });
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void fullAutoReleaseKeepsOriginalFinalStartCallback(GameTestHelper h) {
        definition(
                "[{\"actionType\":\"Shoot\",\"duration\":0.05}]",
                "FullAuto",
                .05F,
                0,
                () -> {
                    var f = fixture(h, "mars", 3);
                    var input = f.press();
                    f.release(input);
                    f.ticks(4);
                    h.assertTrue(
                            f.shots.stream().map(Shot::index).toList().equals(List.of(0L, 1L)),
                            "Source performs final eligible START callback before Finished"
                                    + " removal");
                });
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void acknowledgementsDoNotRefillReservationsOrRollBackReloadedAmmo(
            GameTestHelper h) {
        var gun = fixture(h, "g11k2", 12).gun;
        var ammo = new PredictedGunAmmo();
        ammo.observe(gun);
        var id = UUID.randomUUID();
        ammo.begin(id);
        for (int i = 0; i < 3; i++) ammo.consume(id);
        ammo.acknowledge(id, 1, 1, 11);
        h.assertTrue(
                ammo.rounds().items().size() == 9,
                "First reply acknowledges only one of three reservations");
        ammo.acknowledge(id, 2, 2, 10);
        ammo.acknowledge(id, 1, 1, 11);
        ammo.observe(gun);
        h.assertTrue(
                ammo.rounds().items().size() == 9,
                "Older receipt and stale actual item cannot refill shadow rounds");
        ammo.localFinished(id);
        ammo.end(id, 3, 3, 9);
        gun.set(ModDataComponents.GUN_AMMO_REVISION, 4L);
        ammo.observe(gun);
        ammo.acknowledge(id, 2, 2, 10);
        h.assertTrue(
                ammo.rounds().items().size() == 12,
                "New reloaded item identity is authoritative, old callbacks cannot roll it back");
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void denialBeforeDelayedCallbackCannotLeakAmmoReservation(GameTestHelper h) {
        var gun = fixture(h, "g11k2", 3).gun;
        var ammo = new PredictedGunAmmo();
        ammo.observe(gun);
        var id = UUID.randomUUID();
        ammo.begin(id);
        ammo.end(id, 0, 0, 3);
        ammo.consume(id);
        h.assertTrue(
                ammo.rounds().items().size() == 2,
                "Already-owned delayed visual can still reserve while local group runs");
        ammo.localFinished(id);
        h.assertTrue(
                ammo.rounds().items().size() == 3,
                "End-before-first-callback reservation retires when local group finishes");
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void independentSameItemGunsKeepTheirOwnLocalBurstAndAmmo(GameTestHelper h) {
        var a = fixture(h, "g11k2", 3);
        var b = a.gun.copy();
        b.set(ModDataComponents.GUN_ID, UUID.randomUUID());
        a.player.getInventory().setItem(1, b);
        var shots = new ArrayList<Shot>();
        var second =
                new LocalGunActions(
                        a.player, b, (gun, group, x, y, index) -> shots.add(new Shot(x, y, index)));
        var first = a.press();
        a.release(first);
        a.player.getInventory().selected = 1;
        var other =
                second.input(
                        CGunInput.capture(a.player, InteractionHand.MAIN_HAND, "Fire1")
                                .orElseThrow(),
                        b);
        second.input(other.released(), b);
        for (int i = 0; i < 4; i++) {
            a.runtime.tick(a.gun);
            second.tick(b);
        }
        h.assertTrue(
                a.shots.size() == 3
                        && shots.size() == 3
                        && a.runtime.rounds() == 0
                        && second.rounds() == 0,
                "Same item type is not shared local action state");
        h.assertTrue(
                !first.prediction().activation().equals(other.prediction().activation())
                        && GunAmmo.count(a.gun) == 3
                        && GunAmmo.count(b) == 3,
                "Independent activation references and unchanged actual inventory");
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void interleavedActivationAcknowledgementsPreserveOtherReservations(
            GameTestHelper h) {
        var gun = fixture(h, "g11k2", 8).gun;
        var ammo = new PredictedGunAmmo();
        ammo.observe(gun);
        var a = UUID.randomUUID();
        var b = UUID.randomUUID();
        ammo.begin(a);
        ammo.begin(b);
        ammo.consume(a);
        ammo.consume(a);
        ammo.consume(b);
        ammo.acknowledge(b, 1, 1, 7);
        h.assertTrue(
                ammo.rounds().items().size() == 5, "B's reply cannot confirm A's two reservations");
        ammo.acknowledge(a, 1, 2, 6);
        ammo.acknowledge(a, 2, 3, 5);
        ammo.acknowledge(b, 1, 1, 7);
        h.assertTrue(
                ammo.rounds().items().size() == 5,
                "Late unrelated activation reply does not rewind current authoritative ammo");
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void everyBundledGunPredictsWithItsOwnCompatibleAmmunition(GameTestHelper h) {
        int checked = 0;
        for (var entry : GunContent.guns().entrySet()) {
            var magazine =
                    GunLookup.resolveMagazineForSlot(entry.getValue().magazines().getFirst())
                            .orElseThrow();
            var round =
                    GunContent.bullets().keySet().stream()
                            .map(LocalGunPredictionGameTests::item)
                            .filter(
                                    stack ->
                                            GunLookup.matchesBullet(
                                                    magazine.matchingBullets(), stack))
                            .findFirst()
                            .orElseThrow();
            int count = Math.max(1, Math.min(3, magazine.numRounds()));
            var f = fixture(h, entry.getKey(), count);
            f.gun.set(
                    ModDataComponents.GUN_LOADED_ROUNDS,
                    new AmmoPouchContents(
                            java.util.stream.IntStream.range(0, count)
                                    .mapToObj(n -> round.copy())
                                    .toList()));
            var input = f.press();
            h.assertTrue(
                    input.prediction().present()
                            && !f.shots.isEmpty()
                            && f.shots.getFirst().vertical() > 0,
                    "Bundled loaded gun predicts its own initial callback: " + entry.getKey());
            h.assertTrue(
                    GunAmmo.count(f.gun) == count,
                    "Catalog prediction never consumes authoritative inventory: " + entry.getKey());
            checked++;
        }
        h.assertTrue(checked == 49, "All49 bundled guns were exercised, got" + checked);
        h.succeed();
    }
}
