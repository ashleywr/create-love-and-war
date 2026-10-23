package net.pttheta.loveandwar.gun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;
import net.pttheta.loveandwar.gun.runtime.GunEngine;
import net.pttheta.loveandwar.gun.runtime.GunLookup;
import org.slf4j.LoggerFactory;

/** Drives the real click handler and packet path without native mouse automation. */
@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class GunFireClientScenario {
    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        if (!Boolean.getBoolean("law.enableClientScenario")) return;
        event.getDispatcher().register(Commands.literal("law_test_fire_client")
            .then(Commands.literal("press").executes(context -> press(false)))
            .then(Commands.literal("tap").executes(context -> press(true)))
            .then(Commands.literal("release").executes(context -> {
                Minecraft.getInstance().options.keyAttack.setDown(false);
                return 1;
            }))
            .then(Commands.literal("status").executes(context -> {
                var minecraft = Minecraft.getInstance();
                if (minecraft.player == null) return 0;
                var gun = minecraft.player.getMainHandItem();
                LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_FIRE_CLIENT rounds={} keyHeld={}",
                    gun.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY).items().size(), minecraft.options.keyAttack.isDown());
                return 1;
            })));
    }

    private static int press(boolean tap) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return 0;
        minecraft.setScreen(null);
        var gun = minecraft.player.getMainHandItem();
        var predicted = GunEngine.predictStartedGroup(minecraft.player, gun, "Fire1", minecraft.level.getGameTime());
        LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_RESTRICTION_PREDICTION gun={} group={} rounds={} eyeFluid={} offhand={}",
                GunLookup.contentKey(gun), predicted.map(group -> group.key()).orElse("none"),
                gun.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY).items().size(),
                minecraft.level.getFluidState(net.minecraft.core.BlockPos.containing(minecraft.player.getEyePosition())), minecraft.player.getOffhandItem());
        minecraft.options.keyAttack.setDown(true);
        GunLeftClickFire.onInteractionKey(new InputEvent.InteractionKeyMappingTriggered(0, minecraft.options.keyAttack, InteractionHand.MAIN_HAND));
        if (tap) minecraft.options.keyAttack.setDown(false);
        return 1;
    }
}
