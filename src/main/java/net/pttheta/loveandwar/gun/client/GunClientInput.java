package net.pttheta.loveandwar.gun.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.pttheta.loveandwar.gun.runtime.GunInputDispatcher;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class GunClientInput {
    private static final GunInputDispatcher INPUTS = new GunInputDispatcher();

    private GunClientInput() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(Post event) {
        poll(GunKeyMappings.SPECIAL_KEY_1, "SpecialKey1", false);
        poll(GunKeyMappings.SPECIAL_KEY_2, "SpecialKey2", false);
        poll(GunKeyMappings.RELOAD1, "Reload1", false);
    }

    private static void poll(KeyMapping mapping, String input, boolean continuous) {
        var mc = Minecraft.getInstance();
        INPUTS.poll(
                mc.player,
                input,
                mc.screen == null && mapping.isDown(),
                continuous,
                GunLeftClickFire.SINK);
    }
}
