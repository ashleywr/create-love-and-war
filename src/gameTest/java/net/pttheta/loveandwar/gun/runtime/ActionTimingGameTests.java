package net.pttheta.loveandwar.gun.runtime;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.content.GunContent;
import net.pttheta.loveandwar.gun.network.SGunSound;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class ActionTimingGameTests {
    private static long reloadSoundCount(GunAudioGameTests.Listener listener) {
        return listener.packets.stream().filter(packet -> packet instanceof ClientboundCustomPayloadPacket custom
                && custom.payload() instanceof SGunSound sound && sound.sound().sound().equals("flansmod:magazine_load_latch")
                && sound.position().equals(listener.player.getEyePosition())).count();
    }
    private static ItemStack stack(String name) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:" + name)));
    }
    private static FakePlayer player(GameTestHelper helper, ItemStack gun) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "TimingTest"));
        player.setPos(helper.absoluteVec(new Vec3(2.5, 6, 2.5)));
        player.setXRot(-90);
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        return player;
    }
    private static ActionGroup group(String json) {
        return ActionGroup.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void authoredVetterliVitaliCasingWaitsFiveTicks(GameTestHelper helper) {
        var gun = stack("vetterlivitali");
        var player = player(helper, gun);
        var def = GunContent.gun("vetterlivitali").orElseThrow();
        var group = def.actionGroups().stream().filter(g -> g.key().equals("reload_primary_eject")).findFirst().orElseThrow();
        var casing = BuiltInRegistries.ITEM.get(ResourceLocation.parse(GunEngine.findMagazineDef(player, def).orElseThrow().spawnBulletCasing()));
        helper.assertTrue(GunEngine.findMagazineDef(player, def).orElseThrow().ejectCasings(), "Fixture magazine must actually eject casings");
        gun.set(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE.withGroup(group.key(), 31, List.of()));
        GunEngine.triggerSounds(player, gun, def, group);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(3), e -> e.getItem().is(casing)).isEmpty(),
                "Authored delay5 must not eject a casing at trigger time");
        for (int tick = 1; tick <= 6; tick++) {
            int current = tick;
            helper.runAtTickTime(tick, () -> {
                GunEngine.tick(player, gun);
                var casings = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(3), e -> e.getItem().is(casing));
                helper.assertTrue(casings.size() == (current < 5 ? 0 : 1), "Casing must execute once at tick5; tick=" + current + " count=" + casings.size());
                if (current == 6) { casings.forEach(ItemEntity::discard); helper.succeed(); }
            });
        }
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void authoredWz38ReloadSoundsWaitTenAndFortyTicks(GameTestHelper helper) throws ReflectiveOperationException {
        var listener = new GunAudioGameTests.Listener(helper);
        var gun = stack("wz38");
        listener.player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        var def = GunContent.gun("wz38").orElseThrow();
        var group = def.actionGroups().stream().filter(g -> g.key().equals("reload_primary_load_one")).findFirst().orElseThrow();
        gun.set(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE.withGroup(group.key(), 56, List.of()));
        GunEngine.triggerSounds(listener.player, gun, def, group);
        if (reloadSoundCount(listener) != 0) listener.close();
        helper.assertTrue(reloadSoundCount(listener) == 0, "Both authored reload sounds are delayed, so trigger time is silent");
        for (int tick = 1; tick <= 41; tick++) {
            int current = tick;
            helper.runAtTickTime(tick, () -> {
                GunEngine.tick(listener.player, gun);
                int expected = current < 10 ? 0 : current < 40 ? 1 : 2;
                if (reloadSoundCount(listener) != expected) listener.close();
                helper.assertTrue(reloadSoundCount(listener) == expected, "Reload sound offsets10/40; tick=" + current + " packets=" + reloadSoundCount(listener));
                if (current == 41) { listener.close(); helper.succeed(); }
            });
        }
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void retriggerReplacesPendingDelayedShot(GameTestHelper helper) {
        var gun = stack("g11k2");
        var player = player(helper, gun);
        gun.set(ModDataComponents.GUN_LOADED_ROUNDS, new AmmoPouchContents(List.of(stack("caseless_bullet"), stack("caseless_bullet"), stack("caseless_bullet"))));
        var def = GunContent.gun("g11k2").orElseThrow();
        var group = group("{\"key\":\"primary_fire\",\"repeatMode\":\"SemiAuto\",\"actions\":[{\"actionType\":\"Shoot\",\"delay\":3,\"duration\":1}]}");
        GunFireScheduler.start(player, gun, def, group, GunAttachments.EMPTY);
        helper.assertTrue(gun.get(ModDataComponents.GUN_LOADED_ROUNDS).items().size() == 3, "A delayed Shoot action must not consume ammunition immediately");
        for (int tick = 1; tick <= 5; tick++) {
            int current = tick;
            helper.runAtTickTime(tick, () -> {
                GunEngine.tick(player, gun);
                if (current == 1) GunFireScheduler.start(player, gun, def, group, GunAttachments.EMPTY);
                int expected = current < 4 ? 3 : 2;
                helper.assertTrue(gun.get(ModDataComponents.GUN_LOADED_ROUNDS).items().size() == expected,
                        "Original pending-action map overwrites delay3 at tick1, firing once at4; tick=" + current);
                if (current == 5) helper.succeed();
            });
        }
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void actionDelayDoesNotExtendOriginalGroupLifetime(GameTestHelper helper) {
        var group = group("{\"key\":\"timing\",\"repeatDelay\":0.02,\"actions\":[{\"actionType\":\"PlaySound\",\"delay\":40,\"duration\":0.558}]}");
        helper.assertTrue(GunEngine.groupDurationTicks(group) == 11, "Original duration floors max(duration,repeatDelay)*20 and excludes delay offsets");
        helper.succeed();
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void fractionalNegativeAndDuplicateDelaysKeepDistinctPendingActions(GameTestHelper helper) {
        var def = GunContent.gun("g11k2").orElseThrow();
        for (float delay : new float[]{0, 0.4F, -0.4F, 2.9F}) {
            var gun = stack("g11k2"); var owner = player(helper, gun);
            gun.set(ModDataComponents.GUN_LOADED_ROUNDS, new AmmoPouchContents(List.of(stack("caseless_bullet"), stack("caseless_bullet"), stack("caseless_bullet"))));
            var group = group("{\"key\":\"primary_fire\",\"actions\":[{\"actionType\":\"Shoot\",\"delay\":" + delay + ",\"duration\":1},{\"actionType\":\"Shoot\",\"delay\":" + delay + ",\"duration\":1}]}");
            GunFireScheduler.start(owner, gun, def, group, GunAttachments.EMPTY);
            helper.assertTrue(gun.get(ModDataComponents.GUN_LOADED_ROUNDS).items().size() == (delay == 0 ? 1 : 3), "Only exactly zero delay is immediate");
            for (int tick = 1; tick <= 3; tick++) {
                int current = tick;
                helper.runAtTickTime(tick, () -> {
                    GunEngine.tick(owner, gun);
                    int due = delay == 0 ? 0 : Math.max(1, (int)delay);
                    helper.assertTrue(gun.get(ModDataComponents.GUN_LOADED_ROUNDS).items().size() == (current >= due ? 1 : 3),
                            "Both identical action entries must execute independently at truncated tick " + due + "; delay=" + delay + " tick=" + current);
                });
            }
        }
        helper.runAtTickTime(4, helper::succeed);
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void delayPastGroupLifetimeIsDiscarded(GameTestHelper helper) {
        var def = GunContent.gun("g11k2").orElseThrow();
        var gun = stack("g11k2"); var owner = player(helper, gun);
        gun.set(ModDataComponents.GUN_LOADED_ROUNDS, new AmmoPouchContents(List.of(stack("caseless_bullet"))));
        var group = group("{\"key\":\"primary_fire\",\"actions\":[{\"actionType\":\"Shoot\",\"delay\":3,\"duration\":0}]}");
        GunFireScheduler.start(owner, gun, def, group, GunAttachments.EMPTY);
        for (int tick = 1; tick <= 4; tick++) helper.runAtTickTime(tick, () -> GunEngine.tick(owner, gun));
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(gun.get(ModDataComponents.GUN_LOADED_ROUNDS).items().size() == 1,
                    "Zero-duration group ticks once then discards pending delay3 instead of draining it after finish");
            helper.succeed();
        });
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar", timeoutTicks = 160)
    public static void original1887ReloadRetainsRepeatedLoadClock(GameTestHelper helper) {
        var gun = stack("1887"); var owner = player(helper, gun);
        owner.getInventory().setItem(1, stack("buckshot_shell").copyWithCount(5));
        GunEngine.handleInput(owner, gun, InteractionHand.MAIN_HAND, "Reload1", true);
        for (int tick = 1; tick <= 117; tick++) {
            int current = tick;
            helper.runAtTickTime(tick, () -> {
                GunEngine.tick(owner, gun);
                int expected = current < 46 ? 0 : current < 58 ? 1 : current < 69 ? 2 : current < 80 ? 3 : current < 91 ? 4 : 5;
                helper.assertTrue(gun.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY).items().size() == expected,
                        "1887 source transfer ticks46/58/69/80/91; tick=" + current);
                if (current == 23) helper.assertTrue(gun.get(ModDataComponents.GUN_ACTION_STATE).activeGroup().equals("reload_primary_eject"), "Eject enters at23");
                if (current == 102) helper.assertTrue(gun.get(ModDataComponents.GUN_ACTION_STATE).activeGroup().equals("reload_primary_end"), "End enters at102");
                if (current == 115) helper.assertTrue(gun.get(ModDataComponents.GUN_ACTION_STATE).activeGroup().equals("reload_primary_end"), "End remains active at115");
                if (current == 116) helper.assertTrue(gun.get(ModDataComponents.GUN_ACTION_STATE).activeGroup().isEmpty(), "Reload completes exactly at116");
                if (current == 117) {
                    helper.assertTrue(gun.get(ModDataComponents.GUN_ACTION_STATE).activeGroup().isEmpty(), "Reload completes at116");
                    helper.getLevel().getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(3)).forEach(ItemEntity::discard);
                    helper.succeed();
                }
            });
        }
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar", timeoutTicks = 100)
    public static void fullMagazineTransfersAtLoadEntryInsteadOfEnd(GameTestHelper helper) {
        var gun = stack("g11k2"); var owner = player(helper, gun);
        owner.getInventory().setItem(1, stack("caseless_bullet").copyWithCount(12));
        GunEngine.handleInput(owner, gun, InteractionHand.MAIN_HAND, "Reload1", true);
        for (int tick = 1; tick <= 75; tick++) {
            int current = tick;
            helper.runAtTickTime(tick, () -> {
                GunEngine.tick(owner, gun);
                int expected = current < 42 ? 0 : 12;
                helper.assertTrue(gun.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY).items().size() == expected,
                        "G11 FullMag transfers every available round at LoadOne entry42; tick=" + current);
                if (current == 53) helper.assertTrue(gun.get(ModDataComponents.GUN_ACTION_STATE).activeGroup().equals("reload_primary_end"), "End enters at53");
                if (current == 73) helper.assertTrue(gun.get(ModDataComponents.GUN_ACTION_STATE).activeGroup().equals("reload_primary_end"), "End remains active at73");
                if (current == 74) helper.assertTrue(gun.get(ModDataComponents.GUN_ACTION_STATE).activeGroup().isEmpty(), "Reload completes exactly at74");
                if (current == 75) {
                    helper.assertTrue(gun.get(ModDataComponents.GUN_ACTION_STATE).activeGroup().isEmpty(), "Reload completes at74");
                    helper.succeed();
                }
            });
        }
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void releasedMat49StillExecutesItsDueCasingOnFinalTick(GameTestHelper helper) {
        var gun = stack("mat49"); var owner = player(helper, gun);
        gun.set(ModDataComponents.GUN_LOADED_ROUNDS, new AmmoPouchContents(List.of(stack("pistol_bullet"), stack("pistol_bullet"))));
        var casing = BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:pistol_casing"));
        GunEngine.handleInput(owner, gun, InteractionHand.MAIN_HAND, "Fire1", true);
        GunEngine.handleInput(owner, gun, InteractionHand.MAIN_HAND, "Fire1", false);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(3), e -> e.getItem().is(casing)).isEmpty(),
                "MAT49 casing delay1 must not execute with the initial shot");
        helper.runAtTickTime(1, () -> {
            GunEngine.tick(owner, gun);
            var casings = helper.getLevel().getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(3), e -> e.getItem().is(casing));
            helper.assertTrue(casings.size() == 1 && gun.get(ModDataComponents.GUN_LOADED_ROUNDS).items().size() == 1,
                    "Release permits the final due casing but cannot create an extra authoritative shot");
            casings.forEach(ItemEntity::discard);
            helper.succeed();
        });
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar", timeoutTicks = 160)
    public static void reloadReevaluatesInventoryInsteadOfUsingAnInitialQueue(GameTestHelper helper) {
        var gun = stack("1887"); var owner = player(helper, gun);
        owner.getInventory().setItem(1, stack("buckshot_shell"));
        GunEngine.handleInput(owner, gun, InteractionHand.MAIN_HAND, "Reload1", true);
        for (int tick = 1; tick <= 117; tick++) {
            int current = tick;
            helper.runAtTickTime(tick, () -> {
                GunEngine.tick(owner, gun);
                if (current == 50) owner.getInventory().setItem(1, stack("buckshot_shell").copyWithCount(4));
                if (current == 91) helper.assertTrue(gun.get(ModDataComponents.GUN_LOADED_ROUNDS).items().size() == 5,
                        "Rounds added during loading must be noticed at the next transition");
                if (current == 117) {
                    helper.assertTrue(gun.get(ModDataComponents.GUN_ACTION_STATE).activeGroup().isEmpty(), "Dynamic reload finishes normally");
                    helper.getLevel().getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(3)).forEach(ItemEntity::discard);
                    helper.succeed();
                }
            });
        }
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar", timeoutTicks = 70)
    public static void deadOwnerCannotAdvanceReloadIntoAnAmmoTransfer(GameTestHelper helper) {
        var gun = stack("g11k2"); var owner = player(helper, gun);
        owner.getInventory().setItem(1, stack("caseless_bullet").copyWithCount(12));
        GunEngine.handleInput(owner, gun, InteractionHand.MAIN_HAND, "Reload1", true);
        for (int tick = 1; tick <= 45; tick++) {
            int current = tick;
            helper.runAtTickTime(tick, () -> {
                if (current == 41) owner.setHealth(0);
                GunEngine.tick(owner, gun);
            });
        }
        helper.runAtTickTime(46, () -> {
            helper.assertTrue(gun.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY).isEmpty()
                    && owner.getInventory().getItem(1).getCount() == 12, "Owner death cancels pending reload work without moving ammunition");
            helper.succeed();
        });
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar", timeoutTicks = 110)
    public static void savedReloadStateCannotResumeOnAnotherStackOrOwner(GameTestHelper helper) {
        var source = stack("g11k2"); var original = player(helper, source);
        original.getInventory().setItem(1, stack("caseless_bullet").copyWithCount(12));
        GunEngine.handleInput(original, source, InteractionHand.MAIN_HAND, "Reload1", true);
        var loaded = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag)source.save(helper.getLevel().registryAccess()));
        var owner = player(helper, loaded);
        owner.getInventory().setItem(1, stack("caseless_bullet").copyWithCount(12));
        for (int tick = 1; tick <= 90; tick++) helper.runAtTickTime(tick, () -> GunEngine.tick(owner, loaded));
        helper.runAtTickTime(91, () -> {
            helper.assertTrue(loaded.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY).isEmpty()
                    && owner.getInventory().getItem(1).getCount() == 12 && loaded.get(ModDataComponents.GUN_ACTION_STATE).activeGroup().isEmpty(),
                    "Saved metadata without original runtime ownership must cancel, never reload from another holder's inventory");
            helper.succeed();
        });
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar", timeoutTicks = 110)
    public static void droppedReloadCannotResumeAfterAnInventoryTickGap(GameTestHelper helper) {
        var gun = stack("g11k2"); var owner = player(helper, gun);
        owner.getInventory().setItem(1, stack("caseless_bullet").copyWithCount(12));
        GunEngine.handleInput(owner, gun, InteractionHand.MAIN_HAND, "Reload1", true);
        helper.runAtTickTime(1, () -> GunEngine.tick(owner, gun));
        for (int tick = 4; tick <= 90; tick++) helper.runAtTickTime(tick, () -> GunEngine.tick(owner, gun));
        helper.runAtTickTime(91, () -> {
            helper.assertTrue(gun.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY).isEmpty()
                    && owner.getInventory().getItem(1).getCount() == 12 && gun.get(ModDataComponents.GUN_ACTION_STATE).activeGroup().isEmpty(),
                    "A dropped group's runtime gap must cancel reload instead of resuming on pickup");
            helper.succeed();
        });
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar", timeoutTicks = 110)
    public static void logoutDiscardsPendingReloadOwnership(GameTestHelper helper) {
        var gun = stack("g11k2"); var owner = player(helper, gun);
        owner.getInventory().setItem(1, stack("caseless_bullet").copyWithCount(12));
        GunEngine.handleInput(owner, gun, InteractionHand.MAIN_HAND, "Reload1", true);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(owner));
        for (int tick = 1; tick <= 90; tick++) helper.runAtTickTime(tick, () -> GunEngine.tick(owner, gun));
        helper.runAtTickTime(91, () -> {
            helper.assertTrue(gun.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY).isEmpty()
                    && owner.getInventory().getItem(1).getCount() == 12 && gun.get(ModDataComponents.GUN_ACTION_STATE).activeGroup().isEmpty(),
                    "Logout removes action ownership; reusing the old stack cannot resume its reload");
            helper.succeed();
        });
    }

    @GameTest(template = "projectile_arena", templateNamespace = "createloveandwar")
    public static void reloadClockAdvancesOncePerGameTickAfterTrigger(GameTestHelper helper) {
        var gun = stack("g11k2"); var owner = player(helper, gun);
        owner.getInventory().setItem(1, stack("caseless_bullet").copyWithCount(12));
        GunEngine.handleInput(owner, gun, InteractionHand.MAIN_HAND, "Reload1", true);
        GunEngine.tick(owner, gun);
        helper.assertTrue(gun.get(ModDataComponents.GUN_ACTION_STATE).groupTicksElapsed() == 0, "Trigger gameTime cannot also advance a fresh group");
        helper.runAtTickTime(1, () -> {
            GunEngine.tick(owner, gun); GunEngine.tick(owner, gun);
            helper.assertTrue(gun.get(ModDataComponents.GUN_ACTION_STATE).groupTicksElapsed() == 1, "Duplicate inventory calls in one gameTime must not advance reload twice");
        });
        helper.runAtTickTime(2, () -> {
            GunEngine.tick(owner, gun);
            helper.assertTrue(gun.get(ModDataComponents.GUN_ACTION_STATE).groupTicksElapsed() == 2, "Following tick advances normally");
            helper.succeed();
        });
    }
}
