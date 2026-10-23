package net.pttheta.loveandwar.gametest;

import java.util.List;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.pttheta.loveandwar.gun.content.SoundDef;
import net.pttheta.loveandwar.gun.network.SGunSound;
import net.pttheta.loveandwar.gun.runtime.GunAttachments;
import net.pttheta.loveandwar.item.ModDataComponents;
import net.pttheta.loveandwar.item.custom.AmmoPouchContents;
import net.pttheta.loveandwar.network.ModMessages;

/** Opt-in commands for disposable development worlds; absent from release artifacts. */
@EventBusSubscriber(modid = "createloveandwar")
public final class AudioClientScenarioSetup {
    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createloveandwar:" + id)));
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        if (!Boolean.getBoolean("law.enableClientScenario")) return;
        var setup = Commands.literal("law_test_audio_setup").requires(source -> source.hasPermission(2));
        for (String variant : new String[]{"bare", "silenced"}) {
            setup.then(Commands.literal(variant).executes(context -> {
                var player = context.getSource().getPlayerOrException();
                var gun = item("g11k2");
                gun.set(ModDataComponents.GUN_LOADED_ROUNDS, new AmmoPouchContents(List.of(
                        item("caseless_bullet"), item("caseless_bullet"), item("caseless_bullet"))));
                if (variant.equals("silenced")) gun.set(ModDataComponents.GUN_ATTACHMENTS,
                        GunAttachments.EMPTY.with("barrel", List.of(item("attachment_silencer"))));
                player.setItemInHand(InteractionHand.MAIN_HAND, gun);
                player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), -80);
                context.getSource().sendSuccess(() -> Component.literal("LAW_AUDIO_SETUP variant=" + variant + " rounds=3"), false);
                return 1;
            }));
        }
        event.getDispatcher().register(setup);
        var lod = Commands.literal("law_test_audio_lod").requires(source -> source.hasPermission(2));
        for (String variant : new String[]{"near", "far"}) {
            lod.then(Commands.literal(variant).executes(context -> {
                var player = context.getSource().getPlayerOrException();
                double distance = variant.equals("near") ? 2 : 12;
                var sound = new SoundDef("flansmod:gunshot_slam", 1, 1, 1, 1, 1, 100,
                        List.of(new SoundDef.Lod("flansmod:gunshot_snap", 5)));
                ModMessages.sendToPlayer(new SGunSound(sound, player.getEyePosition().add(distance, 0, 0), 1, 1), player);
                context.getSource().sendSuccess(() -> Component.literal("LAW_AUDIO_LOD variant=" + variant + " offset=" + distance), false);
                return 1;
            }));
        }
        event.getDispatcher().register(lod);
    }
}
