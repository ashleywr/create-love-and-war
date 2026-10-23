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
        minecraft.options.keyAttack.setDown(true);
        GunLeftClickFire.onInteractionKey(new InputEvent.InteractionKeyMappingTriggered(0, minecraft.options.keyAttack, InteractionHand.MAIN_HAND));
        if (tap) minecraft.options.keyAttack.setDown(false);
        return 1;
    }
}
