package net.pttheta.loveandwar.gun.client;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.pttheta.loveandwar.gun.runtime.GunItem;
import net.pttheta.loveandwar.gun.runtime.GunTraitTooltips;

@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class GunTraitTooltipExpansion {
    @SubscribeEvent
    public static void expand(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof GunItem) || !Screen.hasShiftDown()) return;
        var collapsed = GunTraitTooltips.lines(event.getItemStack(), false);
        var tooltip = event.getToolTip();
        int start = java.util.Collections.indexOfSubList(tooltip, collapsed);
        if (start < 0) return;
        tooltip.subList(start, start + collapsed.size()).clear();
        tooltip.addAll(start, GunTraitTooltips.lines(event.getItemStack(), true));
    }
}
