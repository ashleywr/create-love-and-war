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
import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.content.AttachmentDef;
import net.pttheta.loveandwar.gun.content.GunContent;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.item.ModDataComponents;

import java.util.List;
import java.util.UUID;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class InputContinuationGameTests {
    private static void attached(String nodes, Runnable test) {
        attached(nodes, "[" + group("attached", "Toggle", "a") + "]", test);
    }

    private static void attached(String nodes, String groups, Runnable test) {
        var old = GunContent.attachment("attachment_red_dot").orElseThrow();
        var json =
                AttachmentDef.CODEC
                        .encodeStart(JsonOps.INSTANCE, old)
                        .getOrThrow()
                        .getAsJsonObject();
        json.add(
                "handlerOverrides",
                JsonParser.parseString(
                        "[{\"inputType\":\"SpecialKey1\",\"nodes\":" + nodes + "}]"));
        json.add("actionOverrides", JsonParser.parseString(groups));
        GunContent.attachments()
                .put(
                        "attachment_red_dot",
                        AttachmentDef.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        try {
            test.run();
        } finally {
            GunContent.attachments().put("attachment_red_dot", old);
        }
    }

    private static void install(ItemStack gun) {
        var item =
                new ItemStack(
                        BuiltInRegistries.ITEM.get(
                                ResourceLocation.parse("createloveandwar:attachment_red_dot")));
        gun.set(
                ModDataComponents.GUN_ATTACHMENTS,
                GunAttachments.EMPTY.with("scope", List.of(item)));
    }

    private static String group(String key, String mode, String counter) {
        return "{\"key\":\""
                + key
                + "\",\"repeatMode\":\""
                + mode
                + "\",\"actions\":[{\"actionType\":\"SwitchMode\",\"duration\":0.4}],\"modifiers\":[{\"stat\":\"mode\",\"setValue\":\""
                + counter
                + "\"}]}";
    }

    private static void fixture(
            GameTestHelper h,
            String groups,
            String nodes,
            java.util.function.BiConsumer<FakePlayer, ItemStack> test) {
        var original = GunContent.gun("mars").orElseThrow();
        var json =
                GunDef.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow().getAsJsonObject();
        json.add(
                "modes",
                JsonParser.parseString(
                        "[{\"key\":\"a\",\"values\":[\"0\",\"1\",\"2\"],\"defaultValue\":\"0\"},{\"key\":\"b\",\"values\":[\"0\",\"1\",\"2\"],\"defaultValue\":\"0\"}]"));
        json.add("actionGroups", JsonParser.parseString(groups));
        json.add(
                "inputHandlers",
                JsonParser.parseString(
                        "[{\"inputType\":\"SpecialKey1\",\"nodes\":" + nodes + "}]"));
        var gun =
                new ItemStack(
                        BuiltInRegistries.ITEM.get(
                                ResourceLocation.parse("createloveandwar:mars")));
        var p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "ContinueTest"));
        p.setPos(h.absolutePos(new BlockPos(2, 6, 2)).getCenter());
        p.setItemInHand(InteractionHand.MAIN_HAND, gun);
        GunContent.guns().put("mars", GunDef.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        try {
            test.accept(p, gun);
        } finally {
            GunFireScheduler.cancel(p, gun);
            GunActionTimeline.cancelAll(p, gun);
            GunContent.guns().put("mars", original);
        }
    }

    private static void press(FakePlayer p, ItemStack gun) {
        GunEngine.handleInput(p, gun, InteractionHand.MAIN_HAND, "SpecialKey1", true);
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void successfulContinuingRootStartsBothToggleGroupsInOrder(GameTestHelper h) {
        fixture(
                h,
                "[" + group("first", "Toggle", "a") + "," + group("second", "Toggle", "b") + "]",
                "[{\"actionGroupToTrigger\":\"first\",\"andContinueEvaluating\":true},{\"actionGroupToTrigger\":\"second\"}]",
                (p, gun) -> {
                    press(p, gun);
                    var state =
                            gun.getOrDefault(
                                    ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
                    // First switch creates the mode compound; the second missing key reads empty
                    // and cycles to0.
                    h.assertTrue(
                            "1".equals(state.modeValues().get("a"))
                                    && "0".equals(state.modeValues().get("b")),
                            "Both continuing nodes execute their independent actions: "
                                    + state.modeValues());
                    h.assertTrue(
                            GunActiveGroups.of(gun).entries().stream()
                                    .map(GunActiveGroups.Entry::key)
                                    .toList()
                                    .equals(List.of("first", "second")),
                            "Instances retain input activation order");
                });
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void continuingSemiGroupsDoNotBlockEachOtherAsGloballyBusy(GameTestHelper h) {
        fixture(
                h,
                "["
                        + group("first", "SemiAuto", "a")
                        + ","
                        + group("second", "SemiAuto", "b")
                        + "]",
                "[{\"actionGroupToTrigger\":\"first\",\"andContinueEvaluating\":true},{\"actionGroupToTrigger\":\"second\"}]",
                (p, gun) -> {
                    press(p, gun);
                    h.assertTrue(
                            GunActiveGroups.of(gun).entries().size() == 2,
                            "Source CanStart has no global busy lock between independent action"
                                    + " groups");
                });
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void modalInputCandidatesAreSnapshottedBeforeFirstActionChangesMode(
            GameTestHelper h) {
        fixture(
                h,
                "[" + group("first", "Toggle", "a") + "," + group("second", "Toggle", "b") + "]",
                "[{\"actionGroupToTrigger\":\"first\",\"andContinueEvaluating\":true},{\"actionGroupToTrigger\":\"second\",\"modalCheck\":\"a:0\"}]",
                (p, gun) -> {
                    press(p, gun);
                    var state =
                            gun.getOrDefault(
                                    ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
                    h.assertTrue(
                            "0".equals(state.modeValues().get("b")),
                            "Already evaluated second candidate remains eligible after first"
                                    + " changes a: "
                                    + state.modeValues());
                });
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void modeChangeCannotAddAnInputCandidateMidEvaluation(GameTestHelper h) {
        fixture(
                h,
                "[" + group("first", "Toggle", "a") + "," + group("second", "Toggle", "b") + "]",
                "[{\"actionGroupToTrigger\":\"first\",\"andContinueEvaluating\":true},{\"actionGroupToTrigger\":\"second\",\"modalCheck\":\"a:1\"}]",
                (p, gun) -> {
                    press(p, gun);
                    var state =
                            gun.getOrDefault(
                                    ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
                    h.assertTrue(
                            "1".equals(state.modeValues().get("a"))
                                    && !state.modeValues().containsKey("b"),
                            "Newly matching mode cannot add a candidate after evaluation snapshot");
                });
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void successfulNonContinuingRootStopsLaterCandidates(GameTestHelper h) {
        fixture(
                h,
                "[" + group("first", "Toggle", "a") + "," + group("second", "Toggle", "b") + "]",
                "[{\"actionGroupToTrigger\":\"first\"},{\"actionGroupToTrigger\":\"second\"}]",
                (p, gun) -> {
                    press(p, gun);
                    h.assertTrue(
                            GunActiveGroups.of(gun).entries().stream()
                                    .map(GunActiveGroups.Entry::key)
                                    .toList()
                                    .equals(List.of("first")),
                            "Successful stop flag is honored");
                });
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void rejectedNonContinuingRootAllowsFallback(GameTestHelper h) {
        fixture(
                h,
                "[{\"key\":\"blocked\",\"actions\":[{\"actionType\":\"Shoot\"}]},"
                        + group("second", "Toggle", "b")
                        + "]",
                "[{\"actionGroupToTrigger\":\"blocked\"},{\"actionGroupToTrigger\":\"second\"}]",
                (p, gun) -> {
                    press(p, gun);
                    var state =
                            gun.getOrDefault(
                                    ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
                    h.assertTrue(
                            "1".equals(state.modeValues().get("b"))
                                    && GunActiveGroups.of(gun)
                                            .entries()
                                            .getFirst()
                                            .key()
                                            .equals("second"),
                            "Rejected root falls through irrespective of its stop flag");
                });
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void repeatedPlainKeyRetainsSingleInstanceWithinContinuingInput(
            GameTestHelper h) {
        fixture(
                h,
                "[" + group("shared", "SemiAuto", "a") + "]",
                "[{\"actionGroupToTrigger\":\"shared\",\"andContinueEvaluating\":true},{\"actionGroupToTrigger\":\"shared\"}]",
                (p, gun) -> {
                    press(p, gun);
                    var state =
                            gun.getOrDefault(
                                    ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
                    h.assertTrue(
                            "2".equals(state.modeValues().get("a"))
                                    && GunActiveGroups.of(gun).entries().size() == 1,
                            "Repeated key retriggers the first instance twice, rather than"
                                    + " replacing/duplicating it");
                });
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void attachmentChildContinueOverridesStoppingParent(GameTestHelper h) {
        attached(
                "[{\"actionGroupToTrigger\":\"attached\",\"andContinueEvaluating\":true}]",
                () ->
                        fixture(
                                h,
                                "[" + group("second", "Toggle", "b") + "]",
                                "[{\"deferToAttachment\":true,\"attachmentType\":\"Sights\"},{\"actionGroupToTrigger\":\"second\"}]",
                                (p, gun) -> {
                                    install(gun);
                                    press(p, gun);
                                    h.assertTrue(
                                            GunActiveGroups.of(gun).entries().stream()
                                                    .map(GunActiveGroups.Entry::key)
                                                    .toList()
                                                    .equals(List.of("attached", "second")),
                                            "Child continuation replaces parent stop flag");
                                }));
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void attachmentChildStopOverridesContinuingParent(GameTestHelper h) {
        attached(
                "[{\"actionGroupToTrigger\":\"attached\"}]",
                () ->
                        fixture(
                                h,
                                "[" + group("second", "Toggle", "b") + "]",
                                "[{\"deferToAttachment\":true,\"attachmentType\":\"Sights\",\"andContinueEvaluating\":true},{\"actionGroupToTrigger\":\"second\"}]",
                                (p, gun) -> {
                                    install(gun);
                                    press(p, gun);
                                    h.assertTrue(
                                            GunActiveGroups.of(gun).entries().stream()
                                                    .map(GunActiveGroups.Entry::key)
                                                    .toList()
                                                    .equals(List.of("attached")),
                                            "Bundled gadget child stop flag replaces continuing"
                                                    + " parent");
                                }));
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void nestedAttachmentDeferralIsIgnoredRatherThanRecursing(GameTestHelper h) {
        attached(
                "[{\"deferToAttachment\":true,\"attachmentType\":\"Sights\"},{\"actionGroupToTrigger\":\"attached\"}]",
                () ->
                        fixture(
                                h,
                                "[]",
                                "[{\"deferToAttachment\":true,\"attachmentType\":\"Sights\"}]",
                                (p, gun) -> {
                                    install(gun);
                                    press(p, gun);
                                    h.assertTrue(
                                            GunActiveGroups.of(gun)
                                                    .entries()
                                                    .getFirst()
                                                    .key()
                                                    .equals("attached"),
                                            "Source ignores a nested deferral then evaluates the"
                                                    + " next child");
                                }));
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void invalidRootGroupsCannotConsumeOrExecuteInput(GameTestHelper h) {
        fixture(
                h,
                "["
                        + group("", "Toggle", "a")
                        + ", {\"key\":\"empty\",\"actions\":[]},"
                        + group("second", "Toggle", "b")
                        + "]",
                "[{\"actionGroupToTrigger\":\"\"},{\"actionGroupToTrigger\":\"empty\"},{\"actionGroupToTrigger\":\"second\"}]",
                (p, gun) -> {
                    press(p, gun);
                    var state =
                            gun.getOrDefault(
                                    ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
                    h.assertTrue(
                            !state.modeValues().containsKey("a")
                                    && "1".equals(state.modeValues().get("b"))
                                    && GunActiveGroups.of(gun).entries().stream()
                                            .map(GunActiveGroups.Entry::key)
                                            .toList()
                                            .equals(List.of("second")),
                            "Source IsValid skips empty keys and action arrays before input"
                                + " iteration");
                });
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void invalidAttachmentGroupsCannotTruncateChildExpansion(GameTestHelper h) {
        attached(
                "[{\"actionGroupToTrigger\":\"\"},{\"actionGroupToTrigger\":\"empty\"},{\"actionGroupToTrigger\":\"attached\"}]",
                "["
                        + group("", "Toggle", "b")
                        + ", {\"key\":\"empty\",\"actions\":[]},"
                        + group("attached", "Toggle", "a")
                        + "]",
                () ->
                        fixture(
                                h,
                                "[]",
                                "[{\"deferToAttachment\":true,\"attachmentType\":\"Sights\"}]",
                                (p, gun) -> {
                                    install(gun);
                                    press(p, gun);
                                    var state =
                                            gun.getOrDefault(
                                                    ModDataComponents.GUN_ACTION_STATE,
                                                    GunActionState.IDLE);
                                    h.assertTrue(
                                            "1".equals(state.modeValues().get("a"))
                                                    && !state.modeValues().containsKey("b")
                                                    && GunActiveGroups.of(gun).entries().stream()
                                                            .map(GunActiveGroups.Entry::key)
                                                            .toList()
                                                            .equals(List.of("attached")),
                                            "Invalid children do not execute or apply a stopping"
                                                + " flag");
                                }));
        h.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void independentTimedInstancesReachTheirOwnFinishBoundary(GameTestHelper h) {
        var def = GunContent.gun("mars").orElseThrow();
        var gun =
                new ItemStack(
                        BuiltInRegistries.ITEM.get(
                                ResourceLocation.parse("createloveandwar:mars")));
        var p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "ContinueClock"));
        p.setPos(h.absolutePos(new BlockPos(2, 6, 2)).getCenter());
        p.setItemInHand(InteractionHand.MAIN_HAND, gun);
        for (String key : List.of("first", "second")) {
            var group =
                    ActionGroup.CODEC
                            .parse(
                                    JsonOps.INSTANCE,
                                    JsonParser.parseString(
                                            "{\"key\":\""
                                                    + key
                                                    + "\",\"actions\":[{\"actionType\":\"AimDownSights\",\"duration\":0.4}]}"))
                            .getOrThrow();
            GunEngine.triggerActions(p, gun, def, group, GunAttachments.EMPTY);
        }
        for (int tick = 1; tick <= 9; tick++) {
            int phase = tick;
            h.runAtTickTime(
                    tick,
                    () -> {
                        try {
                            GunEngine.tick(p, gun);
                            h.assertTrue(
                                    GunActiveGroups.of(gun).entries().size()
                                            == (phase <= 8 ? 2 : 0),
                                    "Independent strict finish boundary at phase" + phase);
                            if (phase == 9) h.succeed();
                        } finally {
                            if (phase == 9) {
                                GunFireScheduler.cancel(p, gun);
                                GunActionTimeline.cancelAll(p, gun);
                            }
                        }
                    });
        }
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void allowedIndependentActionPreservesTheActualReloadStage(GameTestHelper h) {
        var groups =
                GunDef.CODEC
                        .encodeStart(JsonOps.INSTANCE, GunContent.gun("mars").orElseThrow())
                        .getOrThrow()
                        .getAsJsonObject()
                        .getAsJsonArray("actionGroups");
        groups.add(JsonParser.parseString(group("utility", "SemiAuto", "a")));
        fixture(
                h,
                groups.toString(),
                "[{\"actionGroupToTrigger\":\"utility\",\"canTriggerWhileReloading\":true}]",
                (p, gun) -> {
                    p.getInventory()
                            .setItem(
                                    8,
                                    new ItemStack(
                                            BuiltInRegistries.ITEM.get(
                                                    ResourceLocation.parse(
                                                            "createloveandwar:pistol_bullet")),
                                            3));
                    var def = GunContent.gun("mars").orElseThrow();
                    GunReload.start(p, gun, def, def.reloads().getFirst(), GunActionState.IDLE);
                    var before =
                            gun.getOrDefault(
                                            ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE)
                                    .activeGroup();
                    h.assertTrue(
                            before.equals(def.reloads().getFirst().startActionKey())
                                    && GunActionTimeline.owns(p, gun, before),
                            "Fixture starts a real reload instance");
                    press(p, gun);
                    var state =
                            gun.getOrDefault(
                                    ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
                    h.assertTrue(
                            state.activeGroup().equals(before)
                                    && GunActionTimeline.owns(p, gun, "utility")
                                    && "1".equals(state.modeValues().get("a")),
                            "Independent allowed action executes without replacing the reload"
                                    + " controller state");
                });
        h.succeed();
    }
}
