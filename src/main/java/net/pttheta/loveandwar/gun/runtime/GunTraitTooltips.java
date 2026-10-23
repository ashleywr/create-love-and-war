package net.pttheta.loveandwar.gun.runtime;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.pttheta.loveandwar.item.ModDataComponents;

public final class GunTraitTooltips {
    private GunTraitTooltips() {}

    public static List<Component> lines(ItemStack gun, boolean expanded) {
        var lines = new ArrayList<Component>();
        if (!expanded) lines.add(Component.translatable("tooltip.createloveandwar.hold_shift_for_parts").withStyle(ChatFormatting.GRAY));
        else {
            var parts = gun.getOrDefault(ModDataComponents.CRAFTED_PARTS, CraftedParts.EMPTY).parts();
            if (parts.isEmpty()) lines.add(Component.translatable("tooltip.createloveandwar.crafted_from_nothing").withStyle(ChatFormatting.GRAY));
            for (var part : parts) lines.add(Component.translatable("tooltip.createloveandwar.crafted_from", itemName(part)).withStyle(ChatFormatting.GRAY));
        }
        for (var trait : GunTraits.resolve(gun).values()) {
            String key = "ability." + trait.id().getNamespace() + "." + trait.id().getPath();
            var label = Component.translatable(key);
            if (trait.definition().maxLevel() != 1) label.append(" " + roman(trait.level()));
            if (expanded) label.append(" - ").append(Component.translatable(key + ".tooltip." + trait.level()));
            // The reference obtains the formatting code from the localized trait colour.
            String colour = Component.translatable(key + ".colour").getString();
            ChatFormatting format = colour.length() == 1 ? ChatFormatting.getByCode(colour.charAt(0)) : null;
            if (format != null) label.withStyle(format);
            lines.add(label);
        }
        var attachments = gun.getOrDefault(ModDataComponents.GUN_ATTACHMENTS, GunAttachments.EMPTY);
        for (String category : List.of("barrel", "grip", "stock", "scope", "generic")) {
            for (var attachment : attachments.forCategory(category)) lines.add(Component.translatable("tooltip.createloveandwar.attached", attachment.getHoverName()));
        }
        return lines;
    }

    private static Component itemName(ResourceLocation id) {
        return Component.translatable("item." + id.getNamespace() + "." + id.getPath());
    }

    private static String roman(int number) {
        if (number <= 0 || number > 3999) return Integer.toString(number);
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] digits = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        var result = new StringBuilder();
        for (int i = 0; i < values.length; i++) while (number >= values[i]) { result.append(digits[i]); number -= values[i]; }
        return result.toString();
    }
}
