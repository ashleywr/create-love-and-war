package net.pttheta.loveandwar.gametest;

import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = "createloveandwar")
public final class PouchClientScenarioSetup {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        if (!Boolean.getBoolean("law.enableClientScenario")) return;
        event.getDispatcher().register(Commands.literal("law_test_pouch_status").requires(source -> source.hasPermission(2)).executes(context -> {
            var player = context.getSource().getPlayerOrException();
            LoggerFactory.getLogger("createloveandwar/scenario").info("LAW_POUCH_SERVER held={} contents={} slot1={} carried={}",
                    player.getMainHandItem(), player.getMainHandItem().getOrDefault(ModDataComponents.AMMO_POUCH_CONTENTS, AmmoPouchContents.EMPTY),
                    player.getInventory().getItem(1), player.containerMenu.getCarried());
            return 1;
        }));
    }
}
