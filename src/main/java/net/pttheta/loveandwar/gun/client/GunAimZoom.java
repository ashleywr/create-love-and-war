package net.pttheta.loveandwar.gun.client;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.pttheta.loveandwar.Config;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.gun.content.stat.SightFov;
import net.pttheta.loveandwar.gun.runtime.GunActiveGroups;
import net.pttheta.loveandwar.gun.runtime.GunAttachments;
import net.pttheta.loveandwar.gun.runtime.GunItem;
import net.pttheta.loveandwar.gun.runtime.GunLookup;
import net.pttheta.loveandwar.gun.runtime.StatSources;
import net.pttheta.loveandwar.item.ModDataComponents;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class GunAimZoom {
    private GunAimZoom() {}

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        // Quirk kept: original ClientRenderHooks uses the local shooter's actions even for a different camera player.
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        SightFov sights = SightFov.EMPTY;
        for (var hand : InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof GunItem)) continue;
            var definition = GunLookup.gunDef(stack);
            if (definition.isEmpty()) continue;
            sights = addSights(sights, definition.get(), stack.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY),
                    GunActiveGroups.of(stack), player.getUUID());
        }
        float strength = Config.ADS_ZOOM.get().floatValue();
        // GameRenderer smooths/clamps this modifier for world FOV. The separate hand projection stays untouched.
        event.setNewFovModifier(sights.apply(event.getNewFovModifier(), strength));
    }

    static SightFov addSights(SightFov sights, GunDef def, GunAttachments attachments, GunActiveGroups active, UUID owner) {
        for (var entry : active.entries()) {
            if (!entry.owner().equals(owner)) continue;
            var group = entry.resolve();
            if (group.isPresent()) sights = sights.add(group.get(), StatSources.forFiring(def, group.get(), attachments));
        }
        return sights;
    }
}
