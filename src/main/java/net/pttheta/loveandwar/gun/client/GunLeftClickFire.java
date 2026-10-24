package net.pttheta.loveandwar.gun.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.pttheta.loveandwar.gun.network.CGunInput;
import net.pttheta.loveandwar.gun.runtime.GunInputDispatcher;
import net.pttheta.loveandwar.gun.runtime.GunItem;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class GunLeftClickFire {
    private static final GunInputDispatcher INPUTS = new GunInputDispatcher();
    static final GunInputDispatcher.Sink SINK =
            new GunInputDispatcher.Sink() {
                @Override
                public CGunInput send(CGunInput input) {
                    return GunPrediction.send(input);
                }

                @Override
                public java.util.Optional<CGunInput> held(CGunInput input) {
                    return GunPrediction.held(input);
                }
            };

    private GunLeftClickFire() {}

    @SubscribeEvent
    static void onInteractionKey(InteractionKeyMappingTriggered event) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        if (event.isAttack() && player.getMainHandItem().getItem() instanceof GunItem) {
            event.setSwingHand(false);
            event.setCanceled(true);
        } else if (event.isUseItem()
                && player.getItemInHand(event.getHand()).getItem() instanceof GunItem) {
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onClientTick(Post event) {
        var mc = Minecraft.getInstance();
        INPUTS.poll(
                mc.player, "Fire1", mc.screen == null && mc.options.keyAttack.isDown(), true, SINK);
        INPUTS.poll(
                mc.player,
                "Fire2",
                mc.screen == null && (mc.options.keyUse.isDown() || GunKeyMappings.FIRE2.isDown()),
                true,
                SINK);
        GunPrediction.flushAuthored();
    }
}
