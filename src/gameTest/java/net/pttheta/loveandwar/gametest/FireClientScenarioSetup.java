package net.pttheta.loveandwar.gametest;

import java.util.ArrayList;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.pttheta.loveandwar.Config;
import net.pttheta.loveandwar.gun.runtime.GunActionState;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;

/** Only present in development; use exclusively in the disposable test world. */
@EventBusSubscriber(modid = "createloveandwar")
public final class FireClientScenarioSetup {
    @SubscribeEvent
    public static void resetDiagnostics(ServerStoppedEvent event) {
        if (Boolean.getBoolean("law.enableClientScenario")) Config.GUN_DIAGNOSTICS.set(false);
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        if (!Boolean.getBoolean("law.enableClientScenario")) return;
        var setup = Commands.literal("law_test_fire_setup").requires(source -> source.hasPermission(2));
        for (String name : new String[]{"g11k2", "mat49"}) {
            setup.then(Commands.literal(name).executes(context -> {
                var player = context.getSource().getPlayerOrException();
                var gun = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:" + name)));
                var rounds = new ArrayList<ItemStack>();
                String bullet = name.equals("g11k2") ? "caseless_bullet" : "pistol_bullet";
                for (int i = 0; i < 12; i++) rounds.add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:" + bullet))));
                gun.set(ModDataComponents.GUN_LOADED_ROUNDS, new AmmoPouchContents(rounds));
                player.setItemInHand(InteractionHand.MAIN_HAND, gun);
                player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), -80);
                Config.GUN_DIAGNOSTICS.set(true);
                context.getSource().sendSuccess(() -> Component.literal("LAW_FIRE_SETUP gun=" + name + " rounds=12"), false);
                return 1;
            }));
        }
        event.getDispatcher().register(setup);
        event.getDispatcher().register(Commands.literal("law_test_fire_status").requires(source -> source.hasPermission(2)).executes(context -> {
            var gun = context.getSource().getPlayerOrException().getMainHandItem();
            var state = gun.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE);
            String message = "LAW_FIRE_SERVER rounds=" + gun.getOrDefault(ModDataComponents.GUN_LOADED_ROUNDS, AmmoPouchContents.EMPTY).items().size()
                    + " group=" + state.activeGroup() + " pending=" + state.burstRemaining() + " waiting=" + state.waitingForRelease();
            context.getSource().sendSuccess(() -> Component.literal(message), false);
            return 1;
        }));
    }
}
