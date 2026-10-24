package net.pttheta.loveandwar.gun.runtime;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.pttheta.loveandwar.gun.content.GunContent;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.gun.content.AttachmentDef;
import net.pttheta.loveandwar.item.ModDataComponents;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class ModeLifecycleGameTests {
    private static ItemStack item(String name) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:" + name));
        if (item == Items.AIR) throw new IllegalArgumentException("Missing fixture item: " + name);
        return new ItemStack(item);
    }
    private static FakePlayer player(GameTestHelper helper, ItemStack gun) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ModeTest"));
        player.setPos(helper.absoluteVec(new Vec3(2.5, 6, 2.5)));
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        return player;
    }
    private static GunActionState state(ItemStack gun) { return gun.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE); }
    private static void clean(FakePlayer player, ItemStack gun) {
        GunFireScheduler.releaseAll(player); GunFireScheduler.cancel(player, gun); GunActionTimeline.cancelAll(player, gun);
    }
    private static GunDef definition(String modes, String groups, String handlers) {
        var json = GunDef.CODEC.encodeStart(JsonOps.INSTANCE, GunContent.gun("mars").orElseThrow()).getOrThrow().getAsJsonObject();
        json.add("modes", JsonParser.parseString(modes));
        json.add("actionGroups", JsonParser.parseString(groups));
        json.add("inputHandlers", JsonParser.parseString(handlers));
        return GunDef.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }
    private static void withDefinition(GunDef def, Runnable test) {
        var original = GunContent.guns().put("mars", def);
        try { test.run(); } finally { GunContent.guns().put("mars", original); }
    }
    private static void input(FakePlayer player, ItemStack gun, String name) {
        GunEngine.handleInput(player, gun, InteractionHand.MAIN_HAND, name, true);
        GunEngine.handleInput(player, gun, InteractionHand.MAIN_HAND, name, false);
    }
    private static void trigger(FakePlayer player, ItemStack gun, GunDef def, String key) {
        var group = def.actionGroups().stream().filter(g -> g.key().equals(key)).findFirst().orElseThrow();
        GunEngine.triggerActions(player, gun, def, group, gun.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY));
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void freshAuthoredSightModesAreOffUntilFirstSwitch(GameTestHelper helper) {
        for (String name : List.of("krag", "lebel_scope")) {
            var gun = item(name); var player = player(helper, gun);
            try {
                input(player, gun, "Fire2");
                helper.assertTrue(state(gun).activeToggles().isEmpty(), "Fresh " + name + " uses default off, neither sight0 nor sight1");
                var def = GunContent.gun(name).orElseThrow();
                trigger(player, gun, def, "iron_sight_toggle");
                helper.assertTrue("0".equals(state(gun).modeValues().get("iron_sight")), "First authored cycle selects0 from default off");
                input(player, gun, "Fire2");
                helper.assertTrue(state(gun).activeToggles().contains("ads_0"), "First switched sight0 must be usable");
            } finally { clean(player, gun); }
        }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void bareModalCheckRequiresOnAndDoesNotImplementNegation(GameTestHelper helper) {
        var gun = item("mars"); var player = player(helper, gun);
        var def = definition("[{\"key\":\"m\",\"values\":[\"on\",\"off\"]}]", "[{\"key\":\"utility\",\"repeatMode\":\"Toggle\",\"actions\":[{\"actionType\":\"AimDownSights\"}]}]",
                "[{\"inputType\":\"SpecialKey1\",\"nodes\":[{\"actionGroupToTrigger\":\"utility\",\"modalCheck\":\"m\"}]}]");
        try {
            withDefinition(def, () -> {
                input(player, gun, "SpecialKey1");
                helper.assertTrue(state(gun).activeToggles().isEmpty(), "Bare m requires on, default off must refuse");
                gun.set(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE.withMode("m", "on"));
                input(player, gun, "SpecialKey1");
                helper.assertTrue(state(gun).activeToggles().equals(List.of("utility")), "Bare m accepts explicit on");
                var json = GunDef.CODEC.encodeStart(JsonOps.INSTANCE, def).getOrThrow().getAsJsonObject();
                json.getAsJsonArray("inputHandlers").get(0).getAsJsonObject().getAsJsonArray("nodes").get(0).getAsJsonObject().addProperty("modalCheck", "!m");
                var negated = GunDef.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
                gun.set(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE.withMode("m", "on"));
                withDefinition(negated, () -> input(player, gun, "SpecialKey1"));
                helper.assertTrue(state(gun).activeToggles().isEmpty(), "!m is a literal unknown key, not implemented negation");
            });
        } finally { clean(player, gun); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void explicitDefaultValueIsIndependentOfCycleValues(GameTestHelper helper) {
        var gun = item("mars"); var player = player(helper, gun);
        var def = definition("[{\"key\":\"m\",\"values\":[\"cold\",\"warm\"],\"defaultValue\":\"ready\"}]",
                "[{\"key\":\"utility\",\"repeatMode\":\"Toggle\",\"actions\":[{\"actionType\":\"AimDownSights\"}]}]",
                "[{\"inputType\":\"SpecialKey1\",\"nodes\":[{\"actionGroupToTrigger\":\"utility\",\"modalCheck\":\"m:ready\"}]}]");
        try {
            withDefinition(def, () -> {
                input(player, gun, "SpecialKey1");
                helper.assertTrue(state(gun).activeToggles().equals(List.of("utility")), "Declared default ready is used even outside cycle list");
            });
        } finally { clean(player, gun); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void existingModesCompoundMissingKeyDoesNotUseDefault(GameTestHelper helper) {
        var gun = item("mars"); var player = player(helper, gun);
        var def = definition("[{\"key\":\"m\",\"values\":[\"armed\"],\"defaultValue\":\"armed\"}]",
                "[{\"key\":\"utility\",\"repeatMode\":\"Toggle\",\"actions\":[{\"actionType\":\"AimDownSights\"}]}]",
                "[{\"inputType\":\"SpecialKey1\",\"nodes\":[{\"actionGroupToTrigger\":\"utility\",\"modalCheck\":\"m:armed\"}]}]");
        gun.set(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE.withMode("other", "on"));
        try {
            withDefinition(def, () -> {
                input(player, gun, "SpecialKey1");
                helper.assertTrue(state(gun).activeToggles().isEmpty(), "After writing another mode, absent m reads empty string instead of armed default");
            });
        } finally { clean(player, gun); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void switchModeCanSelectLiteralOutsideCycleValues(GameTestHelper helper) {
        var gun = item("mars"); var player = player(helper, gun);
        var def = definition("[{\"key\":\"m\",\"values\":[\"a\",\"b\"]}]", """
                [{"key":"switch","actions":[{"actionType":"SwitchMode"}],
                  "modifiers":[{"stat":"mode","setValue":"m"},{"stat":"set_value","setValue":"unlisted"}]}]
                """, "[]");
        try {
            trigger(player, gun, def, "switch");
            helper.assertTrue("unlisted".equals(state(gun).modeValues().get("m")), "set_value selects its literal without membership validation");
        } finally { clean(player, gun); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void switchModeUsesDefaultModeKeyAndLastDuplicateIndex(GameTestHelper helper) {
        var gun = item("mars"); var player = player(helper, gun);
        var def = definition("[{\"key\":\"mode\",\"values\":[\"x\",\"wrong\",\"x\",\"next\"]}]",
                "[{\"key\":\"switch\",\"actions\":[{\"actionType\":\"SwitchMode\"}]}]", "[]");
        gun.set(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE.withMode("mode", "x"));
        try {
            trigger(player, gun, def, "switch");
            helper.assertTrue("next".equals(state(gun).modeValues().get("mode")), "No mode override means key mode; cycle uses last matching index");
        } finally { clean(player, gun); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar", timeoutTicks = 15)
    public static void switchingSightFinishesOldToggleAfterItsFinalPendingTick(GameTestHelper helper) {
        var gun = item("mars"); var player = player(helper, gun);
        var def = definition("[{\"key\":\"m\",\"values\":[\"0\",\"1\"]}]", """
                [{"key":"ads0","repeatMode":"Toggle","actions":[{"actionType":"SwitchMode","delay":1}],
                  "modifiers":[{"stat":"mode","setValue":"feedback"},{"stat":"set_value","setValue":"due"}]},
                 {"key":"switch","actions":[{"actionType":"SwitchMode"}],"modifiers":[{"stat":"mode","setValue":"m"}]}]
                """, "[{\"inputType\":\"Fire2\",\"nodes\":[{\"actionGroupToTrigger\":\"ads0\",\"modalCheck\":\"m:0\"}]}]");
        // Add feedback mode so the delayed action has a real destination.
        var json = GunDef.CODEC.encodeStart(JsonOps.INSTANCE, def).getOrThrow().getAsJsonObject();
        json.getAsJsonArray("modes").add(JsonParser.parseString("{\"key\":\"feedback\",\"values\":[\"due\"]}"));
        var actual = GunDef.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        gun.set(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE.withMode("m", "0"));
        withDefinition(actual, () -> { input(player, gun, "Fire2"); trigger(player, gun, actual, "switch"); });
        helper.runAtTickTime(1, () -> {
            GunEngine.tick(player, gun);
            var state = state(gun); clean(player, gun);
            helper.assertTrue(!state.activeToggles().contains("ads0"), "Mode mismatch must finish old sight toggle");
            helper.assertTrue("due".equals(state.modeValues().get("feedback")), "Finished old group still receives final pending callback tick");
            helper.succeed();
        });
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void modalColonUsesFirstValueFieldAndUnknownKeysStayEmpty(GameTestHelper helper) {
        var gun = item("mars"); var player = player(helper, gun);
        var def = definition("[{\"key\":\"m\",\"values\":[\"on\"]}]", "[{\"key\":\"utility\",\"repeatMode\":\"Toggle\",\"actions\":[{\"actionType\":\"AimDownSights\"}]}]",
                "[{\"inputType\":\"SpecialKey1\",\"nodes\":[{\"actionGroupToTrigger\":\"utility\",\"modalCheck\":\"m:on:ignored\"}]}]");
        gun.set(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE.withMode("m", "on"));
        try {
            withDefinition(def, () -> {
                input(player, gun, "SpecialKey1");
                helper.assertTrue(state(gun).activeToggles().equals(List.of("utility")), "Original split takes field1 and ignores subsequent colon fields");
            });
        } finally { clean(player, gun); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void modeCompoundPresenceAndMissingKeysSurviveSaveAndPacket(GameTestHelper helper) {
        var def = definition("[{\"key\":\"m\",\"values\":[\"ready\"],\"defaultValue\":\"ready\"}]", "[{\"key\":\"utility\",\"repeatMode\":\"Toggle\",\"actions\":[{\"actionType\":\"AimDownSights\"}]}]",
                "[{\"inputType\":\"SpecialKey1\",\"nodes\":[{\"actionGroupToTrigger\":\"utility\",\"modalCheck\":\"m:ready\"}]}]");
        var emptyPresent = GunActionState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"modesInitialized\":true}")).getOrThrow();
        var legacyNonempty = GunActionState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"modeValues\":{\"other\":\"on\"}}")).getOrThrow();
        for (var initial : List.of(GunActionState.IDLE, emptyPresent, legacyNonempty)) {
            var gun = item("mars"); gun.set(ModDataComponents.GUN_ACTION_STATE, initial);
            var loaded = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag)gun.save(helper.getLevel().registryAccess()));
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            ItemStack decoded;
            try {
                ItemStack.STREAM_CODEC.encode(buffer, loaded); decoded = ItemStack.STREAM_CODEC.decode(buffer);
                helper.assertTrue(buffer.readableBytes() == 0 && state(decoded).equals(initial), "Saved/packet component must retain absent, empty-present and legacy nonempty mode storage");
            } finally { buffer.release(); }
            var player = player(helper, decoded);
            try {
                withDefinition(def, () -> input(player, decoded, "SpecialKey1"));
                helper.assertTrue(state(decoded).activeToggles().contains("utility") == (initial == GunActionState.IDLE),
                        "Only absent mode compound uses declared default after save/packet roundtrip");
            } finally { clean(player, decoded); }
        }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar", timeoutTicks = 15)
    public static void directModeWriteDoesNotEnforceUnusedAutoCancelField(GameTestHelper helper) {
        var gun = item("mars"); var player = player(helper, gun);
        var def = definition("[{\"key\":\"m\",\"values\":[\"on\",\"off\"]}]",
                "[{\"key\":\"utility\",\"repeatMode\":\"Toggle\",\"autoCancelIfNotInMode\":\"m:on\",\"actions\":[{\"actionType\":\"AimDownSights\"}]}]",
                "[{\"inputType\":\"SpecialKey1\",\"nodes\":[{\"actionGroupToTrigger\":\"utility\",\"modalCheck\":\"m:on\"}]}]");
        gun.set(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE.withMode("m", "on"));
        withDefinition(def, () -> input(player, gun, "SpecialKey1"));
        gun.set(ModDataComponents.GUN_ACTION_STATE, state(gun).withMode("m", "off"));
        helper.runAtTickTime(1, () -> {
            GunEngine.tick(player, gun); var state = state(gun); clean(player, gun);
            helper.assertTrue(state.activeToggles().contains("utility"), "Only SwitchModeAction scans cancellations; direct value writes and unused autoCancel field do not");
            helper.succeed();
        });
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar", timeoutTicks = 15)
    public static void switchCancellationKeepsOriginalModeKeyPrefixMatch(GameTestHelper helper) {
        var gun = item("mars"); var player = player(helper, gun);
        var def = definition("[{\"key\":\"mode\",\"values\":[\"on\",\"off\"]},{\"key\":\"mode_extra\",\"values\":[\"on\"]}]", """
                [{"key":"utility","repeatMode":"Toggle","actions":[{"actionType":"AimDownSights"}]},{"key":"switch","actions":[{"actionType":"SwitchMode"}],
                  "modifiers":[{"stat":"set_value","setValue":"off"}]}]
                """, "[{\"inputType\":\"SpecialKey1\",\"nodes\":[{\"actionGroupToTrigger\":\"utility\",\"modalCheck\":\"mode_extra:on\"}]}]");
        gun.set(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE.withMode("mode_extra", "on"));
        withDefinition(def, () -> { input(player, gun, "SpecialKey1"); trigger(player, gun, def, "switch"); });
        helper.runAtTickTime(1, () -> {
            GunEngine.tick(player, gun); var state = state(gun); clean(player, gun);
            helper.assertTrue(!state.activeToggles().contains("utility") && "on".equals(state.modeValues().get("mode_extra")),
                    "Source startsWith mode cancels mode_extra group's toggle without changing mode_extra itself");
            helper.succeed();
        });
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void attachedGunOverridesWinOverGroupModeSelection(GameTestHelper helper) {
        var gun = item("mars"); var player = player(helper, gun);
        var def = definition("[{\"key\":\"m\",\"values\":[\"a\",\"b\"]},{\"key\":\"attached\",\"values\":[\"x\",\"y\"]}]", """
                [{"key":"switch","actions":[{"actionType":"SwitchMode"}],
                  "modifiers":[{"stat":"mode","setValue":"m"},{"stat":"set_value","setValue":"group"}]}]
                """, "[]");
        var attachment = AttachmentDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"modifiers":[{"stat":"mode","setValue":"attached"},{"stat":"set_value","setValue":"attachment"}]}
                """)).getOrThrow();
        var original = GunContent.attachments().put("optical_scope", attachment);
        gun.set(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY.with("scope", List.of(item("optical_scope"))));
        try {
            trigger(player, gun, def, "switch");
            helper.assertTrue("attachment".equals(state(gun).modeValues().get("attached")) && !state(gun).modeValues().containsKey("m"),
                    "Original gun/attachment string override outranks group mode and set_value");
        } finally { GunContent.attachments().put("optical_scope", original); clean(player, gun); }
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar", timeoutTicks = 15)
    public static void attachedCancellationUsesLastChildAndAliasesBaseKey(GameTestHelper helper) {
        var gun = item("mars"); var player = player(helper, gun);
        var def = definition("[{\"key\":\"m\",\"values\":[\"0\",\"1\"]}]", """
                [{"key":"first","repeatMode":"Toggle","actions":[{"actionType":"AimDownSights"}]},{"key":"shared","repeatMode":"Toggle","actions":[{"actionType":"AimDownSights"}]},
                 {"key":"switch","actions":[{"actionType":"SwitchMode"}],"modifiers":[{"stat":"mode","setValue":"m"}]}]
                """, """
                [{"inputType":"Fire2","nodes":[{"deferToAttachment":true,"attachmentType":"Sights","modalCheck":"m:0"}]},
                 {"inputType":"SpecialKey1","nodes":[{"actionGroupToTrigger":"first"}]},
                 {"inputType":"SpecialKey2","nodes":[{"actionGroupToTrigger":"shared"}]}]
                """);
        var attachment = AttachmentDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"handlerOverrides":[{"inputType":"Fire2","nodes":[{"actionGroupToTrigger":"first"},{"actionGroupToTrigger":"shared"}]}],
                 "actionOverrides":[{"key":"first","repeatMode":"Toggle","actions":[{"actionType":"AimDownSights"}]},{"key":"shared","repeatMode":"Toggle","actions":[{"actionType":"AimDownSights"}]}]}
                """)).getOrThrow();
        var original = GunContent.attachments().put("optical_scope", attachment);
        gun.set(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY.with("scope", List.of(item("optical_scope"))));
        gun.set(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE.withMode("m", "0"));
        try {
            withDefinition(def, () -> { input(player, gun, "SpecialKey1"); input(player, gun, "SpecialKey2"); trigger(player, gun, def, "switch"); });
        } finally { GunContent.attachments().put("optical_scope", original); }
        helper.runAtTickTime(1, () -> {
            GunEngine.tick(player, gun); var state = state(gun); clean(player, gun);
            helper.assertTrue(state.activeToggles().equals(List.of("first")),
                    "Cancellation scans last attachment child only, and plain shared key aliases the active base group");
            helper.succeed();
        });
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar", timeoutTicks = 15)
    public static void absentOrEmptyAttachmentHandlerRetainsRootCancellationPath(GameTestHelper helper) {
        var def = definition("[{\"key\":\"m\",\"values\":[\"0\",\"1\"]}]", """
                [{"key":"shared","repeatMode":"Toggle","actions":[{"actionType":"AimDownSights"}]},{"key":"switch","actions":[{"actionType":"SwitchMode"}],
                  "modifiers":[{"stat":"mode","setValue":"m"}]}]
                """, """
                [{"inputType":"Fire2","nodes":[{"deferToAttachment":true,"attachmentType":"Sights","actionGroupToTrigger":"shared","modalCheck":"m:0"}]},
                 {"inputType":"SpecialKey1","nodes":[{"actionGroupToTrigger":"shared"}]}]
                """);
        var guns = new java.util.ArrayList<ItemStack>();
        var players = new java.util.ArrayList<FakePlayer>();
        var empty = AttachmentDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"handlerOverrides\":[{\"inputType\":\"Fire2\",\"nodes\":[]}]}")).getOrThrow();
        var original = GunContent.attachments().put("optical_scope", empty);
        try {
            for (boolean installed : List.of(false, true)) {
                var gun = item("mars"); var player = player(helper, gun); guns.add(gun); players.add(player);
                if (installed) gun.set(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY.with("scope", List.of(item("optical_scope"))));
                gun.set(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE.withMode("m", "0"));
                withDefinition(def, () -> { input(player, gun, "SpecialKey1"); trigger(player, gun, def, "switch"); });
            }
        } finally { GunContent.attachments().put("optical_scope", original); }
        helper.runAtTickTime(1, () -> {
            for (int index = 0; index < guns.size(); index++) {
                var gun = guns.get(index); var player = players.get(index);
                GunEngine.tick(player, gun); var state = state(gun); clean(player, gun);
                helper.assertTrue(!state.activeToggles().contains("shared"), "Without any child path overwrite, source retains the root cancellation path; variant=" + index);
            }
            helper.succeed();
        });
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar", timeoutTicks = 15)
    public static void firingGroupCanSwitchAndFinishItselfWithoutLosingItsCallbackContext(GameTestHelper helper) {
        var gun = item("mars"); var player = player(helper, gun);
        var def = definition("[{\"key\":\"m\",\"values\":[\"0\",\"1\"]}]", """
                [{"key":"primary_fire","repeatMode":"FullAuto","repeatDelay":0.1,
                  "actions":[{"actionType":"Shoot"},{"actionType":"SwitchMode"}],"modifiers":[{"stat":"mode","setValue":"m"}]}]
                """, "[{\"inputType\":\"Fire1\",\"nodes\":[{\"actionGroupToTrigger\":\"primary_fire\",\"modalCheck\":\"m:0\"}]}]");
        gun.set(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE.withMode("m", "0"));
        gun.set(ModDataComponents.GUN_LOADED_ROUNDS, new net.pttheta.loveandwar.item.custom.AmmoPouchContents(List.of(item("pistol_bullet"), item("pistol_bullet"), item("pistol_bullet"))));
        withDefinition(def, () -> GunEngine.handleInput(player, gun, InteractionHand.MAIN_HAND, "Fire1", true));
        helper.assertTrue("1".equals(state(gun).modeValues().get("m")), "Initial callback can switch its own handler mode");
        helper.runAtTickTime(1, () -> {
            withDefinition(def, () -> GunEngine.tick(player, gun));
            var state = state(gun); clean(player, gun);
            helper.assertTrue(state.activeGroup().isEmpty(), "Self-finished firing group exits through normal final tick");
            helper.assertTrue(gun.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, net.pttheta.loveandwar.item.custom.AmmoPouchContents.EMPTY).items().size() == 2,
                    "Mode cancellation does not add a client-only final trigger as authoritative server damage");
            helper.succeed();
        });
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void savedOrLoggedOutSightToggleCannotResumeWithoutRuntimeOwner(GameTestHelper helper) {
        var source = item("krag");
        source.set(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE.withMode("iron_sight", "0").withToggle("ads_0", true));
        var gun = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag)source.save(helper.getLevel().registryAccess()));
        var player = player(helper, gun);
        try {
            GunEngine.tick(player, gun);
            helper.assertTrue(state(gun).activeToggles().isEmpty() && "0".equals(state(gun).modeValues().get("iron_sight")),
                    "Saved mode survives, but runtime-only sight activation does not resume");
            input(player, gun, "Fire2");
            helper.assertTrue(state(gun).activeToggles().contains("ads_0"), "Fresh sight input creates a current runtime owner");
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(player));
            GunEngine.tick(player, gun);
            helper.assertTrue(state(gun).activeToggles().isEmpty(), "Logout invalidates old sight runtime ownership");
        } finally { clean(player, gun); }
        helper.succeed();
    }
}
