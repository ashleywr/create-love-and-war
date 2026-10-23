package net.pttheta.loveandwar.gun.client;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.pttheta.loveandwar.Config;
import net.pttheta.loveandwar.gun.content.ActionGroup;
import net.pttheta.loveandwar.gun.content.GunDef;
import net.pttheta.loveandwar.gun.content.stat.SightFov;
import net.pttheta.loveandwar.gun.runtime.GunActionState;
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
                    stack.getOrDefault(ModDataComponents.GUN_ACTION_STATE, GunActionState.IDLE));
        }
        float strength = Config.ADS_ZOOM.get().floatValue();
        // GameRenderer smooths/clamps this modifier for world FOV. The separate hand projection stays untouched.
        event.setNewFovModifier(sights.apply(event.getNewFovModifier(), strength));
    }

    static SightFov addSights(SightFov sights, GunDef def, GunAttachments attachments, GunActionState state) {
        for (String key : state.activeToggles()) {
            var group = findActionGroup(def, attachments, key);
            if (group.isPresent()) sights = sights.add(group.get(), StatSources.forFiring(def, group.get(), attachments));
        }
        return sights;
    }

    private static Optional<ActionGroup> findActionGroup(GunDef def, GunAttachments attachments, String key) {
        // Runtime first-created group provenance is still pending; this fallback cannot distinguish aliased providers.
        var base = def.actionGroups().stream().filter(group -> group.key().equals(key)).findFirst();
        if (base.isPresent()) return base;
        for (ItemStack stack : attachments.all()) {
            var attachment = GunLookup.attachmentDef(stack);
            if (attachment.isEmpty()) continue;
            var override = attachment.get().actionOverrides().stream().filter(group -> group.key().equals(key)).findFirst();
            if (override.isPresent()) return override;
        }
        return Optional.empty();
    }
}
