package net.pttheta.loveandwar.item.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.pttheta.loveandwar.item.custom.AmmoPouchTooltip;

/** Vanilla bundle grid art/layout with the original pouch's raw count fullness rule. */
@EventBusSubscriber(modid = "createloveandwar", value = Dist.CLIENT)
public final class AmmoPouchClientTooltip implements ClientTooltipComponent {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("container/bundle/background");
    private static final ResourceLocation SLOT = ResourceLocation.withDefaultNamespace("container/bundle/slot");
    private static final ResourceLocation BLOCKED = ResourceLocation.withDefaultNamespace("container/bundle/blocked_slot");
    private final AmmoPouchTooltip contents;
    private final int columns;
    private final int rows;

    public AmmoPouchClientTooltip(AmmoPouchTooltip contents) {
        this.contents = contents;
        columns = Math.max(2, (int)Math.ceil(Math.sqrt(contents.items().size() + 1.0)));
        rows = (contents.items().size() + columns) / columns;
    }

    @SubscribeEvent
    public static void register(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(AmmoPouchTooltip.class, AmmoPouchClientTooltip::new);
    }

    @Override public int getWidth(Font font) { return columns * 18 + 2; }
    @Override public int getHeight() { return rows * 20 + 6; }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        graphics.blitSprite(BACKGROUND, x, y, columns * 18 + 2, rows * 20 + 2);
        for (int index = 0; index < columns * rows; index++) {
            int left = x + index % columns * 18 + 1;
            int top = y + index / columns * 20 + 1;
            boolean hasItem = index < contents.items().size();
            // Quirk kept: original BundleTooltip blocks empty cells at raw weight 64, even for larger pouches.
            graphics.blitSprite(!hasItem && contents.weight() >= 64 ? BLOCKED : SLOT, left, top, 0, 18, 20);
            if (hasItem) {
                var item = contents.items().get(index);
                graphics.renderItem(item, left + 1, top + 1, index);
                graphics.renderItemDecorations(font, item, left + 1, top + 1);
                if (index == 0) AbstractContainerScreen.renderSlotHighlight(graphics, left + 1, top + 1, 0);
            }
        }
    }
}
