package net.pttheta.loveandwar.gametest;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.IntConsumer;
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
import net.pttheta.loveandwar.gun.runtime.GunActionState;
import net.pttheta.loveandwar.gun.runtime.GunEngine;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;

@GameTestHolder("createloveandwar")
@PrefixGameTestTemplate(false)
public final class GunBurstGameTests {
    private record Fixture(FakePlayer player, ItemStack gun) {
        void input(boolean pressed) { GunEngine.handleInput(player, gun, InteractionHand.MAIN_HAND, "Fire1", pressed); }
        void tick() { GunEngine.tick(player, gun); }
        int rounds() { return gun.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY).items().size(); }
    }

    private static Fixture fixture(GameTestHelper helper, String name, int count) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "FireTest"));
        var position = helper.absolutePos(new BlockPos(2, 3, 2));
        player.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
        player.setXRot(-90); // Real registered hitscan ammunition, aimed away from other test structures.
        var gun = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:" + name)));
        String ammunition = name.equals("mat49") ? "pistol_bullet" : name.equals("chassepot") ? "black_powder_cartridge" : "caseless_bullet";
        var bullet = BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:" + ammunition));
        var rounds = new ArrayList<ItemStack>();
        for (int i = 0; i < count; i++) rounds.add(new ItemStack(bullet));
        gun.set(ModDataComponents.GUN_LOADED_ROUNDS, new AmmoPouchContents(rounds));
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        return new Fixture(player, gun);
    }

    private static void frames(GameTestHelper helper, int count, IntConsumer frame) {
        for (int i = 1; i <= count; i++) {
            final int tick = i;
            helper.runAtTickTime(tick, () -> frame.accept(tick));
        }
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void releasedG11FinishesItsThreeRoundBurst(GameTestHelper helper) {
        var f = fixture(helper, "g11k2", 12);
        f.input(true);
        helper.assertTrue(f.rounds() == 11, "Initial press must fire one round");
        f.input(false);
        frames(helper, 4, tick -> {
            f.tick();
            int expected = 12 - Math.min(3, tick + 1);
            helper.assertTrue(f.rounds() == expected, "G11 release must preserve burst cadence at tick " + tick + "; expected " + expected + " rounds, got " + f.rounds());
        });
        helper.runAtTickTime(5, helper::succeed);
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void heldG11DoesNotRestartAnExhaustedBurst(GameTestHelper helper) {
        var f = fixture(helper, "g11k2", 12);
        f.input(true);
        frames(helper, 55, tick -> { f.input(true); f.tick(); }); // Also exercises legacy/spammed hold packets.
        helper.runAtTickTime(56, () -> {
            helper.assertTrue(f.rounds() == 9, "Holding the trigger must fire one finite burst, even after its animation ends; got " + f.rounds() + " rounds");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void releasedAndRepressedG11StartsANewBurst(GameTestHelper helper) {
        var f = fixture(helper, "g11k2", 12);
        f.input(true);
        frames(helper, 8, tick -> {
            f.tick();
            if (tick == 3) f.input(false);
            if (tick == 4) f.input(true);
        });
        helper.runAtTickTime(9, () -> {
            helper.assertTrue(f.rounds() == 6, "A fresh press must rearm one new burst; got " + f.rounds() + " rounds");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void serverSchedulesFullAutoUntilRelease(GameTestHelper helper) {
        var f = fixture(helper, "mat49", 12);
        f.input(true);
        frames(helper, 10, tick -> {
            f.tick();
            if (tick == 5) f.input(false);
        });
        helper.runAtTickTime(11, () -> {
            helper.assertTrue(f.rounds() == 9, "Full auto must fire at ticks 0/2/4 with a single press, then stop on release; got " + f.rounds());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void shortBurstDoesNotFallThroughToReloadWhileHeld(GameTestHelper helper) {
        var f = fixture(helper, "g11k2", 2);
        f.input(true);
        frames(helper, 8, tick -> { f.input(true); f.tick(); });
        helper.runAtTickTime(9, () -> {
            helper.assertTrue(f.rounds() == 0, "A short magazine must consume only its available rounds");
            var state = f.gun().getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
            helper.assertTrue(state.pendingReloadGroups().isEmpty() && !state.activeGroup().contains("reload"), "Held burst must not become a fresh reload request");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void releasedBurstKeepsTickingInAnUnselectedInventorySlot(GameTestHelper helper) {
        var f = fixture(helper, "g11k2", 12);
        f.input(true);
        f.input(false);
        f.player().getInventory().setItem(1, f.gun());
        f.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        frames(helper, 4, tick -> f.gun().inventoryTick(helper.getLevel(), f.player(), 1, false));
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(f.rounds() == 9, "Original action stack keeps ticking when its gun remains in an unselected inventory slot");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void releaseWithEmptyHandStopsPreviouslySelectedFullAuto(GameTestHelper helper) {
        var f = fixture(helper, "mat49", 12);
        f.input(true);
        frames(helper, 6, tick -> {
            f.tick();
            if (tick == 3) {
                f.player().getInventory().setItem(1, f.gun());
                f.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                // Network release now resolves the captured UUID, even with an empty current hand.
                GunEngine.handleInput(f.player(), f.gun(), InteractionHand.MAIN_HAND, "Fire1", false);
            }
        });
        helper.runAtTickTime(7, () -> {
            helper.assertTrue(f.rounds() == 10, "Release must stop the original gun even after selecting an empty slot");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void semiAutoFiresOncePerFreshPress(GameTestHelper helper) {
        var f = fixture(helper, "chassepot", 12);
        f.input(true);
        frames(helper, 25, tick -> {
            f.input(true);
            f.tick();
            if (tick == 20) f.input(false);
            if (tick == 21) f.input(true);
        });
        helper.runAtTickTime(26, () -> {
            helper.assertTrue(f.rounds() == 10, "Semi auto must consume one round for each of two fresh presses; got " + f.rounds());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void savedFiringMetadataDoesNotResumeWithoutRuntimeControl(GameTestHelper helper) {
        var f = fixture(helper, "g11k2", 12);
        f.gun().set(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE.withGroup("primary_fire", 46, java.util.List.of())
                .withBurstRemaining(2).withWaitingForRelease(true));
        f.tick();
        var state = f.gun().getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
        helper.assertTrue(f.rounds() == 12 && state.activeGroup().isEmpty() && state.burstRemaining() == 0 && !state.waitingForRelease(),
                "Saved action metadata must not restore a live trigger or burst after a session restart");
        f.gun().set(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE.withWaitingForRelease(true).withCooldown(Long.MAX_VALUE));
        f.tick();
        state = f.gun().getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
        helper.assertTrue(!state.waitingForRelease() && state.cooldownUntilGameTime() == 0, "Idle saved trigger flags must not block first-shot prediction after reconnect");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void manualReloadInterruptsBurstRetriggers(GameTestHelper helper) {
        var f = fixture(helper, "g11k2", 12);
        // Valid spare ammunition for both the original base magazine and the still-unfixed target's first-match conversion.
        f.player().getInventory().setItem(9, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:caseless_bullet")), 12));
        f.player().getInventory().setItem(10, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:gyrojet_basic")), 12));
        f.input(true);
        frames(helper, 5, tick -> {
            f.tick();
            if (tick == 1) GunEngine.handleInput(f.player(), f.gun(), InteractionHand.MAIN_HAND, "Reload1", true);
        });
        helper.runAtTickTime(6, () -> {
            helper.assertTrue(f.rounds() == 10, "Reload must interrupt remaining burst retriggers after the first two rounds");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void deadOwnerDoesNotContinueFullAuto(GameTestHelper helper) {
        var f = fixture(helper, "mat49", 12);
        f.input(true);
        f.player().setHealth(0);
        frames(helper, 4, tick -> f.tick());
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(f.rounds() == 11, "A dead owner must not continue consuming ammunition or firing");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "createloveandwar")
    public static void droppedAndPickedUpGunDoesNotResumeStaleBurst(GameTestHelper helper) {
        var f = fixture(helper, "g11k2", 12);
        f.input(true);
        frames(helper, 8, tick -> {
            if (tick == 1 || tick >= 4) f.tick();
            if (tick == 2) f.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (tick == 2 || tick == 3) net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                    new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(f.player()));
            if (tick == 4) f.player().setItemInHand(InteractionHand.MAIN_HAND, f.gun());
            if (tick == 5) helper.assertTrue(f.rounds() == 10, "Pickup must not resume the dropped burst");
            if (tick == 6) f.input(true);
        });
        helper.runAtTickTime(9, () -> {
            helper.assertTrue(f.rounds() == 7, "A new press after pickup must start exactly one new burst; got " + f.rounds());
            helper.succeed();
        });
    }
}
