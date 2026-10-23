package net.pttheta.loveandwar.item.custom;

import java.util.List;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

/** Raw pouch weight, independent of vanilla BundleContents' per-stack-limit fractions. */
public record AmmoPouchTooltip(List<ItemStack> items, int weight) implements TooltipComponent {
    public AmmoPouchTooltip {
        items = items.stream().map(ItemStack::copy).toList();
    }
}
