package net.pttheta.loveandwar.gun.runtime;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.pttheta.loveandwar.gun.content.GunContent;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.gun.content.AttachmentDef;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class ActionRestrictionGameTests {
    private static ItemStack stack(String name) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:" + name));
        if (item == Items.AIR) throw new IllegalArgumentException("Missing fixture item: " + name);
        return new ItemStack(item);
    }
    private static FakePlayer player(GameTestHelper helper, ItemStack gun, InteractionHand hand) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "RestrictTest"));
        player.setPos(helper.absoluteVec(new Vec3(2.5, 6, 2.5)));
        player.setXRot(-90);
        player.setItemInHand(hand, gun);
        String bullet = GunLookup.contentKey(gun).equals("mars") ? "pistol_bullet" : "caseless_bullet";
        gun.set(ModDataComponents.GUN_LOADED_ROUNDS, new AmmoPouchContents(List.of(stack(bullet))));
        return player;
    }
    private static int rounds(ItemStack gun) {
        return gun.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY).items().size();
    }
    private static void press(FakePlayer player, ItemStack gun, InteractionHand hand, String input) {
        GunEngine.handleInput(player, gun, hand, input, true);
        GunEngine.handleInput(player, gun, hand, input, false);
    }
    private static void clean(FakePlayer player, ItemStack gun) {
        GunFireScheduler.releaseAll(player);
        GunFireScheduler.cancel(player, gun);
        GunActionTimeline.cancelAll(player, gun);
    }
    private static JsonObject fixture(String groups, String nodes) {
        var json = GunDef.CODEC.encodeStart(JsonOps.INSTANCE, GunContent.gun("mars").orElseThrow()).getOrThrow().getAsJsonObject();
        json.add("actionGroups", JsonParser.parseString(groups));
        json.add("inputHandlers", JsonParser.parseString("[{\"inputType\":\"SpecialKey1\",\"nodes\":" + nodes + "}]"));
        return json;
    }
    private static void withDefinition(JsonObject json, Runnable test) {
        var original = GunContent.guns().put("mars", GunDef.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        try { test.run(); } finally { GunContent.guns().put("mars", original); }
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void authoredTwoHandedGunRefusesOccupiedOffhand(GameTestHelper helper) {
        var gun = stack("aai_acr");
        var player = player(helper, gun, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK));
        try {
            helper.assertTrue(GunContent.gun("aai_acr").orElseThrow().actionGroups().getFirst().twoHanded(), "Authored fixture must require two hands");
            helper.assertTrue(GunEngine.predictStartedGroup(player, gun, "Fire1", player.level().getGameTime()).isEmpty(), "Prediction must share occupied-hand gate when no reload ammo exists");
            press(player, gun, InteractionHand.MAIN_HAND, "Fire1");
            helper.assertTrue(rounds(gun) == 1, "Occupied other hand must prevent two-handed initial shot");
        } finally { clean(player, gun); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void twoHandedGunInOffhandRequiresEmptyMainHand(GameTestHelper helper) {
        for (boolean occupied : List.of(true, false)) {
            var gun = stack("aai_acr");
            var player = player(helper, gun, InteractionHand.OFF_HAND);
            player.setItemInHand(InteractionHand.MAIN_HAND, occupied ? new ItemStack(Items.STICK) : ItemStack.EMPTY);
            try {
                press(player, gun, InteractionHand.OFF_HAND, "Fire1");
                helper.assertTrue(rounds(gun) == (occupied ? 1 : 0), "Original XOR empty-hand check applies equally to offhand gun; occupied=" + occupied);
            } finally { clean(player, gun); }
        }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void oneHandedGunAllowsAnOccupiedOtherHand(GameTestHelper helper) {
        var gun = stack("mars");
        var player = player(helper, gun, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK));
        try {
            helper.assertTrue(GunEngine.predictStartedGroup(player, gun, "Fire1", player.level().getGameTime()).map(group -> group.key().equals("primary_fire")).orElse(false),
                    "Prediction must retain allowed one-handed fire");
            press(player, gun, InteractionHand.MAIN_HAND, "Fire1");
            helper.assertTrue(rounds(gun) == 0, "One-handed authored gun must remain usable with both hands occupied");
        } finally { clean(player, gun); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void waterRestrictionUsesShootOriginRatherThanFeet(GameTestHelper helper) {
        for (boolean eyeWet : List.of(true, false)) {
            var gun = stack("aai_acr");
            var player = player(helper, gun, InteractionHand.MAIN_HAND);
            var eye = net.minecraft.core.BlockPos.containing(player.getEyePosition());
            var fluid = eyeWet ? eye : player.blockPosition();
            helper.getLevel().setBlock(fluid, Blocks.WATER.defaultBlockState(), 2);
            try {
                helper.assertTrue(helper.getLevel().isWaterAt(eye) == eyeWet, "Fixture must distinguish eye water from feet-only water");
                press(player, gun, InteractionHand.MAIN_HAND, "Fire1");
                helper.assertTrue(rounds(gun) == (eyeWet ? 1 : 0), "Water start gate must use eye block; eyeWet=" + eyeWet);
            } finally {
                helper.getLevel().setBlock(fluid, Blocks.AIR.defaultBlockState(), 2);
                clean(player, gun);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void sourceWaterAndFlowingWaterKeepOriginalOtherLiquidDistinction(GameTestHelper helper) {
        for (int level : List.of(0, 3)) {
            var gun = stack("mars");
            var player = player(helper, gun, InteractionHand.MAIN_HAND);
            var eye = net.minecraft.core.BlockPos.containing(player.getEyePosition());
            helper.getLevel().setBlock(eye, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, level), 2);
            try {
                helper.assertTrue(helper.getLevel().isWaterAt(eye), "Both source and flowing fixture must be water tagged");
                press(player, gun, InteractionHand.MAIN_HAND, "Fire1");
                helper.assertTrue(rounds(gun) == (level == 0 ? 0 : 1), "Original other-liquid gate excludes source WATER only; level=" + level);
            } finally {
                helper.getLevel().setBlock(eye, Blocks.AIR.defaultBlockState(), 2);
                clean(player, gun);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void otherLiquidGateRefusesLavaAtEye(GameTestHelper helper) {
        var gun = stack("mars");
        var player = player(helper, gun, InteractionHand.MAIN_HAND);
        var eye = net.minecraft.core.BlockPos.containing(player.getEyePosition());
        helper.getLevel().setBlock(eye, Blocks.LAVA.defaultBlockState(), 2);
        try {
            press(player, gun, InteractionHand.MAIN_HAND, "Fire1");
            helper.assertTrue(rounds(gun) == 1, "Water-allowed gun still refuses other liquids unless enabled");
            var json = GunDef.CODEC.encodeStart(JsonOps.INSTANCE, GunContent.gun("mars").orElseThrow()).getOrThrow().getAsJsonObject();
            json.getAsJsonArray("actionGroups").get(0).getAsJsonObject().addProperty("canActUnderOtherLiquid", true);
            withDefinition(json, () -> {
                press(player, gun, InteractionHand.MAIN_HAND, "Fire1");
                helper.assertTrue(rounds(gun) == 0, "Explicitly enabling other-liquid actions permits that same lava fixture");
            });
        } finally {
            helper.getLevel().setBlock(eye, Blocks.AIR.defaultBlockState(), 2);
            clean(player, gun);
        }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void refusedFirstGroupFallsThroughToAllowedGroup(GameTestHelper helper) {
        var gun = stack("mars");
        var player = player(helper, gun, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK));
        var json = fixture("[{\"key\":\"blocked\",\"twoHanded\":true,\"repeatMode\":\"Toggle\",\"actions\":[{\"actionType\":\"AimDownSights\"}]},{\"key\":\"fallback\",\"repeatMode\":\"Toggle\",\"actions\":[{\"actionType\":\"AimDownSights\"}]}]",
                "[{\"actionGroupToTrigger\":\"blocked\"},{\"actionGroupToTrigger\":\"fallback\"}]");
        try {
            withDefinition(json, () -> {
                press(player, gun, InteractionHand.MAIN_HAND, "SpecialKey1");
                var toggles = gun.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE).activeToggles();
                helper.assertTrue(!toggles.contains("blocked") && toggles.contains("fallback"), "TryNextAction must reach an allowed later node");
            });
        } finally { clean(player, gun); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void rootHandlerReloadFlagFiltersEvenToggleGroups(GameTestHelper helper) {
        for (boolean allowed : List.of(false, true)) {
            var gun = stack("mars");
            var player = player(helper, gun, InteractionHand.MAIN_HAND);
            var json = fixture("[{\"key\":\"utility\",\"repeatMode\":\"Toggle\",\"actions\":[{\"actionType\":\"AimDownSights\"}]}]",
                    "[{\"actionGroupToTrigger\":\"utility\",\"canTriggerWhileReloading\":" + allowed + "}]");
            var stage = GunContent.gun("mars").orElseThrow().reloads().getFirst().loadOneActionKey();
            gun.set(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE.withReloadGroup(stage, 20, List.of()));
            try {
                withDefinition(json, () -> {
                    press(player, gun, InteractionHand.MAIN_HAND, "SpecialKey1");
                    var toggles = gun.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE).activeToggles();
                    helper.assertTrue(toggles.contains("utility") == allowed, "Root node flag applies before group processing; allowed=" + allowed);
                });
            } finally { clean(player, gun); }
        }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void refusedAttachmentFallsBackToRootInsteadOfClaimingInput(GameTestHelper helper) {
        var gun = stack("mars");
        var player = player(helper, gun, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK));
        gun.set(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY.with("scope", List.of(stack("optical_scope"))));
        var json = fixture("[{\"key\":\"fallback\",\"repeatMode\":\"Toggle\",\"actions\":[{\"actionType\":\"AimDownSights\"}]}]",
                "[{\"deferToAttachment\":true,\"attachmentType\":\"Sights\"},{\"actionGroupToTrigger\":\"fallback\"}]");
        var attachment = AttachmentDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"handlerOverrides":[{"inputType":"SpecialKey1","nodes":[{"actionGroupToTrigger":"blocked"},{"actionGroupToTrigger":"wrong_child"}]}],
                 "actionOverrides":[{"key":"blocked","twoHanded":true,"repeatMode":"Toggle","actions":[{"actionType":"AimDownSights"}]},{"key":"wrong_child","repeatMode":"Toggle","actions":[{"actionType":"AimDownSights"}]}]}
                """)).getOrThrow();
        var original = GunContent.attachments().put("optical_scope", attachment);
        try {
            withDefinition(json, () -> {
                press(player, gun, InteractionHand.MAIN_HAND, "SpecialKey1");
                var toggles = gun.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE).activeToggles();
                helper.assertTrue(toggles.equals(List.of("fallback")), "Rejected child with andContinue=false skips later children but permits later root fallback: " + toggles);
            });
        } finally {
            GunContent.attachments().put("optical_scope", original);
            clean(player, gun);
        }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void reloadFlagAppliesBeforeAttachmentExpansionOnly(GameTestHelper helper) {
        var attachment = AttachmentDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"handlerOverrides":[{"inputType":"SpecialKey1","nodes":[{"actionGroupToTrigger":"utility","canTriggerWhileReloading":false}]}],
                 "actionOverrides":[{"key":"utility","repeatMode":"Toggle","actions":[{"actionType":"AimDownSights"}]}]}
                """)).getOrThrow();
        var original = GunContent.attachments().put("optical_scope", attachment);
        try {
            for (boolean allowed : List.of(false, true)) {
                var gun = stack("mars");
                var player = player(helper, gun, InteractionHand.MAIN_HAND);
                gun.set(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY.with("scope", List.of(stack("optical_scope"))));
                gun.set(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE.withReloadGroup("reload_primary_load_one", 20, List.of()));
                var json = fixture("[]", "[{\"deferToAttachment\":true,\"attachmentType\":\"Sights\",\"canTriggerWhileReloading\":" + allowed + "}]");
                try {
                    withDefinition(json, () -> {
                        press(player, gun, InteractionHand.MAIN_HAND, "SpecialKey1");
                        var toggles = gun.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE).activeToggles();
                        helper.assertTrue(toggles.contains("utility") == allowed, "Root flag gates attachment; child flag is not rechecked in source; rootAllowed=" + allowed);
                    });
                } finally { clean(player, gun); }
            }
        } finally { GunContent.attachments().put("optical_scope", original); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar", timeoutTicks = 65)
    public static void rejectedLoadOnePreservesTransferButDoesNotStartPhase(GameTestHelper helper) {
        var gun = stack("g11k2");
        var player = player(helper, gun, InteractionHand.MAIN_HAND);
        gun.set(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY);
        player.getInventory().items.set(8, stack("caseless_bullet").copyWithCount(12));
        press(player, gun, InteractionHand.MAIN_HAND, "Reload1");
        var eye = net.minecraft.core.BlockPos.containing(player.getEyePosition());
        for (int tick = 1; tick <= 43; tick++) {
            int current = tick;
            helper.runAtTickTime(tick, () -> {
                if (current == 41) helper.getLevel().setBlock(eye, Blocks.LAVA.defaultBlockState(), 2);
                GunEngine.tick(player, gun);
                if (current == 43) {
                    helper.getLevel().setBlock(eye, Blocks.AIR.defaultBlockState(), 2);
                    var state = gun.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
                    clean(player, gun);
                    helper.assertTrue(rounds(gun) == 12, "Original EnterReloadState still invokes FullMag LoadOne after failed CanStart");
                    helper.assertTrue(state.activeGroup().isEmpty(), "Rejected LoadOne must not run actions, lifetime or later End phase");
                    helper.succeed();
                }
            });
        }
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar", timeoutTicks = 20)
    public static void activeFullAutoDoesNotRepeatCanStartGates(GameTestHelper helper) {
        var gun = stack("aai_acr");
        var player = player(helper, gun, InteractionHand.MAIN_HAND);
        gun.set(ModDataComponents.GUN_LOADED_ROUNDS, new AmmoPouchContents(java.util.stream.IntStream.range(0, 4).mapToObj(i -> stack("caseless_bullet")).toList()));
        GunEngine.handleInput(player, gun, InteractionHand.MAIN_HAND, "Fire1", true);
        helper.assertTrue(rounds(gun) == 3, "Initial authored shot must start dry with empty other hand");
        var eye = net.minecraft.core.BlockPos.containing(player.getEyePosition());
        for (int tick = 1; tick <= 4; tick++) {
            int current = tick;
            helper.runAtTickTime(tick, () -> {
                if (current == 1) {
                    player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK));
                    helper.getLevel().setBlock(eye, Blocks.LAVA.defaultBlockState(), 2);
                }
                GunEngine.tick(player, gun);
                if (current == 4) {
                    helper.getLevel().setBlock(eye, Blocks.AIR.defaultBlockState(), 2);
                    clean(player, gun);
                    helper.assertTrue(rounds(gun) == 1, "Original active full-auto CanRetrigger checks ammo/reload, not CanStart hand/liquid gates");
                    helper.succeed();
                }
            });
        }
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void reloadPredictionRequiresAvailableCompatibleAmmunition(GameTestHelper helper) {
        var gun = stack("mars");
        var player = player(helper, gun, InteractionHand.MAIN_HAND);
        gun.set(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY);
        try {
            helper.assertTrue(GunEngine.predictStartedGroup(player, gun, "Fire1", player.level().getGameTime()).isEmpty(), "Empty gun with no spare ammo must not predict reload");
            player.getInventory().items.set(8, stack("caseless_bullet").copyWithCount(12));
            helper.assertTrue(GunEngine.predictStartedGroup(player, gun, "Fire1", player.level().getGameTime()).isEmpty(), "Incompatible spare ammo must not predict reload");
            player.getInventory().items.set(8, stack("pistol_bullet").copyWithCount(12));
            helper.assertTrue(GunEngine.predictStartedGroup(player, gun, "Fire1", player.level().getGameTime()).map(group -> group.key().equals("reload_primary_start")).orElse(false),
                    "Compatible spare ammo must allow explicit reload fallback prediction");
        } finally { clean(player, gun); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void unloadableReloadCandidateFallsThroughToLaterRootNode(GameTestHelper helper) {
        var gun = stack("mars");
        var player = player(helper, gun, InteractionHand.MAIN_HAND);
        var json = fixture("[{\"key\":\"reload_primary_start\",\"actions\":[{\"actionType\":\"AimDownSights\"}]},{\"key\":\"fallback\",\"repeatMode\":\"Toggle\",\"actions\":[{\"actionType\":\"AimDownSights\"}]}]",
                "[{\"actionGroupToTrigger\":\"reload_primary_start\"},{\"actionGroupToTrigger\":\"fallback\"}]");
        try {
            withDefinition(json, () -> {
                press(player, gun, InteractionHand.MAIN_HAND, "SpecialKey1");
                helper.assertTrue(gun.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE).activeToggles().equals(List.of("fallback")),
                        "Reload CanStart inventory rejection must permit the later root candidate");
            });
        } finally { clean(player, gun); }
        helper.succeed();
    }
}
