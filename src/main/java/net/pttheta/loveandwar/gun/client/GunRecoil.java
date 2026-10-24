package net.pttheta.loveandwar.gun.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.pttheta.loveandwar.gun.network.SGunRecoil;

@EventBusSubscriber(modid="createloveandwar",value=Dist.CLIENT)
public final class GunRecoil {
    private static final RecoilState STATE=new RecoilState();
    private GunRecoil(){}
    public static void accept(SGunRecoil impulse){
        var player=Minecraft.getInstance().player;
        if(player!=null)STATE.shot(impulse.horizontal(),impulse.vertical(),impulse.triggerIndex(),player.getRandom().nextGaussian());
    }
    @SubscribeEvent static void tick(ClientTickEvent.Post event){STATE.tick();}
    @SubscribeEvent static void camera(ViewportEvent.ComputeCameraAngles event){
        // Original global manager has no camera, hand, player/world or first-person filter.
        float partial=Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        event.setYaw(event.getYaw()+STATE.yaw(partial));event.setPitch(event.getPitch()-STATE.pitch(partial));
    }
}
